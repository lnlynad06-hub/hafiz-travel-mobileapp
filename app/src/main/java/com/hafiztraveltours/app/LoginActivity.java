package com.hafiztraveltours.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.CheckBox;
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
import com.hafiztraveltours.app.network.LoginRequest;
import com.hafiztraveltours.app.network.UserDto;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Enterprise-grade Luxury Login Activity for Hafiz Travel & Tours.
 * Backed by MySQL REST API (Retrofit), SessionManager, Remember Me,
 * Luxury Language Picker, and smooth micro-animations.
 */
public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private static final String PREF_AUTH = "auth_prefs";
    private static final String KEY_REMEMBER_ME = "pref_remember_me";
    private static final String KEY_SAVED_EMAIL = "pref_saved_email";

    private GoogleSignInClient mGoogleSignInClient;

    private TextInputLayout emailLayout, passwordLayout;
    private TextInputEditText emailInput, passwordInput;
    private CheckBox rememberMeCheckBox;
    private MaterialButton loginButton;
    private ProgressBar loginProgressBar;
    private TextView tvActiveLanguage;

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
        setContentView(R.layout.activity_login);

        // 1. Initialize Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 2. Bind UI elements
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        rememberMeCheckBox = findViewById(R.id.rememberMeCheckBox);
        loginButton = findViewById(R.id.loginButton);
        loginProgressBar = findViewById(R.id.loginProgressBar);
        tvActiveLanguage = findViewById(R.id.tvActiveLanguage);

        // 3. Clear errors dynamically as user types
        if (emailInput != null) {
            emailInput.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (emailLayout != null) emailLayout.setError(null);
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }
        if (passwordInput != null) {
            passwordInput.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (passwordLayout != null) passwordLayout.setError(null);
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }

        // 4. Load Remember Me preference
        loadRememberMePreference();

        // 5. Setup listeners
        loginButton.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            attemptLogin();
        });

        findViewById(R.id.forgotPasswordText).setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            showForgotPasswordBottomSheet();
        });

        findViewById(R.id.goToSignUp).setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });

        View btnLanguagePicker = findViewById(R.id.btnLanguagePicker);
        if (btnLanguagePicker != null) {
            btnLanguagePicker.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                showLanguageBottomSheet();
            });
        }

        View googleBtn = findViewById(R.id.googleLoginButton);
        if (googleBtn != null) {
            googleBtn.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                setLoadingState(true);
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            });
        }

        View guestText = findViewById(R.id.guestText);
        if (guestText != null) {
            guestText.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                SessionManager.getInstance(LoginActivity.this).clearSession();
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            });
        }

        // 5. Update language label & play entrance animation
        updateActiveLanguageLabel();
        playEntranceAnimation();
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
                                        SessionManager.getInstance(LoginActivity.this).saveAuthSession(token, user);
                                        saveRememberMePreference(email);
                                        Toast.makeText(LoginActivity.this, "Log masuk Google berjaya! " + name, Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                        finish();
                                    } else {
                                        String err = "Log masuk Google gagal. Sila cuba lagi.";
                                        if (response.body() != null && response.body().message != null) {
                                            err = response.body().message;
                                        }
                                        Toast.makeText(LoginActivity.this, err, Toast.LENGTH_LONG).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                                    if (isFinishing() || isDestroyed()) return;
                                    setLoadingState(false);
                                    Toast.makeText(LoginActivity.this, "Ralat sambungan: " + t.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                }
            } catch (ApiException e) {
                setLoadingState(false);
                Toast.makeText(this, "Google Sign-In: Code " + e.getStatusCode(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadRememberMePreference() {
        SharedPreferences prefs = getSharedPreferences(PREF_AUTH, MODE_PRIVATE);
        boolean remember = prefs.getBoolean(KEY_REMEMBER_ME, false);
        String savedEmail = prefs.getString(KEY_SAVED_EMAIL, "");

        if (rememberMeCheckBox != null) {
            rememberMeCheckBox.setChecked(remember);
        }
        if (remember && !savedEmail.isEmpty() && emailInput != null) {
            emailInput.setText(savedEmail);
        }
    }

    private void saveRememberMePreference(String email) {
        SharedPreferences prefs = getSharedPreferences(PREF_AUTH, MODE_PRIVATE);
        boolean isRemember = rememberMeCheckBox != null && rememberMeCheckBox.isChecked();
        prefs.edit()
                .putBoolean(KEY_REMEMBER_ME, isRemember)
                .putString(KEY_SAVED_EMAIL, isRemember ? email : "")
                .apply();
    }

    private void setLoadingState(boolean loading) {
        if (isFinishing() || isDestroyed()) return;
        if (loginButton != null) {
            loginButton.setEnabled(!loading);
            loginButton.setText(loading ? getString(R.string.login_signing_in) : getString(R.string.login_button));
        }
        if (loginProgressBar != null) {
            loginProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
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
                                        LocaleHelper.applyAndSaveLanguage(LoginActivity.this, langCode);
                                    }
                                })
                                .start();
                    })
                    .start();
        });
    }

    private void attemptLogin() {
        String email = emailInput != null && emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String password = passwordInput != null && passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";

        boolean valid = true;

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            if (emailLayout != null) emailLayout.setError(getString(R.string.login_email_invalid));
            valid = false;
        } else {
            if (emailLayout != null) emailLayout.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            if (passwordLayout != null) passwordLayout.setError(getString(R.string.login_password_required));
            valid = false;
        } else {
            if (passwordLayout != null) passwordLayout.setError(null);
        }

        if (!valid) return;

        setLoadingState(true);

        ApiClient.getApiService().login(new LoginRequest(email, password))
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
                                user = new UserDto("1", email.split("@")[0], email, "");
                            }
                            SessionManager.getInstance(LoginActivity.this).saveAuthSession(token, user);
                            saveRememberMePreference(email);
                            Toast.makeText(LoginActivity.this, getString(R.string.login_success), Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            String errorMsg = getString(R.string.login_failed_default);
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
                            Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        setLoadingState(false);
                        String errMsg = t.getMessage() != null ? t.getMessage() : getString(R.string.login_failed_default);
                        Toast.makeText(LoginActivity.this, errMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showForgotPasswordBottomSheet() {
        BottomSheetDialog resetDialog = new BottomSheetDialog(this);
        View sheet = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_forgot_password, null);
        resetDialog.setContentView(sheet);

        Window w = resetDialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setDimAmount(0.55f);
        }

        TextInputLayout resetLayout = sheet.findViewById(R.id.resetEmailLayout);
        TextInputEditText resetInput = sheet.findViewById(R.id.resetEmailInput);
        MaterialButton btnSend = sheet.findViewById(R.id.btnSendReset);

        String currentEmail = emailInput != null && emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        if (!currentEmail.isEmpty() && resetInput != null) {
            resetInput.setText(currentEmail);
        }

        if (btnSend != null) {
            btnSend.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                String email = resetInput != null && resetInput.getText() != null ? resetInput.getText().toString().trim() : "";

                if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    if (resetLayout != null) {
                        resetLayout.setError(getString(R.string.reset_password_invalid_email));
                    }
                    return;
                }
                if (resetLayout != null) resetLayout.setError(null);

                btnSend.setEnabled(false);
                Map<String, String> body = new HashMap<>();
                body.put("email", email);

                ApiClient.getApiService().forgotPassword(body)
                        .enqueue(new Callback<ApiResponse<Object>>() {
                            @Override
                            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                                if (isFinishing() || isDestroyed()) return;
                                resetDialog.dismiss();
                                if (response.isSuccessful()) {
                                    Toast.makeText(LoginActivity.this, getString(R.string.reset_link_sent_success), Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(LoginActivity.this, getString(R.string.reset_password_failed), Toast.LENGTH_LONG).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                                if (isFinishing() || isDestroyed()) return;
                                resetDialog.dismiss();
                                Toast.makeText(LoginActivity.this, getString(R.string.reset_link_sent_success), Toast.LENGTH_LONG).show();
                            }
                        });
            });
        }

        resetDialog.show();
    }

    private void playEntranceAnimation() {
        View header = findViewById(R.id.headerContainer);
        View form = findViewById(R.id.formContainer);
        View signUp = findViewById(R.id.goToSignUp);
        View socialDivider = findViewById(R.id.socialDividerContainer);
        View googleButton = findViewById(R.id.googleLoginButton);
        View guest = findViewById(R.id.guestText);

        View[] views = {header, form, signUp, socialDivider, googleButton, guest};

        long delay = 60;
        for (View v : views) {
            if (v != null) {
                v.setAlpha(0f);
                v.setTranslationY(24f);
                v.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(450)
                        .setStartDelay(delay)
                        .setInterpolator(new DecelerateInterpolator(1.4f))
                        .start();
                delay += 55;
            }
        }
    }
}