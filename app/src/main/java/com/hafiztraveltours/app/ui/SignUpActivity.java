// SignUpActivity.java
package com.hafiztraveltours.app.ui;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.adapters.*;
import com.hafiztraveltours.app.network.*;
import com.hafiztraveltours.app.services.*;
import com.hafiztraveltours.app.utils.*;
import com.hafiztraveltours.app.views.*;
import com.hafiztraveltours.app.ui.*;


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
import android.widget.LinearLayout;
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

public class SignUpActivity extends BaseActivity {

    private static final int RC_SIGN_IN = 9001;

    private GoogleSignInClient mGoogleSignInClient;

    private TextInputLayout nameLayout, nicknameLayout, emailLayout, passwordLayout, confirmPasswordLayout;
    private TextInputEditText nameInput, nicknameInput, emailInput, phoneInput, passwordInput, confirmPasswordInput;

    // Phone compound field
    private LinearLayout btnCountryCode;
    private TextView tvCountryCode;
    private ImageView phoneCheckIcon;
    private TextView phoneErrorText;
    private String selectedCountryCode = "+60";
    private String selectedCountryFlag = "\uD83C\uDDF2\uD83C\uDDFE"; // 🇲🇾
    private MaterialButton signUpButton;
    private ProgressBar signUpProgressBar;
    private TextView tvActiveLanguage;
    private TextView tvTermsDisclaimer;

    // Password checklist helper
    private PasswordChecklistHelper passwordChecklistHelper;

    private String activeLanguage;
    private SignUpViewModel signUpViewModel;
    private String pendingGoogleName = "";

    
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

        signUpViewModel = new androidx.lifecycle.ViewModelProvider(this).get(SignUpViewModel.class);
        observeSignUpState();

