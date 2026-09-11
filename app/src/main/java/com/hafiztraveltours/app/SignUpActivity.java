// SignUpActivity.java
package com.hafiztraveltours.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

// Google Sign-In SDK
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.network.AuthResponse;
import com.hafiztraveltours.app.network.GoogleLoginRequest;
import com.hafiztraveltours.app.network.RegisterRequest;
import com.hafiztraveltours.app.network.UserDto;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignUpActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;

    private GoogleSignInClient mGoogleSignInClient;

    private TextInputLayout nameLayout, emailLayout, phoneLayout, passwordLayout, confirmPasswordLayout;
    private TextInputEditText nameInput, emailInput, phoneInput, passwordInput, confirmPasswordInput;
    private MaterialButton signUpButton;
    private ProgressBar signUpProgressBar;
    private TextView tvActiveLanguage;
    private TextView tvTermsDisclaimer;

    private String activeLanguage;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentSaved = LocaleHelper.getSavedLanguage(this);
        if (activeLanguage != null && !activeLanguage.equals(currentSaved)) {
            recreate();
            return;
        }
        activeLanguage = currentSaved;
        updateActiveLanguageLabel();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        nameLayout = findViewById(R.id.nameLayout);
        emailLayout = findViewById(R.id.emailLayout);
        phoneLayout = findViewById(R.id.phoneLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);

        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        phoneInput = findViewById(R.id.phoneInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);

        tvActiveLanguage = findViewById(R.id.tvActiveLanguage);
        signUpButton = findViewById(R.id.signUpButton);
        signUpProgressBar = findViewById(R.id.signUpProgressBar);
        tvTermsDisclaimer = findViewById(R.id.tvTermsDisclaimer);

        // Clear errors as user types
        setupClearErrorOnType(nameInput, nameLayout);
        setupClearErrorOnType(emailInput, emailLayout);
        setupClearErrorOnType(phoneInput, phoneLayout);
        setupClearErrorOnType(passwordInput, passwordLayout);
        setupClearErrorOnType(confirmPasswordInput, confirmPasswordLayout);

        if (tvTermsDisclaimer != null) {
            tvTermsDisclaimer.setText(Html.fromHtml(getString(R.string.signup_terms_disclaimer)));
            tvTermsDisclaimer.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                showTermsBottomSheet();
            });
        }

        signUpButton.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            attemptSignUp();
        });

        findViewById(R.id.goToLogin).setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        });

        View googleBtn = findViewById(R.id.googleSignUpButton);
        if (googleBtn != null) {
            googleBtn.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                setLoadingState(true);
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            });
        }

        View guestSignUpText = findViewById(R.id.guestSignUpText);
        if (guestSignUpText != null) {
            guestSignUpText.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                SessionManager.getInstance(SignUpActivity.this).clearSession();
                startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                finish();
            });
        }

        View btnLanguagePicker = findViewById(R.id.btnLanguagePicker);
        if (btnLanguagePicker != null) {
            btnLanguagePicker.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                showLanguageBottomSheet();
            });
        }

        updateActiveLanguageLabel();
    }

    private void setLoadingState(boolean loading) {
        if (isFinishing() || isDestroyed()) return;
        if (signUpButton != null) {
            signUpButton.setEnabled(!loading);
            signUpButton.setText(loading ? getString(R.string.signup_signing_up) : getString(R.string.signup_button));
        }
        if (signUpProgressBar != null) {
            signUpProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private void setupClearErrorOnType(TextInputEditText input, TextInputLayout layout) {
        if (input == null || layout == null) return;
        input.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            setLoadingState(false);
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    String name = account.getDisplayName() != null ? account.getDisplayName() : "Google User";
                    String email = account.getEmail() != null ? account.getEmail() : "";
                    String googleId = account.getId() != null ? account.getId() : "";
                    String avatar = account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "";

                    setLoadingState(true);
                    GoogleLoginRequest request = new GoogleLoginRequest(email, name, googleId, avatar);
                    ApiClient.getApiService().googleLogin(request)
                            .enqueue(new Callback<ApiResponse<AuthResponse>>() {
                                @Override
                                public void onResponse(Call<ApiResponse<AuthResponse>> call, Response<ApiResponse<AuthResponse>> response) {
                                    if (isFinishing() || isDestroyed()) return;
                                    setLoadingState(false);

                                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                        AuthResponse authData = response.body().data;
                                        String token = authData != null ? authData.token : "";
                                        UserDto user = authData != null ? authData.user : null;
                                        if (user == null) {
                                            user = new UserDto("1", name, email, "");
                                        }
                                        SessionManager.getInstance(SignUpActivity.this).saveAuthSession(token, user);
                                        Toast.makeText(SignUpActivity.this, "Log masuk Google berjaya! " + name, Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                                        finish();
                                    } else {
                                        String err = "Log masuk Google gagal. Sila cuba lagi.";
                                        if (response.body() != null && response.body().message != null) {
                                            err = response.body().message;
                                        }
                                        Toast.makeText(SignUpActivity.this, err, Toast.LENGTH_LONG).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                                    if (isFinishing() || isDestroyed()) return;
                                    setLoadingState(false);
                                    Toast.makeText(SignUpActivity.this, "Ralat sambungan: " + t.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                }
            } catch (ApiException e) {
                setLoadingState(false);
                Toast.makeText(this, "Google Sign-In failed: Code " + e.getStatusCode(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showTermsBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_forgot_password, null);
        dialog.setContentView(sheet);

        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setDimAmount(0.55f);
        }

        // Show quick Terms info
        Toast.makeText(this, "Hafiz Travel & Tours Sdn Bhd - Privasi & Terma", Toast.LENGTH_SHORT).show();
    }

    private void updateActiveLanguageLabel() {
        if (tvActiveLanguage == null) return;
        String lang = LocaleHelper.getSavedLanguage(this);
        tvActiveLanguage.setText(LocaleHelper.getLanguageBadge(lang));
    }

    private void showLanguageBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_language_picker, null);
        dialog.setContentView(sheetView);

        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setDimAmount(0.55f);
        }

        View btnClose = sheetView.findViewById(R.id.btnCloseSheet);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        String current = LocaleHelper.getSavedLanguage(this);

        View[] items = {
                sheetView.findViewById(R.id.itemLangEnglish),
                sheetView.findViewById(R.id.itemLangMalay),
                sheetView.findViewById(R.id.itemLangArabic),
                sheetView.findViewById(R.id.itemLangKorean),
                sheetView.findViewById(R.id.itemLangJapanese),
                sheetView.findViewById(R.id.itemLangChinese)
        };

        String[] codes = {
                LocaleHelper.LANGUAGE_ENGLISH,
                LocaleHelper.LANGUAGE_MALAY,
                LocaleHelper.LANGUAGE_ARABIC,
                LocaleHelper.LANGUAGE_KOREAN,
                LocaleHelper.LANGUAGE_JAPANESE,
                LocaleHelper.LANGUAGE_CHINESE
        };

        int[] radioIds = {
                R.id.icRadioEnglish,
                R.id.icRadioMalay,
                R.id.icRadioArabic,
                R.id.icRadioKorean,
                R.id.icRadioJapanese,
                R.id.icRadioChinese
        };

        for (int i = 0; i < items.length; i++) {
            setupLanguageItem(sheetView, items[i], radioIds[i], codes[i], current, dialog, i);
        }

        dialog.show();
    }

    private void setupLanguageItem(View sheet, View item, int radioId, String langCode, String currentLang, BottomSheetDialog dialog, int index) {
        if (item == null) return;
        ImageView radio = sheet.findViewById(radioId);

        boolean isSelected = langCode.equalsIgnoreCase(currentLang);
        if (isSelected) {
            item.setBackgroundResource(R.drawable.bg_language_item_selected);
            if (radio != null) radio.setImageResource(R.drawable.ic_check_circle_magenta);
        } else {
            item.setBackgroundResource(R.drawable.bg_language_item_normal);
            if (radio != null) radio.setImageResource(R.drawable.ic_circle_unselected);
        }

        item.setAlpha(0f);
        item.setTranslationY(32f);
        item.setScaleX(0.96f);
        item.setScaleY(0.96f);
        item.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(35L * index)
                .setDuration(280)
                .setInterpolator(new DecelerateInterpolator(1.6f))
                .start();

        if (isSelected && radio != null) {
            radio.setScaleX(0f);
            radio.setScaleY(0f);
            radio.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setStartDelay(35L * index + 80)
                    .setDuration(240)
                    .setInterpolator(new OvershootInterpolator(2.4f))
                    .start();
        }

        item.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            item.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(80)
                    .withEndAction(() -> {
                        item.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(120)
                                .setInterpolator(new OvershootInterpolator(1.8f))
                                .withEndAction(() -> {
                                    dialog.dismiss();
                                    if (!langCode.equalsIgnoreCase(currentLang)) {
                                        LocaleHelper.applyAndSaveLanguage(SignUpActivity.this, langCode);
                                    }
                                })
                                .start();
                    })
                    .start();
        });
    }

    private void attemptSignUp() {
        String name = textOf(nameInput);
        String email = textOf(emailInput);
        String rawPhone = textOf(phoneInput);
        String password = textOf(passwordInput);
        String confirmPassword = textOf(confirmPasswordInput);

        boolean valid = true;

        if (TextUtils.isEmpty(name)) {
            nameLayout.setError("Sila masukkan nama penuh");
            valid = false;
        } else {
            nameLayout.setError(null);
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Sila masukkan email yang sah");
            valid = false;
        } else {
            emailLayout.setError(null);
        }

        if (TextUtils.isEmpty(rawPhone) || rawPhone.length() < 7) {
            phoneLayout.setError("Sila masukkan nombor telefon yang sah");
            valid = false;
        } else {
            phoneLayout.setError(null);
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            passwordLayout.setError("Kata laluan sekurang-kurangnya 6 aksara");
            valid = false;
        } else {
            passwordLayout.setError(null);
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError("Kata laluan tidak sepadan");
            valid = false;
        } else {
            confirmPasswordLayout.setError(null);
        }

        if (!valid) return;

        // Normalize phone number with +60 prefix
        final String normalizedPhone;
        if (rawPhone.startsWith("+60")) {
            normalizedPhone = rawPhone;
        } else if (rawPhone.startsWith("60")) {
            normalizedPhone = "+" + rawPhone;
        } else if (rawPhone.startsWith("0")) {
            normalizedPhone = "+60" + rawPhone.substring(1);
        } else {
            normalizedPhone = "+60" + rawPhone;
        }

        setLoadingState(true);

        RegisterRequest request = new RegisterRequest(name, email, normalizedPhone, password, confirmPassword);
        ApiClient.getApiService().register(request)
                .enqueue(new Callback<ApiResponse<AuthResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AuthResponse>> call, Response<ApiResponse<AuthResponse>> response) {
                        if (isFinishing() || isDestroyed()) return;
                        setLoadingState(false);

                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            AuthResponse authData = response.body().data;
                            String token = authData != null ? authData.token : "";
                            UserDto user = authData != null ? authData.user : null;
                            if (user == null) {
                                user = new UserDto("1", name, email, normalizedPhone);
                            }
                            SessionManager.getInstance(SignUpActivity.this).saveAuthSession(token, user);
                            Toast.makeText(SignUpActivity.this, "Pendaftaran berjaya disimpan!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                            finish();
                        } else {
                            String errorMsg = "Pendaftaran gagal, sila cuba lagi.";
                            if (response.body() != null && response.body().message != null && !response.body().message.isEmpty()) {
                                errorMsg = response.body().message;
                            } else if (response.errorBody() != null) {
                                try {
                                    String errJson = response.errorBody().string();
                                    org.json.JSONObject obj = new org.json.JSONObject(errJson);
                                    if (obj.has("message")) {
                                        errorMsg = obj.getString("message");
                                    }
                                } catch (Exception ignored) {}
                            }
                            Toast.makeText(SignUpActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        setLoadingState(false);
                        Toast.makeText(SignUpActivity.this, "Ralat sambungan: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String textOf(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }
}