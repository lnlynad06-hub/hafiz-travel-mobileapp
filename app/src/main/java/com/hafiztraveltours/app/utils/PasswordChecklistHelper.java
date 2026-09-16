package com.hafiztraveltours.app.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.textfield.TextInputEditText;
import com.hafiztraveltours.app.R;

public class PasswordChecklistHelper {

    public interface StateChangeListener {
        void onPasswordStateChanged(boolean isValid);
    }

    private final View container;
    private final View passwordStrengthBarContainer;
    private final View passwordStrengthBar;
    private final TextView tvPasswordStrength;
    private final ImageView iconReqLength, iconReqUppercase, iconReqLowercase, iconReqNumber, iconReqSpecial;
    private final TextView tvReqLength, tvReqUppercase, tvReqLowercase, tvReqNumber, tvReqSpecial;
    private final View strongPasswordStatusContainer;
    private StateChangeListener listener;

    // Strength levels: 0=Too Short, 1=Weak, 2=Medium, 3=Strong
    private int currentStrengthLevel = -1;

    // Track state to prevent excessive or duplicate animations
    private boolean isContainerVisible = false;
    private boolean isStrongStateVisible = false;
    private boolean reqLengthMet = false;
    private boolean reqUppercaseMet = false;
    private boolean reqLowercaseMet = false;
    private boolean reqNumberMet = false;
    private boolean reqSpecialMet = false;

    public PasswordChecklistHelper(View container) {
        this.container = container;

        passwordStrengthBarContainer = container.findViewById(R.id.passwordStrengthBarContainer);
        passwordStrengthBar = container.findViewById(R.id.passwordStrengthBar);
        tvPasswordStrength = container.findViewById(R.id.tvPasswordStrength);

        iconReqLength = container.findViewById(R.id.iconReqLength);
        iconReqUppercase = container.findViewById(R.id.iconReqUppercase);
        iconReqLowercase = container.findViewById(R.id.iconReqLowercase);
        iconReqNumber = container.findViewById(R.id.iconReqNumber);
        iconReqSpecial = container.findViewById(R.id.iconReqSpecial);

        tvReqLength = container.findViewById(R.id.tvReqLength);
        tvReqUppercase = container.findViewById(R.id.tvReqUppercase);
        tvReqLowercase = container.findViewById(R.id.tvReqLowercase);
        tvReqNumber = container.findViewById(R.id.tvReqNumber);
        tvReqSpecial = container.findViewById(R.id.tvReqSpecial);

        strongPasswordStatusContainer = container.findViewById(R.id.strongPasswordStatusContainer);

        if (container != null) {
            container.setVisibility(View.GONE);
            container.setAlpha(0f);
            container.setTranslationY(-16f);
        }
        if (strongPasswordStatusContainer != null) {
            strongPasswordStatusContainer.setVisibility(View.GONE);
            strongPasswordStatusContainer.setAlpha(0f);
            strongPasswordStatusContainer.setScaleX(0.9f);
            strongPasswordStatusContainer.setScaleY(0.9f);
        }
    }

    public void setStateChangeListener(StateChangeListener listener) {
        this.listener = listener;
    }

