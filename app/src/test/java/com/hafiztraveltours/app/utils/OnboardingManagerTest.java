package com.hafiztraveltours.app.utils;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * Unit test for OnboardingManager null-safety and defaults.
 */
public class OnboardingManagerTest {

    @Test
    public void testNullContext_safeDefaultFalse() {
        assertFalse(OnboardingManager.isOnboardingCompleted(null));
        assertFalse(OnboardingManager.isPrayerFeatureEnabled(null));
        assertFalse(OnboardingManager.isPrayerTimesEnabled(null));
        assertFalse(OnboardingManager.isQiblaFeatureEnabled(null));
        assertFalse(OnboardingManager.isQiblaEnabled(null));
        assertFalse(OnboardingManager.isAzanFeatureEnabled(null));
        assertFalse(OnboardingManager.isAzanEnabled(null));
        assertFalse(OnboardingManager.isNotificationFeatureEnabled(null));
        assertFalse(OnboardingManager.isNotificationsEnabled(null));
        assertFalse(OnboardingManager.isBatteryPromptShown(null));
    }

    @Test
    public void testPrayerAndNotif_nullContextDefaults() {
        OnboardingManager.setPrayerFeatureEnabled(null, true);
        OnboardingManager.setQiblaFeatureEnabled(null, true);
        OnboardingManager.setAzanFeatureEnabled(null, true);
        OnboardingManager.setNotificationFeatureEnabled(null, true);
        OnboardingManager.setBatteryPromptShown(null, true);
        OnboardingManager.setOnboardingCompleted(null, true);

        assertFalse(OnboardingManager.isPrayerFeatureEnabled(null));
        assertFalse(OnboardingManager.isQiblaFeatureEnabled(null));
        assertFalse(OnboardingManager.isAzanFeatureEnabled(null));
        assertFalse(OnboardingManager.isNotificationFeatureEnabled(null));
        assertFalse(OnboardingManager.isBatteryPromptShown(null));
        assertFalse(OnboardingManager.isOnboardingCompleted(null));
    }
}
