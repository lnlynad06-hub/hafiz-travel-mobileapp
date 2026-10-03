package com.hafiztraveltours.app.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.utils.HapticUtil;
import com.hafiztraveltours.app.utils.LocaleHelper;

public class ForgotPasswordActivity extends BaseActivity {

    public static final String EXTRA_PREFILL_EMAIL = "prefill_email";

    private LoginViewModel loginViewModel;
    private TextInputLayout emailLayout;
    private TextInputEditText emailInput;
    private MaterialButton btnSendCode;
    private ProgressBar sendProgressBar;
    private TextView tvActiveLanguage;

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
        setContentView(R.layout.activity_forgot_password);

        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        emailLayout = findViewById(R.id.emailLayout);
        emailInput = findViewById(R.id.emailInput);
        btnSendCode = findViewById(R.id.btnSendCode);
        sendProgressBar = findViewById(R.id.sendProgressBar);
        tvActiveLanguage = findViewById(R.id.tvActiveLanguage);

        String prefill = getIntent().getStringExtra(EXTRA_PREFILL_EMAIL);
        if (prefill != null && !prefill.isEmpty() && emailInput != null) {
            emailInput.setText(prefill.trim());
        }

        setupListeners();
        observeViewModel();
        updateActiveLanguageLabel();
    }

    private void setupListeners() {
        View btnBack = findViewById(R.id.btnBack);
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

        btnSendCode.setOnClickListener(v -> {
            HapticUtil.click(v);
            attemptSendCode();
        });

        findViewById(R.id.btnBackToLogin).setOnClickListener(v -> {
            HapticUtil.click(v);
            finish();
        });

        if (emailInput != null) {
            emailInput.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(android.text.Editable s) {
                    if (emailLayout == null) return;
                    String val = s.toString().trim();
                    if (val.isEmpty()) {
                        emailLayout.setError(null);
                        emailLayout.setEndIconDrawable(null);
                    } else if (Patterns.EMAIL_ADDRESS.matcher(val).matches()) {
                        emailLayout.setError(null);
                        emailLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
                        emailLayout.setEndIconDrawable(R.drawable.ic_check_circle_magenta);
                    } else {
                        emailLayout.setEndIconDrawable(null);
                        emailLayout.setError(getString(R.string.reset_password_invalid_email));
                    }
                }
            });
        }
    }

    private void observeViewModel() {
        loginViewModel.getForgotOp().observe(this, event -> {
            com.hafiztraveltours.app.utils.ApiOpResult result =
                    event != null ? event.consume() : null;
            if (result == null) return;
            setLoadingState(false);
            if (result.success) {
                String email = emailInput != null && emailInput.getText() != null
                        ? emailInput.getText().toString().trim() : "";
                Intent intent = new Intent(ForgotPasswordActivity.this, VerifyResetCodeActivity.class);
                intent.putExtra(VerifyResetCodeActivity.EXTRA_EMAIL, email);
                startActivity(intent);
                overridePendingTransition(R.anim.nav_seamless_fade_in, R.anim.nav_seamless_fade_out);
                finish();
            } else {
                String errorMsg = result.resolveMessage(this);
                if (emailLayout != null) {
                    emailLayout.setError(errorMsg);
                    if (emailInput != null) {
                        emailInput.requestFocus();
                    }
                }
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void attemptSendCode() {
        String email = emailInput != null && emailInput.getText() != null
                ? emailInput.getText().toString().trim() : "";

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            if (emailLayout != null) {
                emailLayout.setError(getString(R.string.reset_password_invalid_email));
            }
            return;
        }

        if (emailLayout != null) emailLayout.setError(null);
        setLoadingState(true);
        loginViewModel.forgotPassword(email);
    }

    private void setLoadingState(boolean loading) {
        if (isFinishing() || isDestroyed()) return;
        if (btnSendCode != null) {
            btnSendCode.setEnabled(!loading);
            btnSendCode.setText(loading ? getString(R.string.forgot_pass_sending) : getString(R.string.forgot_pass_btn_send_code));
        }
        if (sendProgressBar != null) {
            sendProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (emailInput != null) {
            emailInput.setEnabled(!loading);
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
                                        LocaleHelper.applyAndSaveLanguage(ForgotPasswordActivity.this, langCode);
                                    }
                                })
                                .start();
                    })
                    .start();
        });
    }
}
