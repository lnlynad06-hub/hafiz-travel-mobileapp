package com.hafiztraveltours.app.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.utils.HapticUtil;
import com.hafiztraveltours.app.utils.LocaleHelper;
import com.hafiztraveltours.app.utils.OtpInputHelper;

import java.util.Locale;

public class VerifyResetCodeActivity extends BaseActivity {

    public static final String EXTRA_EMAIL = "extra_email";

    private String targetEmail = "";
    private VerifyResetCodeViewModel viewModel;
    private OtpInputHelper otpHelper;

    private TextView tvActiveLanguage;
    private TextView tvResetCodeDescription;
    private TextView tvOtpError;
    private MaterialButton btnVerifyCode;
    private ProgressBar verifyProgressBar;
    private TextView btnResendCode;
    private TextView tvResendCountdown;

    private CountDownTimer resendTimer;
    private boolean isTimerRunning = false;
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
        setContentView(R.layout.activity_verify_reset_code);

        targetEmail = getIntent().getStringExtra(EXTRA_EMAIL);
        if (targetEmail == null) targetEmail = "";

        viewModel = new ViewModelProvider(this).get(VerifyResetCodeViewModel.class);

        bindViews();
        setupOtpInputs();
        setupListeners();
        observeViewModel();
        startResendCooldown(60);

        updateActiveLanguageLabel();
    }

    private void bindViews() {
        tvActiveLanguage = findViewById(R.id.tvActiveLanguage);
        tvResetCodeDescription = findViewById(R.id.tvResetCodeDescription);
        tvOtpError = findViewById(R.id.tvOtpError);
        btnVerifyCode = findViewById(R.id.btnVerifyCode);
        verifyProgressBar = findViewById(R.id.verifyProgressBar);
        btnResendCode = findViewById(R.id.btnResendCode);
        tvResendCountdown = findViewById(R.id.tvResendCountdown);

        String descFormatted = getString(R.string.enter_reset_code_desc, targetEmail);
        tvResetCodeDescription.setText(Html.fromHtml(descFormatted));
    }

    private void setupOtpInputs() {
        EditText et1 = findViewById(R.id.etDigit1);
        EditText et2 = findViewById(R.id.etDigit2);
        EditText et3 = findViewById(R.id.etDigit3);
        EditText et4 = findViewById(R.id.etDigit4);
        EditText et5 = findViewById(R.id.etDigit5);
        EditText et6 = findViewById(R.id.etDigit6);

        otpHelper = new OtpInputHelper(et1, et2, et3, et4, et5, et6);
        otpHelper.setOnCodeChangeListener((code, isComplete) -> {
            if (tvOtpError != null) tvOtpError.setVisibility(View.GONE);
            if (isComplete) {
                attemptVerification();
            }
        });

        et1.post(() -> otpHelper.requestFirstFocus());
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

        btnVerifyCode.setOnClickListener(v -> {
            HapticUtil.click(v);
            attemptVerification();
        });

        btnResendCode.setOnClickListener(v -> {
            HapticUtil.click(v);
            if (!isTimerRunning) {
                setLoadingState(true);
                viewModel.resendResetCode(targetEmail);
            }
        });

        findViewById(R.id.btnBackToLogin).setOnClickListener(v -> {
            HapticUtil.click(v);
            Intent intent = new Intent(VerifyResetCodeActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void observeViewModel() {
        viewModel.getVerifyOp().observe(this, event -> {
            VerifyResetCodeViewModel.VerifyResetResult result =
                    event != null ? event.consume() : null;
            if (result == null) return;
            setLoadingState(false);
            if (result.success) {
                Intent intent = new Intent(VerifyResetCodeActivity.this, ResetPasswordActivity.class);
                intent.putExtra(ResetPasswordActivity.EXTRA_EMAIL, targetEmail);
                intent.putExtra(ResetPasswordActivity.EXTRA_RESET_TOKEN, result.resetToken);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
            } else {
                String errorMsg = result.errorMessage != null && !result.errorMessage.isEmpty()
                        ? result.errorMessage
                        : getString(result.errorResId != 0 ? result.errorResId : R.string.reset_password_failed);
                if (tvOtpError != null) {
                    tvOtpError.setText(errorMsg);
                    tvOtpError.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }
        });

        viewModel.getResendOp().observe(this, event -> {
            com.hafiztraveltours.app.utils.ApiOpResult result =
                    event != null ? event.consume() : null;
            if (result == null) return;
            setLoadingState(false);
            if (result.success) {
                Toast.makeText(this, getString(R.string.enter_reset_code_resend_success), Toast.LENGTH_SHORT).show();
                startResendCooldown(60);
                otpHelper.clear();
            } else {
                Toast.makeText(this, result.resolveMessage(this), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void attemptVerification() {
        String code = otpHelper.getCode();
        if (code.length() < 6) {
            if (tvOtpError != null) {
                tvOtpError.setText(getString(R.string.verify_account_code_required));
                tvOtpError.setVisibility(View.VISIBLE);
            }
            return;
        }

        if (tvOtpError != null) tvOtpError.setVisibility(View.GONE);
        setLoadingState(true);
        viewModel.verifyResetCode(targetEmail, code);
    }

    private void startResendCooldown(int seconds) {
        if (resendTimer != null) {
            resendTimer.cancel();
        }

        isTimerRunning = true;
        btnResendCode.setEnabled(false);
        btnResendCode.setAlpha(0.5f);
        if (tvResendCountdown != null) tvResendCountdown.setVisibility(View.VISIBLE);

        resendTimer = new CountDownTimer(seconds * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                long sec = millisUntilFinished / 1000;
                String timeStr = String.format(Locale.getDefault(), "%02d:%02d", sec / 60, sec % 60);
                if (tvResendCountdown != null) {
                    tvResendCountdown.setText(getString(R.string.enter_reset_code_resend_countdown, timeStr));
                }
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                btnResendCode.setEnabled(true);
                btnResendCode.setAlpha(1.0f);
                if (tvResendCountdown != null) tvResendCountdown.setVisibility(View.GONE);
            }
        }.start();
    }

    private void setLoadingState(boolean loading) {
        if (isFinishing() || isDestroyed()) return;
        if (btnVerifyCode != null) {
            btnVerifyCode.setEnabled(!loading);
            btnVerifyCode.setText(loading ? getString(R.string.enter_reset_code_verifying) : getString(R.string.enter_reset_code_btn_verify));
        }
        if (verifyProgressBar != null) {
            verifyProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (otpHelper != null) {
            otpHelper.setEnabled(!loading);
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
                                        LocaleHelper.applyAndSaveLanguage(VerifyResetCodeActivity.this, langCode);
                                    }
                                })
                                .start();
                    })
                    .start();
        });
    }

    @Override
    protected void onDestroy() {
        if (resendTimer != null) {
            resendTimer.cancel();
        }
        super.onDestroy();
    }
}