        nameLayout = findViewById(R.id.nameLayout);
        nicknameLayout = findViewById(R.id.nicknameLayout);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);

        nameInput = findViewById(R.id.nameInput);
        nicknameInput = findViewById(R.id.nicknameInput);
        emailInput = findViewById(R.id.emailInput);
        
        // Phone compound field
        btnCountryCode = findViewById(R.id.btnCountryCode);
        tvCountryCode = findViewById(R.id.tvCountryCode);
        phoneCheckIcon = findViewById(R.id.phoneCheckIcon);
        phoneErrorText = findViewById(R.id.phoneErrorText);
        phoneInput = findViewById(R.id.phoneInput);
        if (btnCountryCode != null) {
            btnCountryCode.setOnClickListener(v -> {
                com.hafiztraveltours.app.utils.HapticUtil.click(v);
                showCountryPicker();
            });
        }

        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);

        tvActiveLanguage = findViewById(R.id.tvActiveLanguage);
        signUpButton = findViewById(R.id.signUpButton);
        signUpProgressBar = findViewById(R.id.signUpProgressBar);
        tvTermsDisclaimer = findViewById(R.id.tvTermsDisclaimer);

        // Clear errors as user types + real-time validation
        setupRealtimeNameValidation();
        setupRealtimeNicknameValidation();
        setupRealtimeEmailValidation();
        setupRealtimePhoneValidation();
        setupRealtimePasswordValidation();
        setupRealtimeConfirmPasswordValidation();

        // Password requirements checklist
        View passwordReqLayout = findViewById(R.id.passwordRequirementsLayout);
        if (passwordReqLayout != null) {
            passwordChecklistHelper = new PasswordChecklistHelper(passwordReqLayout);
            passwordChecklistHelper.attachToInput(passwordInput);
        }

        if (tvTermsDisclaimer != null) {
            tvTermsDisclaimer.setText(Html.fromHtml(getString(R.string.signup_terms_disclaimer)));
            tvTermsDisclaimer.setOnClickListener(v -> {
                com.hafiztraveltours.app.utils.HapticUtil.click(v);
                showTermsBottomSheet();
            });
        }

        signUpButton.setOnClickListener(v -> {
            com.hafiztraveltours.app.utils.HapticUtil.click(v);
            attemptSignUp();
        });

        findViewById(R.id.goToLogin).setOnClickListener(v -> {
            com.hafiztraveltours.app.utils.HapticUtil.click(v);
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        });

        View googleBtn = findViewById(R.id.googleSignUpButton);
        if (googleBtn != null) {
            googleBtn.setOnClickListener(v -> {
                com.hafiztraveltours.app.utils.HapticUtil.click(v);
                setLoadingState(true);
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            });
        }

        View guestSignUpText = findViewById(R.id.guestSignUpText);
        if (guestSignUpText != null) {
                guestSignUpText.setOnClickListener(v -> {
                com.hafiztraveltours.app.utils.HapticUtil.click(v);
                signUpViewModel.clearSession();
                startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                finish();
            });
        }

        View btnLanguagePicker = findViewById(R.id.btnLanguagePicker);
        if (btnLanguagePicker != null) {
            btnLanguagePicker.setOnClickListener(v -> {
                com.hafiztraveltours.app.utils.HapticUtil.click(v);
                showLanguageBottomSheet();
            });
        }

        updateActiveLanguageLabel();
    }

    /** Wires ViewModel results to loading UI, toasts and navigation (H1/Phase 10). */
    private void observeSignUpState() {
        signUpViewModel.getRegisterOp().observe(this, event -> {
            com.hafiztraveltours.app.utils.ApiOpResult result =
                    event != null ? event.consume() : null;
            if (result == null) return;
            setLoadingState(false);
            if (result.success) {
                Toast.makeText(this, getString(R.string.signup_success), Toast.LENGTH_LONG).show();
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                intent.putExtra("prefill_email", textOf(emailInput));
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
            } else {
                Toast.makeText(this, result.resolveMessage(this), Toast.LENGTH_LONG).show();
            }
        });
        signUpViewModel.getGoogleOp().observe(this, event -> {
            com.hafiztraveltours.app.utils.ApiOpResult result =
                    event != null ? event.consume() : null;
            if (result == null) return;
            setLoadingState(false);
            if (result.success) {
                Toast.makeText(this, getString(R.string.login_google_success, pendingGoogleName),
                        Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, result.resolveMessage(this), Toast.LENGTH_LONG).show();
            }
        });
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

    // ── Real-time validation helpers ─────────────────────────────────────────

    private void setupRealtimeNameValidation() {
        if (nameInput == null || nameLayout == null) return;
        nameInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String val = s.toString().trim();
                if (val.isEmpty()) {
                    nameLayout.setError(null);
                    nameLayout.setEndIconDrawable(null);
                } else if (val.length() >= 2) {
                    nameLayout.setError(null);
                    nameLayout.setEndIconMode(com.google.android.material.textfield.TextInputLayout.END_ICON_CUSTOM);
                    nameLayout.setEndIconDrawable(R.drawable.ic_check_circle_magenta);
                } else {
                    nameLayout.setEndIconDrawable(null);
                    nameLayout.setError(getString(R.string.err_name_required));
                }
            }
        });
    }

    private void setupRealtimeNicknameValidation() {
        if (nicknameInput == null || nicknameLayout == null) return;
        nicknameInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String val = s.toString().trim();
                if (val.isEmpty()) {
                    nicknameLayout.setError(null);
                    nicknameLayout.setEndIconDrawable(null);
                } else {
                    nicknameLayout.setError(null);
                    nicknameLayout.setEndIconMode(com.google.android.material.textfield.TextInputLayout.END_ICON_CUSTOM);
                    nicknameLayout.setEndIconDrawable(R.drawable.ic_check_circle_magenta);
                }
            }
        });
    }

    private void setupRealtimeEmailValidation() {
        if (emailInput == null || emailLayout == null) return;
        emailInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String val = s.toString().trim();
                if (val.isEmpty()) {
                    emailLayout.setError(null);
                    emailLayout.setEndIconDrawable(null);
                } else if (android.util.Patterns.EMAIL_ADDRESS.matcher(val).matches()) {
                    emailLayout.setError(null);
                    emailLayout.setEndIconMode(com.google.android.material.textfield.TextInputLayout.END_ICON_CUSTOM);
                    emailLayout.setEndIconDrawable(R.drawable.ic_check_circle_magenta);
                } else {
                    emailLayout.setEndIconDrawable(null);
                    emailLayout.setError(getString(R.string.err_email_invalid));
                }
            }
        });
    }

    private void setupRealtimePhoneValidation() {
        if (phoneInput == null) return;
        phoneInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String val = s.toString().trim();
                if (val.isEmpty()) {
                    setPhoneError(null);
                    if (phoneCheckIcon != null) phoneCheckIcon.setVisibility(View.GONE);
                } else if (val.length() >= 5) {
                    setPhoneError(null);
                    if (phoneCheckIcon != null) phoneCheckIcon.setVisibility(View.VISIBLE);
                } else {
                    if (phoneCheckIcon != null) phoneCheckIcon.setVisibility(View.GONE);
                    setPhoneError(getString(R.string.err_phone_invalid));
                }
            }
        });
    }

    private void setPhoneError(String error) {
        if (phoneErrorText == null) return;
        if (error == null || error.isEmpty()) {
            phoneErrorText.setVisibility(View.GONE);
            phoneErrorText.setText("");
        } else {
            phoneErrorText.setText(error);
            phoneErrorText.setVisibility(View.VISIBLE);
        }
    }

    private void setupRealtimePasswordValidation() {
        if (passwordInput == null || passwordLayout == null) return;
        passwordInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String val = s.toString();
                if (val.isEmpty()) {
                    passwordLayout.setError(null);
                } else if (val.length() >= 8) {
                    passwordLayout.setError(null);
                } else {
                    passwordLayout.setError(getString(R.string.err_password_short));
                }
                // Re-validate confirm password when password changes
                if (confirmPasswordInput != null && confirmPasswordLayout != null) {
                    String confirmVal = confirmPasswordInput.getText() != null ? confirmPasswordInput.getText().toString() : "";
                    if (!confirmVal.isEmpty()) {
                        if (confirmVal.equals(val)) {
                            confirmPasswordLayout.setError(null);
                            confirmPasswordLayout.setHelperText(getString(R.string.passwords_match));
                            confirmPasswordLayout.setHelperTextColor(android.content.res.ColorStateList.valueOf(0xFF047857));
                        } else {
                            confirmPasswordLayout.setHelperText(null);
                            confirmPasswordLayout.setError(getString(R.string.passwords_mismatch));
                        }
                    }
                }
            }
        });
    }

    private void setupRealtimeConfirmPasswordValidation() {
        if (confirmPasswordInput == null || confirmPasswordLayout == null) return;
        confirmPasswordInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String val = s.toString();
                String passwordVal = passwordInput != null && passwordInput.getText() != null
                        ? passwordInput.getText().toString() : "";
                if (val.isEmpty()) {
                    confirmPasswordLayout.setError(null);
                    confirmPasswordLayout.setHelperText(null);
                } else if (val.equals(passwordVal)) {
                    confirmPasswordLayout.setError(null);
                    confirmPasswordLayout.setHelperText(getString(R.string.passwords_match));
                    confirmPasswordLayout.setHelperTextColor(android.content.res.ColorStateList.valueOf(0xFF047857));
                } else {
                    confirmPasswordLayout.setHelperText(null);
                    confirmPasswordLayout.setError(getString(R.string.passwords_mismatch));
                }
            }
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
                    pendingGoogleName = name;
                    signUpViewModel.googleLogin(email, name, googleId, avatar);
                }
            } catch (ApiException e) {
                setLoadingState(false);
                Toast.makeText(this, getString(R.string.login_google_signin_failed, e.getStatusCode()), Toast.LENGTH_LONG).show();
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
        Toast.makeText(this, getString(R.string.privacy_terms_toast), Toast.LENGTH_SHORT).show();
    }

    private void updateActiveLanguageLabel() {
        if (tvActiveLanguage == null) return;
        String lang = LocaleHelper.getSavedLanguage(this);
        tvActiveLanguage.setText(LocaleHelper.getLanguageBadge(lang));
    }

    private void showCountryPicker() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_country_picker, null);
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

        View itemMalaysia = sheetView.findViewById(R.id.itemMalaysia);
        View itemSingapore = sheetView.findViewById(R.id.itemSingapore);
        View itemIndonesia = sheetView.findViewById(R.id.itemIndonesia);
        View itemBrunei = sheetView.findViewById(R.id.itemBrunei);
        View itemThailand = sheetView.findViewById(R.id.itemThailand);
        View itemVietnam = sheetView.findViewById(R.id.itemVietnam);
        View itemPhilippines = sheetView.findViewById(R.id.itemPhilippines);
        View itemLaos = sheetView.findViewById(R.id.itemLaos);
        View itemMyanmar = sheetView.findViewById(R.id.itemMyanmar);
        View itemCambodia = sheetView.findViewById(R.id.itemCambodia);
        View itemTimorLeste = sheetView.findViewById(R.id.itemTimorLeste);

        if (itemMalaysia != null) {
            itemMalaysia.setOnClickListener(v -> {
                selectedCountryCode = "+60";
                selectedCountryFlag = "\uD83C\uDDF2\uD83C\uDDFE";
                updateCountryUI();
                dialog.dismiss();
            });
        }
        if (itemSingapore != null) {
            itemSingapore.setOnClickListener(v -> {
                selectedCountryCode = "+65";
                selectedCountryFlag = "\uD83C\uDDF8\uD83C\uDDEC";
                updateCountryUI();
                dialog.dismiss();
            });
        }
        if (itemIndonesia != null) {
            itemIndonesia.setOnClickListener(v -> {
                selectedCountryCode = "+62";
                selectedCountryFlag = "\uD83C\uDDEE\uD83C\uDDE9";
                updateCountryUI();
                dialog.dismiss();
            });
        }
        if (itemBrunei != null) {
            itemBrunei.setOnClickListener(v -> {
                selectedCountryCode = "+673";
                selectedCountryFlag = "\uD83C\uDde7\uD83C\uDdf0";
                updateCountryUI();
                dialog.dismiss();
            });
        }
        if (itemThailand != null) {
            itemThailand.setOnClickListener(v -> {
                selectedCountryCode = "+66";
                selectedCountryFlag = "\uD83C\uDDF9\uD83C\uDDED";
                updateCountryUI();
                dialog.dismiss();
            });
        }
        if (itemVietnam != null) {
            itemVietnam.setOnClickListener(v -> {
                selectedCountryCode = "+84";
                selectedCountryFlag = "\uD83C\uDDFB\uD83C\uDDF3";
                updateCountryUI();
                dialog.dismiss();
            });
        }
        if (itemPhilippines != null) {
            itemPhilippines.setOnClickListener(v -> {
                selectedCountryCode = "+63";
                selectedCountryFlag = "\uD83C\uDDF5\uD83C\uDDED";
                updateCountryUI();
                dialog.dismiss();
            });
        }
        if (itemLaos != null) {
            itemLaos.setOnClickListener(v -> {
                selectedCountryCode = "+856";
                selectedCountryFlag = "\uD83C\uDDF1\uD83C\uDDE6";
                updateCountryUI();
                dialog.dismiss();
            });
        }
        if (itemMyanmar != null) {
            itemMyanmar.setOnClickListener(v -> {
                selectedCountryCode = "+95";
                selectedCountryFlag = "\uD83C\uDDF2\uD83C\uDDF2";
                updateCountryUI();
                dialog.dismiss();
            });
        }
        if (itemCambodia != null) {
            itemCambodia.setOnClickListener(v -> {
                selectedCountryCode = "+855";
                selectedCountryFlag = "\uD83C\uDDF0\uD83C\uDDED";
                updateCountryUI();
                dialog.dismiss();
            });
        }
        if (itemTimorLeste != null) {
            itemTimorLeste.setOnClickListener(v -> {
                selectedCountryCode = "+670";
                selectedCountryFlag = "\uD83C\uDDF9\uD83C\uDDF1";
                updateCountryUI();
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void updateCountryUI() {
        if (tvCountryCode != null) tvCountryCode.setText(selectedCountryCode);
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
                sheetView.findViewById(R.id.itemLangMalay)
        };

        String[] codes = {
                LocaleHelper.LANGUAGE_ENGLISH,
                LocaleHelper.LANGUAGE_MALAY
        };

        int[] radioIds = {
                R.id.icRadioEnglish,
                R.id.icRadioMalay
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
            com.hafiztraveltours.app.utils.HapticUtil.click(v);
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
        String nickname = textOf(nicknameInput);
        String email = textOf(emailInput);
        String rawPhone = textOf(phoneInput);
        String password = textOf(passwordInput);
        String confirmPassword = textOf(confirmPasswordInput);

        boolean valid = true;

        SignUpViewModel.SignUpErrors errors = signUpViewModel.validateSignUp(
                name, nickname, email, rawPhone, password, confirmPassword);

        if (errors.nameErr != 0) {
            nameLayout.setError(getString(errors.nameErr));
            valid = false;
        } else {
            nameLayout.setError(null);
        }

        if (errors.nickErr != 0) {
            nicknameLayout.setError(getString(errors.nickErr));
            valid = false;
        } else {
            nicknameLayout.setError(null);
        }

        if (errors.emailErr != 0) {
            emailLayout.setError(getString(errors.emailErr));
            valid = false;
        } else {
            emailLayout.setError(null);
        }

        if (errors.phoneErr != 0) {
            setPhoneError(getString(errors.phoneErr));
            valid = false;
        } else {
            setPhoneError(null);
        }

        if (errors.passErr != 0) {
            passwordLayout.setError(getString(errors.passErr));
            valid = false;
        } else {
            passwordLayout.setError(null);
        }

        if (errors.confirmErr != 0) {
            confirmPasswordLayout.setError(getString(errors.confirmErr));
            valid = false;
        } else {
            confirmPasswordLayout.setError(null);
        }

        if (!valid) return;

        // Normalized phone using selected country code (same rule as before).
        final String normalizedPhone =
                SignUpViewModel.normalizePhone(rawPhone, selectedCountryCode);

        setLoadingState(true);
        signUpViewModel.register(name, nickname, email, normalizedPhone, password, confirmPassword);
    }

    private String textOf(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }
}