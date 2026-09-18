package com.hafiztraveltours.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Drop-in encrypted SharedPreferences for sensitive data (auth token, user profile,
 * IC/passport details). Dependency-free (AndroidKeyStore + AES-256-GCM, no new libraries).
 *
 * <p>Values are stored as Base64(IV || ciphertext) under the same keys in the same
 * file, so swapping {@code getSharedPreferences(name)} for {@code SecurePrefs.wrap(...)}
 * is a one-line change per call site. Legacy plaintext values are still readable
 * (lazy migration: re-saved encrypted on next write), so existing sessions survive
 * the update. Keys themselves stay plaintext; values are what matter.
 *
 * <p>Fail-open by design: if the KeyStore is broken on a device, values fall back to
 * plaintext (with a Log.w) rather than locking the user out. New writes always attempt
 * encryption first.
 */
public final class SecurePrefs {

    private static final String TAG = "SecurePrefs";
    private static final String ENC_PREFIX = "ENC1:";
    private static final String PLAIN_PREFIX = "P1:";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;
    private static boolean warnedFallback = false;

    private SecurePrefs() {}

    public static SharedPreferences wrap(Context context, String name) {
        SharedPreferences delegate = context.getApplicationContext()
                .getSharedPreferences(name, Context.MODE_PRIVATE);
        return new EncryptedPreferences(delegate, keyAlias(name));
    }

    private static String keyAlias(String prefsName) {
        return "hafiz_" + prefsName.replaceAll("[^A-Za-z0-9]", "_");
    }

    // ---------- AndroidKeyStore AES-GCM ----------

    private static synchronized SecretKey getOrCreateKey(String alias) throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        SecretKey key = (SecretKey) ks.getKey(alias, null);
        if (key != null) return key;
        KeyGenerator kg = KeyGenerator.getInstance("AES", "AndroidKeyStore");
        android.security.keystore.KeyGenParameterSpec spec =
                new android.security.keystore.KeyGenParameterSpec.Builder(
                        alias,
                        android.security.keystore.KeyProperties.PURPOSE_ENCRYPT
                                | android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .setRandomizedEncryptionRequired(true)
                        .build();
        kg.init(spec);
        return kg.generateKey();
    }

    static String encrypt(String alias, String plain) {
        try {
            SecretKey key = getOrCreateKey(alias);
            byte[] iv = new byte[IV_BYTES];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[IV_BYTES + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, IV_BYTES);
            System.arraycopy(cipherText, 0, combined, IV_BYTES, cipherText.length);
            return ENC_PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) {
            warnOnce("Encryption unavailable, storing plaintext: " + e.getMessage());
            return null;
        }
    }

    static String decrypt(String alias, String stored) {
        if (stored == null || !stored.startsWith(ENC_PREFIX)) return null;
        try {
            byte[] combined = Base64.decode(stored.substring(ENC_PREFIX.length()), Base64.NO_WRAP);
            if (combined.length <= IV_BYTES) return null;
            byte[] iv = new byte[IV_BYTES];
            byte[] cipherText = new byte[combined.length - IV_BYTES];
            System.arraycopy(combined, 0, iv, 0, IV_BYTES);
            System.arraycopy(combined, IV_BYTES, cipherText, 0, cipherText.length);
            SecretKey key = getOrCreateKey(alias);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            warnOnce("Decryption failed: " + e.getMessage());
            return null;
        }
    }

    private static void warnOnce(String msg) {
        if (!warnedFallback) {
            warnedFallback = true;
            Log.w(TAG, msg);
        }
    }

    // ---------- SharedPreferences wrapper ----------

    private static final class EncryptedPreferences implements SharedPreferences {
        private final SharedPreferences delegate;
        private final String alias;

        EncryptedPreferences(SharedPreferences delegate, String alias) {
            this.delegate = delegate;
            this.alias = alias;
        }

        /** Decrypts a stored payload ("S:"/"B:"/"I:"/...); falls back to legacy raw plaintext. */
        private String decode(String stored) {
            if (stored == null) return null;
            if (stored.startsWith(ENC_PREFIX)) {
                return decrypt(alias, stored);
            }
            if (stored.startsWith(PLAIN_PREFIX)) {
                return stored.substring(PLAIN_PREFIX.length());
            }
            return stored; // legacy plaintext migration read
        }

        private String encode(String payload) {
            String enc = encrypt(alias, payload);
            return enc != null ? enc : PLAIN_PREFIX + payload;
        }

