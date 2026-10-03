package com.hafiztraveltours.app.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.utils.ApiOpResult;
import com.hafiztraveltours.app.utils.HapticUtil;
import com.hafiztraveltours.app.utils.LocaleHelper;
import com.hafiztraveltours.app.utils.PasswordChecklistHelper;

public class ResetPasswordActivity extends BaseActivity {

    public static final String EXTRA_EMAIL = "extra_email";
    public static final String EXTRA_RESET_TOKEN = "extra_reset_token";

    private String targetEmail = "";
    private String resetToken = "";

    private ResetPasswordViewModel viewModel;
    private PasswordChecklistHelper passwordChecklistHelper;

    private LinearLayout formViewContainer;
    private LinearLayout successViewContainer;
    private TextInputLayout passwordLayout;
    private TextInputEditText passwordInput;
    private TextInputLayout confirmPasswordLayout;
    private TextInputEditText confirmPasswordInput;
    private MaterialButton btnResetPassword;
    private ProgressBar resetProgressBar;
    private MaterialButton btnSuccessLogin;
    private TextView tvActiveLanguage;
    private ImageView btnBack;

    private String activeLanguage;

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
        setContentView(R.layout.activity_reset_password);

        targetEmail = getIntent().getStringExtra(EXTRA_EMAIL);
        resetToken = getIntent().getStringExtra(EXTRA_RESET_TOKEN);
        if (targetEmail == null) targetEmail = "";
        if (resetToken == null) resetToken = "";

        viewModel = new ViewModelProvider(this).get(ResetPasswordViewModel.class);

        bindViews();
        setupPasswordChecklist();
        setupListeners();
        observeViewModel();

        updateActiveLanguageLabel();
    }

    private void bindViews() {
        formViewContainer = findViewById(R.id.formViewContainer);
        successViewContainer = findViewById(R.id.successViewContainer);

        passwordLayout = findViewById(R.id.passwordLayout);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);

        btnResetPassword = findViewById(R.id.btnResetPassword);
        resetProgressBar = findViewById(R.id.resetProgressBar);
        btnSuccessLogin = findViewById(R.id.btnSuccessLogin);

        tvActiveLanguage = findViewById(R.id.tvActiveLanguage);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupPasswordChecklist() {
        View reqContainer = findViewById(R.id.passwordRequirementsLayout);
        if (reqContainer != null) {
            passwordChecklistHelper = new PasswordChecklistHelper(reqContainer);
            passwordChecklistHelper.attachToInput(passwordInput);
        }
    }

    private void setupListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                HapticUtil.click(v);
                finish();
            });
        }

        View btnLanguagePicker = findViewById(R.id.btnLanguagePicker);
        if (btnLanguagePicker != null) {
            btnLanguagePicker.setOnClickListener(v -> {
                HapticUtil.click(v);
                showLanguageBottomSheet();
            });
        }

        TextWatcher clearErrorWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (passwordLayout != null) passwordLayout.setError(null);
                if (confirmPasswordLayout != null) confirmPasswordLayout.setError(null);
            }
        };

        if (passwordInput != null) {
            passwordInput.addTextChangedListener(clearErrorWatcher);
        }
        if (confirmPasswordInput != null) {
            confirmPasswordInput.addTextChangedListener(clearErrorWatcher);
        }

        if (btnResetPassword != null) {
            btnResetPassword.setOnClickListener(v -> {
                HapticUtil.click(v);
                attemptPasswordReset();
            });
        }

        if (btnSuccessLogin != null) {
            btnSuccessLogin.setOnClickListener(v -> {
                HapticUtil.click(v);
                navigateToLogin();
            });
        }
    }

    private void attemptPasswordReset() {
        String password = passwordInput != null && passwordInput.getText() != null
                ? passwordInput.getText().toString().trim() : "";
        String confirmPassword = confirmPasswordInput != null && confirmPasswordInput.getText() != null
                ? confirmPasswordInput.getText().toString().trim() : "";

        ResetPasswordViewModel.ResetErrors errors = viewModel.validateInputs(password, confirmPassword);
        if (errors.hasErrors()) {
            if (errors.passwordErr != 0 && passwordLayout != null) {
                passwordLayout.setError(getString(errors.passwordErr));
            }
            if (errors.confirmErr != 0 && confirmPasswordLayout != null) {
                confirmPasswordLayout.setError(getString(errors.confirmErr));
            }
            return;
        }

        if (passwordLayout != null) passwordLayout.setError(null);
        if (confirmPasswordLayout != null) confirmPasswordLayout.setError(null);

        hideKeyboard();
        setLoadingState(true);
        viewModel.resetPassword(targetEmail, resetToken, password, confirmPassword);
    }

    private void observeViewModel() {
        viewModel.getResetOp().observe(this, event -> {
            ApiOpResult result = event != null ? event.consume() : null;
            if (result == null) return;
            setLoadingState(false);

            if (result.success) {
                showSuccessState();
            } else {
                String errorMsg = result.resolveMessage(this);
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showSuccessState() {
        hideKeyboard();
        if (formViewContainer != null) {
            formViewContainer.setVisibility(View.GONE);
        }
        if (btnBack != null) {
            btnBack.setVisibility(View.GONE);
        }
        if (successViewContainer != null) {
            successViewContainer.setVisibility(View.VISIBLE);
            successViewContainer.setAlpha(0f);
            successViewContainer.setTranslationY(30f);
            successViewContainer.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.nav_seamless_fade_in, R.anim.nav_seamless_fade_out);
        finish();
    }

    private void setLoadingState(boolean loading) {
        if (isFinishing() || isDestroyed()) return;
        if (btnResetPassword != null) {
            btnResetPassword.setEnabled(!loading);
            btnResetPassword.setText(loading ? getString(R.string.reset_pass_submitting) : getString(R.string.reset_pass_btn_submit));
        }
        if (resetProgressBar != null) {
            resetProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (passwordInput != null) passwordInput.setEnabled(!loading);
        if (confirmPasswordInput != null) confirmPasswordInput.setEnabled(!loading);
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
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
            HapticUtil.click(v);
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
                                        LocaleHelper.applyAndSaveLanguage(ResetPasswordActivity.this, langCode);
                                    }
                                })
                                .start();
                    })
                    .start();
        });
    }
}
