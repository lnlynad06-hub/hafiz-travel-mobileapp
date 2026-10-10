package com.hafiztraveltours.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying PrivacyPolicyManager persistence, version management,
 * and null-safety without external test dependencies.
 */
public class PrivacyPolicyManagerTest {

    @Test
    public void testOfficialPolicyUrl_matchesRequirement() {
        assertEquals("https://sites.google.com/view/hafiz-travel-huawei-policy/laman-utama",
                PrivacyPolicyManager.OFFICIAL_POLICY_URL);
    }

    @Test
    public void testCurrentPolicyVersion_initialConstant() {
        assertEquals("1.0", PrivacyPolicyManager.CURRENT_POLICY_VERSION);
    }

    @Test
    public void testNullContext_safeDefaultFalse() {
        assertFalse(PrivacyPolicyManager.isPrivacyPolicyAccepted((Context) null));
        assertFalse(PrivacyPolicyManager.isPrivacyPolicyAccepted((Context) null, "1.0"));
        assertNull(PrivacyPolicyManager.getAcceptedPolicyVersion((Context) null));
        assertEquals(0L, PrivacyPolicyManager.getAcceptedTimestamp((Context) null));
    }

    @Test
    public void testNullPrefs_safeDefaultFalse() {
        assertFalse(PrivacyPolicyManager.isPrivacyPolicyAccepted((SharedPreferences) null));
        assertFalse(PrivacyPolicyManager.isPrivacyPolicyAccepted((SharedPreferences) null, "1.0"));
        assertNull(PrivacyPolicyManager.getAcceptedPolicyVersion((SharedPreferences) null));
        assertEquals(0L, PrivacyPolicyManager.getAcceptedTimestamp((SharedPreferences) null));
    }

    @Test
    public void testNullMutations_noException() {
        PrivacyPolicyManager.setPrivacyPolicyAccepted((Context) null, true);
        PrivacyPolicyManager.setPrivacyPolicyAccepted((SharedPreferences) null, true);
        PrivacyPolicyManager.setPrivacyPolicyAcceptedVersion((Context) null, "1.0", true);
        PrivacyPolicyManager.setPrivacyPolicyAcceptedVersion((SharedPreferences) null, "1.0", true);
        PrivacyPolicyManager.resetAcceptance((Context) null);
        PrivacyPolicyManager.resetAcceptance((SharedPreferences) null);

        assertFalse(PrivacyPolicyManager.isPrivacyPolicyAccepted((Context) null));
    }

    @Test
    public void testPersistenceFlow_acceptanceAndVersionGate() {
        SharedPreferences prefs = createMockSharedPreferences();

        // 1. Initial launch state: policy not accepted yet
        assertFalse(PrivacyPolicyManager.isPrivacyPolicyAccepted(prefs));
        assertNull(PrivacyPolicyManager.getAcceptedPolicyVersion(prefs));
        assertEquals(0L, PrivacyPolicyManager.getAcceptedTimestamp(prefs));

        // 2. User accepts current policy version (v1.0)
        PrivacyPolicyManager.setPrivacyPolicyAccepted(prefs, true);

        // 3. Acceptance verified persistently with current version
        assertTrue(PrivacyPolicyManager.isPrivacyPolicyAccepted(prefs));
        assertEquals("1.0", PrivacyPolicyManager.getAcceptedPolicyVersion(prefs));
        assertTrue(PrivacyPolicyManager.getAcceptedTimestamp(prefs) > 0);

        // 4. Same version is never asked again
        assertTrue(PrivacyPolicyManager.isPrivacyPolicyAccepted(prefs, "1.0"));

        // 5. If a new policy version requires renewed consent (e.g., v2.0),
        // check returns false so consent will be requested again
        assertFalse(PrivacyPolicyManager.isPrivacyPolicyAccepted(prefs, "2.0"));

        // 6. If user declines or revokes consent, acceptance is not saved
        PrivacyPolicyManager.setPrivacyPolicyAccepted(prefs, false);
        assertFalse(PrivacyPolicyManager.isPrivacyPolicyAccepted(prefs));
        assertNull(PrivacyPolicyManager.getAcceptedPolicyVersion(prefs));

        // 7. Reset acceptance clears preferences
        PrivacyPolicyManager.setPrivacyPolicyAccepted(prefs, true);
        assertTrue(PrivacyPolicyManager.isPrivacyPolicyAccepted(prefs));
        PrivacyPolicyManager.resetAcceptance(prefs);
        assertFalse(PrivacyPolicyManager.isPrivacyPolicyAccepted(prefs));
    }

    /**
     * In-memory mock for Android SharedPreferences interface using standard Java dynamic proxy.
     */
    private SharedPreferences createMockSharedPreferences() {
        final Map<String, Object> storage = new HashMap<>();

        final SharedPreferences.Editor editorProxy = (SharedPreferences.Editor) Proxy.newProxyInstance(
                SharedPreferences.Editor.class.getClassLoader(),
                new Class<?>[]{SharedPreferences.Editor.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        String name = method.getName();
                        if ("putString".equals(name)) {
                            storage.put((String) args[0], args[1]);
                            return proxy;
                        } else if ("putLong".equals(name)) {
                            storage.put((String) args[0], args[1]);
                            return proxy;
                        } else if ("remove".equals(name)) {
                            storage.remove((String) args[0]);
                            return proxy;
                        } else if ("clear".equals(name)) {
                            storage.clear();
                            return proxy;
                        } else if ("apply".equals(name) || "commit".equals(name)) {
                            return Boolean.TRUE;
                        }
                        return null;
                    }
                }
        );

        return (SharedPreferences) Proxy.newProxyInstance(
                SharedPreferences.class.getClassLoader(),
                new Class<?>[]{SharedPreferences.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        String name = method.getName();
                        if ("getString".equals(name)) {
                            Object val = storage.get((String) args[0]);
                            return val != null ? val : args[1];
                        } else if ("getLong".equals(name)) {
                            Object val = storage.get((String) args[0]);
                            return val != null ? ((Number) val).longValue() : ((Number) args[1]).longValue();
                        } else if ("edit".equals(name)) {
                            return editorProxy;
                        }
                        return null;
                    }
                }
        );
    }
}
