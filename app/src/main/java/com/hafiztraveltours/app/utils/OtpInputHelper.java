package com.hafiztraveltours.app.utils;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * Reusable helper for managing 6-digit OTP input cells.
 * Supports auto-forward, backspace handling, pasting 6-digit codes, and focus management.
 */
public class OtpInputHelper {

    private final EditText[] digits;
    private boolean isInternalChange = false;
    private OnCodeChangeListener onCodeChangeListener;

    public interface OnCodeChangeListener {
        void onCodeChanged(String code, boolean isComplete);
    }

    public OtpInputHelper(EditText... editTexts) {
        if (editTexts == null || editTexts.length != 6) {
            throw new IllegalArgumentException("OtpInputHelper requires exactly 6 EditText instances.");
        }
        this.digits = editTexts;
        setupListeners();
    }

    public void setOnCodeChangeListener(OnCodeChangeListener listener) {
        this.onCodeChangeListener = listener;
    }

    private void setupListeners() {
        for (int i = 0; i < digits.length; i++) {
            final int index = i;
            final EditText current = digits[index];

            current.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (isInternalChange) return;

                    String text = s.toString();
                    if (text.length() > 1) {
                        // Handle paste of multiple characters
                        handlePaste(text, index);
                        return;
                    }

                    if (text.length() == 1) {
                        if (index < digits.length - 1) {
                            digits[index + 1].requestFocus();
                        }
                    }

                    notifyChange();
                }
            });

            current.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                    if (current.getText().toString().isEmpty() && index > 0) {
                        digits[index - 1].requestFocus();
                        digits[index - 1].setText("");
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private void handlePaste(String pastedText, int fromIndex) {
        String numbersOnly = pastedText.replaceAll("[^0-9]", "");
        if (numbersOnly.isEmpty()) return;

        isInternalChange = true;
        int maxLen = Math.min(numbersOnly.length(), digits.length);
        for (int i = 0; i < digits.length; i++) {
            if (i < maxLen) {
                digits[i].setText(String.valueOf(numbersOnly.charAt(i)));
            } else {
                digits[i].setText("");
            }
        }
        isInternalChange = false;

        int focusTarget = Math.min(maxLen, digits.length - 1);
        digits[focusTarget].requestFocus();
        digits[focusTarget].setSelection(digits[focusTarget].getText().length());

        notifyChange();
    }

    private void notifyChange() {
        if (onCodeChangeListener != null) {
            String code = getCode();
            onCodeChangeListener.onCodeChanged(code, code.length() == 6);
        }
    }

    public String getCode() {
        StringBuilder sb = new StringBuilder();
        for (EditText et : digits) {
            if (et != null && et.getText() != null) {
                sb.append(et.getText().toString().trim());
            }
        }
        return sb.toString();
    }

    public void clear() {
        isInternalChange = true;
        for (EditText et : digits) {
            if (et != null) {
                et.setText("");
            }
        }
        isInternalChange = false;
        requestFirstFocus();
        notifyChange();
    }

    public void setEnabled(boolean enabled) {
        for (EditText et : digits) {
            if (et != null) {
                et.setEnabled(enabled);
            }
        }
    }

    public void requestFirstFocus() {
        if (digits.length > 0 && digits[0] != null) {
            digits[0].requestFocus();
            InputMethodManager imm = (InputMethodManager) digits[0].getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(digits[0], InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }
}