    public void attachToInput(TextInputEditText passwordInput) {
        if (passwordInput == null) return;

        // Initial state evaluation without animation
        updateChecklist(passwordInput.getText() != null ? passwordInput.getText().toString() : "", false);

        passwordInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                updateChecklist(s != null ? s.toString() : "", true);
            }
        });
    }

    public boolean updateChecklist(String password, boolean animate) {
        boolean isEmpty = password == null || password.isEmpty();

        if (isEmpty) {
            hideContainer(animate);
            resetRequirementStates();
            if (listener != null) listener.onPasswordStateChanged(false);
            return false;
        }

        showContainer(animate);

        boolean hasLength = password.length() >= 8;
        boolean hasUppercase = password.matches(".*[A-Z].*");
        boolean hasLowercase = password.matches(".*[a-z].*");
        boolean hasNumber = password.matches(".*[0-9].*");
        boolean hasSpecial = password.matches(".*[^a-zA-Z0-9].*");

        updateRequirementItem(iconReqLength, tvReqLength, hasLength, reqLengthMet, animate);
        reqLengthMet = hasLength;

        updateRequirementItem(iconReqUppercase, tvReqUppercase, hasUppercase, reqUppercaseMet, animate);
        reqUppercaseMet = hasUppercase;

        updateRequirementItem(iconReqLowercase, tvReqLowercase, hasLowercase, reqLowercaseMet, animate);
        reqLowercaseMet = hasLowercase;

        updateRequirementItem(iconReqNumber, tvReqNumber, hasNumber, reqNumberMet, animate);
        reqNumberMet = hasNumber;

        updateRequirementItem(iconReqSpecial, tvReqSpecial, hasSpecial, reqSpecialMet, animate);
        reqSpecialMet = hasSpecial;

        int strengthLevel = calculatePasswordStrength(password, hasLength, hasUppercase, hasLowercase, hasNumber, hasSpecial);
        updateStrengthBar(strengthLevel, animate);

        boolean allMet = hasLength && hasUppercase && hasLowercase && hasNumber && hasSpecial;

        if (allMet) {
            showStrongStatus(animate);
        } else {
            hideStrongStatus(animate);
        }

        if (listener != null) {
            listener.onPasswordStateChanged(allMet);
        }

        return allMet;
    }

    /**
     * Proper password strength calculation (0=Too Short, 1=Weak, 2=Medium, 3=Strong)
     */
    private int calculatePasswordStrength(String password, boolean hasLength, boolean hasUppercase, boolean hasLowercase, boolean hasNumber, boolean hasSpecial) {
        if (!hasLength) return 0; // Too Short (< 8 chars)

        int score = 0;
        if (hasUppercase) score++;
        if (hasLowercase) score++;
        if (hasNumber) score++;
        if (hasSpecial) score++;
        if (password.length() >= 12) score++;

        // Require length + all 4 character types OR 12+ chars with 3 types for Strong
        if (hasLength && hasUppercase && hasLowercase && hasNumber && hasSpecial) return 3; // Strong
        if (score >= 3 && hasLength) return 2; // Medium
        return 1; // Weak
    }

    private void updateStrengthBar(int level, boolean animate) {
        if (passwordStrengthBar == null || tvPasswordStrength == null) return;
        if (level == currentStrengthLevel && animate) return;

        currentStrengthLevel = level;

        Context context = container.getContext();
        String label;
        int color;
        float progressFraction; // 0.25 for Too Short, 0.5 for Weak, 0.75 for Medium, 1.0 for Strong

        switch (level) {
            case 3: // Strong
                label = context.getString(R.string.password_strength_strong);
                color = 0xFF047857; // emerald green
                progressFraction = 1.0f;
                break;
            case 2: // Medium
                label = context.getString(R.string.password_strength_medium);
                color = 0xFFD97706; // amber / warm orange
                progressFraction = 0.7f;
                break;
            case 1: // Weak
                label = context.getString(R.string.password_strength_weak);
                color = 0xFFDC2626; // red
                progressFraction = 0.45f;
                break;
            default: // 0 = Too Short
                label = context.getString(R.string.password_strength_too_short);
                color = 0xFF9CA3AF; // neutral gray
                progressFraction = 0.2f;
                break;
        }

        tvPasswordStrength.setText(label);
        tvPasswordStrength.setTextColor(color);
        passwordStrengthBar.setBackgroundColor(color);

        if (passwordStrengthBarContainer != null && animate) {
            int totalWidth = passwordStrengthBarContainer.getWidth();
            if (totalWidth > 0) {
                int targetWidth = (int) (totalWidth * progressFraction);
                int currentWidth = passwordStrengthBar.getWidth();

                ValueAnimator animator = ValueAnimator.ofInt(currentWidth, targetWidth);
                animator.setDuration(220);
                animator.setInterpolator(new DecelerateInterpolator(1.4f));
                animator.addUpdateListener(animation -> {
                    android.view.ViewGroup.LayoutParams params = passwordStrengthBar.getLayoutParams();
                    params.width = (int) animation.getAnimatedValue();
                    passwordStrengthBar.setLayoutParams(params);
                });
                animator.start();
            }
        }
    }

    private void showContainer(boolean animate) {
        if (container == null || isContainerVisible) return;
        isContainerVisible = true;

        container.animate().cancel();
        container.setVisibility(View.VISIBLE);

        if (animate) {
            container.setAlpha(0f);
            container.setTranslationY(-16f);
            container.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(220)
                    .setInterpolator(new DecelerateInterpolator(1.5f))
                    .setListener(null)
                    .start();
        } else {
            container.setAlpha(1f);
            container.setTranslationY(0f);
        }
    }

    private void hideContainer(boolean animate) {
        if (container == null || !isContainerVisible) return;
        isContainerVisible = false;

        container.animate().cancel();

        if (animate) {
            container.animate()
                    .alpha(0f)
                    .translationY(-12f)
                    .setDuration(180)
                    .setInterpolator(new DecelerateInterpolator(1.2f))
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            container.setVisibility(View.GONE);
                        }
                    })
                    .start();
        } else {
            container.setAlpha(0f);
            container.setTranslationY(-12f);
            container.setVisibility(View.GONE);
        }
    }

    private void showStrongStatus(boolean animate) {
        if (strongPasswordStatusContainer == null || isStrongStateVisible) return;
        isStrongStateVisible = true;

        strongPasswordStatusContainer.animate().cancel();
        strongPasswordStatusContainer.setVisibility(View.VISIBLE);

        if (animate) {
            strongPasswordStatusContainer.setAlpha(0f);
            strongPasswordStatusContainer.setScaleX(0.92f);
            strongPasswordStatusContainer.setScaleY(0.92f);
            strongPasswordStatusContainer.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .setInterpolator(new DecelerateInterpolator(1.5f))
                    .setListener(null)
                    .start();
        } else {
            strongPasswordStatusContainer.setAlpha(1f);
            strongPasswordStatusContainer.setScaleX(1f);
            strongPasswordStatusContainer.setScaleY(1f);
        }
    }

    private void hideStrongStatus(boolean animate) {
        if (strongPasswordStatusContainer == null || !isStrongStateVisible) return;
        isStrongStateVisible = false;

        strongPasswordStatusContainer.animate().cancel();

        if (animate) {
            strongPasswordStatusContainer.animate()
                    .alpha(0f)
                    .scaleX(0.92f)
                    .scaleY(0.92f)
                    .setDuration(150)
                    .setInterpolator(new DecelerateInterpolator())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            strongPasswordStatusContainer.setVisibility(View.GONE);
                        }
                    })
                    .start();
        } else {
            strongPasswordStatusContainer.setAlpha(0f);
            strongPasswordStatusContainer.setVisibility(View.GONE);
        }
    }

    private void updateRequirementItem(ImageView icon, TextView text, boolean isCompleted, boolean prevCompleted, boolean animate) {
        int color = isCompleted ? 0xFF047857 : 0xFF757575; // Emerald green vs neutral gray

        if (text != null) {
            text.setTextColor(color);
        }

        if (icon == null) return;

        if (isCompleted != prevCompleted && animate) {
            // Icon state transition pop animation
            icon.animate().cancel();
            icon.animate()
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setDuration(80)
                    .withEndAction(() -> {
                        icon.setImageResource(isCompleted ? R.drawable.ic_pass_req_check : R.drawable.ic_pass_req_neutral);
                        icon.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(120)
                                .setInterpolator(new DecelerateInterpolator(1.4f))
                                .start();
                    })
                    .start();
        } else {
            icon.setImageResource(isCompleted ? R.drawable.ic_pass_req_check : R.drawable.ic_pass_req_neutral);
            icon.setScaleX(1.0f);
            icon.setScaleY(1.0f);
        }
    }

    private void resetRequirementStates() {
        reqLengthMet = false;
        reqUppercaseMet = false;
        reqLowercaseMet = false;
        reqNumberMet = false;
        reqSpecialMet = false;
        isStrongStateVisible = false;
        currentStrengthLevel = -1;

        if (strongPasswordStatusContainer != null) {
            strongPasswordStatusContainer.setVisibility(View.GONE);
            strongPasswordStatusContainer.setAlpha(0f);
        }
    }
}