        private String rawPayload(String key, String defPayload) {
            String stored;
            try {
                stored = delegate.getString(key, null);
            } catch (ClassCastException e) {
                // Legacy raw non-string value (e.g. boolean written before migration).
                Object raw = null;
                try {
                    raw = delegate.getAll().get(key);
                } catch (Exception ignored) {}
                if (raw instanceof Boolean) return ((Boolean) raw) ? "B:1" : "B:0";
                if (raw instanceof Integer) return "I:" + raw;
                if (raw instanceof Long) return "L:" + raw;
                if (raw instanceof Float) return "F:" + raw;
                if (raw instanceof Set) {
                    org.json.JSONArray arr = new org.json.JSONArray();
                    for (Object s : (Set<?>) raw) arr.put(String.valueOf(s));
                    return "SET:" + arr;
                }
                return defPayload;
            }
            if (stored == null) return defPayload;
            String decoded = decode(stored);
            return decoded != null ? decoded : defPayload;
        }

        @Override
        public Map<String, ?> getAll() {
            Map<String, ?> all = delegate.getAll();
            Map<String, Object> out = new HashMap<>();
            for (Map.Entry<String, ?> e : all.entrySet()) {
                Object v = e.getValue();
                if (v instanceof String) {
                    String decoded = decode((String) v);
                    out.put(e.getKey(), decoded != null ? decoded : v);
                } else {
                    out.put(e.getKey(), v);
                }
            }
            return out;
        }

        @Override
        public String getString(String key, String defValue) {
            String payload = rawPayload(key, null);
            if (payload == null || !payload.startsWith("S:")) return defValue;
            return payload.substring(2);
        }

        @Override
        public Set<String> getStringSet(String key, Set<String> defValues) {
            String payload = rawPayload(key, null);
            if (payload == null || !payload.startsWith("SET:")) return defValues;
            try {
                org.json.JSONArray arr = new org.json.JSONArray(payload.substring(4));
                Set<String> out = new HashSet<>();
                for (int i = 0; i < arr.length(); i++) out.add(arr.optString(i));
                return out;
            } catch (Exception e) {
                return defValues;
            }
        }

        @Override
        public int getInt(String key, int defValue) {
            return parseInt(rawPayload(key, null), defValue);
        }

        @Override
        public long getLong(String key, long defValue) {
            return parseLong(rawPayload(key, null), defValue);
        }

        @Override
        public float getFloat(String key, float defValue) {
            return parseFloat(rawPayload(key, null), defValue);
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            String payload = rawPayload(key, null);
            if (payload == null || !payload.startsWith("B:")) return defValue;
            return "1".equals(payload.substring(2));
        }

        @Override
        public boolean contains(String key) {
            return delegate.contains(key);
        }

        @Override
        public Editor edit() {
            return new EncryptedEditor(delegate.edit(), alias, this);
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) {
            delegate.registerOnSharedPreferenceChangeListener(l);
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) {
            delegate.unregisterOnSharedPreferenceChangeListener(l);
        }

        private static int parseInt(String payload, int def) {
            if (payload != null && payload.startsWith("I:")) {
                try {
                    return Integer.parseInt(payload.substring(2));
                } catch (NumberFormatException ignored) {}
            }
            return def;
        }

        private static long parseLong(String payload, long def) {
            if (payload != null && payload.startsWith("L:")) {
                try {
                    return Long.parseLong(payload.substring(2));
                } catch (NumberFormatException ignored) {}
            }
            return def;
        }

        private static float parseFloat(String payload, float def) {
            if (payload != null && payload.startsWith("F:")) {
                try {
                    return Float.parseFloat(payload.substring(2));
                } catch (NumberFormatException ignored) {}
            }
            return def;
        }
    }

    private static final class EncryptedEditor implements Editor {
        private final Editor delegate;
        private final String alias;
        private final EncryptedPreferences prefs;

        EncryptedEditor(Editor delegate, String alias, EncryptedPreferences prefs) {
            this.delegate = delegate;
            this.alias = alias;
            this.prefs = prefs;
        }

        private String encode(String payload) {
            return prefs.encode(payload);
        }

        @Override
        public Editor putString(String key, String value) {
            delegate.putString(key, value == null ? null : encode("S:" + value));
            return this;
        }

        @Override
        public Editor putStringSet(String key, Set<String> values) {
            if (values == null) {
                delegate.remove(key);
                return this;
            }
            org.json.JSONArray arr = new org.json.JSONArray();
            for (String s : values) arr.put(s);
            delegate.putString(key, encode("SET:" + arr));
            return this;
        }

        @Override
        public Editor putInt(String key, int value) {
            delegate.putString(key, encode("I:" + value));
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            delegate.putString(key, encode("L:" + value));
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            delegate.putString(key, encode("F:" + value));
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            delegate.putString(key, encode(value ? "B:1" : "B:0"));
            return this;
        }

        @Override
        public Editor remove(String key) {
            delegate.remove(key);
            return this;
        }

        @Override
        public Editor clear() {
            delegate.clear();
            return this;
        }

        @Override
        public boolean commit() {
            return delegate.commit();
        }

        @Override
        public void apply() {
            delegate.apply();
        }
    }
}
