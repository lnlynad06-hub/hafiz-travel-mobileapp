package com.hafiztraveltours.app.utils;

import android.view.HapticFeedbackConstants;
import android.view.View;

/**
 * Tap-feedback one-liners (M4 haptic portion). Preserves the two constants already
 * in use: VIRTUAL_KEY for buttons/cards, KEYBOARD_TAP for small icon toggles.
 */
public final class HapticUtil {

    private HapticUtil() {}

    /** Standard button/card tap. No-op on null. */
    public static void click(View v) {
        if (v != null) {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    /** Small icon toggle tap (favorite hearts, steppers). No-op on null. */
    public static void tap(View v) {
        if (v != null) {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
    }
}
