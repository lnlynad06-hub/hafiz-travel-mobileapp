package com.hafiztraveltours.app.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.hafiztraveltours.app.models.DocumentDto;
import com.hafiztraveltours.app.models.ProfileStatsDto;
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.network.ApiService;
import com.hafiztraveltours.app.network.ProfileResponseDto;
import com.hafiztraveltours.app.network.UserDto;
import com.hafiztraveltours.app.utils.SecurePrefs;
import com.hafiztraveltours.app.utils.SessionManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;

/**
 * Profile data access (H1/Step 2). Owns all Retrofit calls for the Profile screen
 * plus session/local persistence for profile data. No Views, no Toasts, no Activity
 * lifecycle handling — callers (ProfileViewModel) track and cancel the returned Calls.
 *
 * <p>Phase-5 contract: user data is persisted via {@code saveUser()} only; the auth
 * token is never written here. Uses the existing ApiService/ApiErrors infrastructure.
 */
public class ProfileRepository {

    private final Context appContext;
    private final SessionManager session;

    public ProfileRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.session = SessionManager.getInstance(this.appContext);
    }

    // ---------- API (fresh Call per invocation; caller owns cancellation) ----------

    public Call<ApiResponse<ProfileStatsDto>> statsCall() {
        return api().getProfileStats();
    }

    public Call<ApiResponse<List<DocumentDto>>> documentsCall() {
        return api().getUserDocuments();
    }

    public Call<ApiResponse<ProfileResponseDto>> updateProfileCall(Map<String, String> body) {
        return api().updateProfile(body);
    }

    public Call<ApiResponse<Object>> changePasswordCall(Map<String, String> body) {
        return api().changePassword(body);
    }

    public Call<ApiResponse<DocumentDto>> uploadDocumentCall(
            String docCode, String fileName, String mimeType, byte[] bytes) {
        RequestBody requestFile = RequestBody.create(
                MediaType.parse(mimeType != null ? mimeType : "application/octet-stream"), bytes);
        MultipartBody.Part file =
                MultipartBody.Part.createFormData("file", fileName, requestFile);
        RequestBody codeBody = RequestBody.create(MediaType.parse("text/plain"), docCode);
        return api().uploadUserDocument(codeBody, file);
    }

    private ApiService api() {
        return ApiClient.getApiService();
    }

    // ---------- session (user data only — never the token) ----------

    public boolean isLoggedIn() {
        return session.isLoggedIn();
    }

    public UserDto getSessionUser() {
        return session.getUser();
    }

    public String getUserName() {
        return session.getUserName();
    }

    public String getUserNickname() {
        return session.getUserNickname();
    }

    public String getUserEmail() {
        return session.getUserEmail();
    }

    public String getUserPhone() {
        return session.getUserPhone();
    }

    /** Persists updated profile user; existing token is preserved by SessionManager. */
    public void persistUser(UserDto user) {
        session.saveUser(user);
    }

    public void persistStats(ProfileStatsDto stats) {
        session.saveProfileStats(stats);
    }

    public ProfileStatsDto cachedStats() {
        return session.getProfileStats();
    }

    /** Fire-and-forget server logout + immediate local clear (same sequence as before). */
    public void logout() {
        try {
            api().logout().enqueue(new retrofit2.Callback<ApiResponse<Object>>() {
                @Override
                public void onResponse(retrofit2.Call<ApiResponse<Object>> call,
                                       retrofit2.Response<ApiResponse<Object>> response) {
                }

                @Override
                public void onFailure(retrofit2.Call<ApiResponse<Object>> call, Throwable t) {
                }
            });
        } catch (Exception ignored) {}
        session.clearSession();
    }

    // ---------- local profile extras (user_profile prefs) ----------

    private SharedPreferences userPrefs() {
        return SecurePrefs.wrap(appContext, "user_profile");
    }

    /** Snapshot of locally kept profile fields for prefill + readiness scoring. */
    public Map<String, String> readProfileExtras() {
        SharedPreferences prefs = userPrefs();
        Map<String, String> out = new HashMap<>();
        String[] keys = {
                "name", "ic_no", "passport_no", "passport_expiry", "issuing_country",
                "gender", "date_of_birth", "nationality", "clothes_size",
                "address", "address_line_1", "address_line_2", "postcode", "city",
                "state", "country", "emergency_name", "emergency_phone",
                "mahram_name", "mahram_relationship", "has_vaccine_cert"
        };
        for (String key : keys) {
            try {
                String v = prefs.getString(key, "");
                out.put(key, v != null ? v : "");
            } catch (Exception ignored) {
                out.put(key, "");
            }
        }
        return out;
    }

    public boolean readProfileFlag(String key, boolean defValue) {
        try {
            return userPrefs().getBoolean(key, defValue);
        } catch (Exception e) {
            return defValue;
        }
    }

    public void writeProfileExtras(Map<String, String> strings, Map<String, Boolean> flags) {
        SharedPreferences.Editor editor = userPrefs().edit();
        if (strings != null) {
            for (Map.Entry<String, String> e : strings.entrySet()) {
                editor.putString(e.getKey(), e.getValue() != null ? e.getValue() : "");
            }
        }
        if (flags != null) {
            for (Map.Entry<String, Boolean> e : flags.entrySet()) {
                editor.putBoolean(e.getKey(), e.getValue() != null && e.getValue());
            }
        }
        editor.apply();
    }

    // ---------- upload file IO ----------

    public static final class UploadPayload {
        public final String fileName;
        public final String mimeType;
        public final byte[] bytes;

        public UploadPayload(String fileName, String mimeType, byte[] bytes) {
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.bytes = bytes;
        }
    }

    /**
     * Reads display name, MIME type and bytes for a picker Uri.
     * Returns null when the content cannot be read (caller maps to doc_read_error).
     */
    public UploadPayload readUploadFile(Uri uri) {
        if (uri == null) return null;
        try {
            String fileName = "selected_file";
            String mimeType = appContext.getContentResolver().getType(uri);
            try (android.database.Cursor cursor =
                         appContext.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        String n = cursor.getString(nameIndex);
                        if (n != null && !n.isEmpty()) fileName = n;
                    }
                }
            } catch (Exception ignored) {}
            if (fileName == null || fileName.isEmpty()) {
                fileName = uri.getLastPathSegment() != null ? uri.getLastPathSegment() : "selected_file";
            }
            InputStream in = appContext.getContentResolver().openInputStream(uri);
            if (in == null) return null;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int n;
            while ((n = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, n);
            }
            buffer.flush();
            byte[] bytes = buffer.toByteArray();
            try {
                in.close();
            } catch (Exception ignored) {}
            return new UploadPayload(fileName, mimeType, bytes);
        } catch (Exception e) {
            return null;
        }
    }

    /** File size for validation without loading bytes (picker-Uri metadata path). */
    public long queryFileSize(Uri uri) {
        if (uri == null) return -1;
        try (android.database.Cursor cursor =
                     appContext.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                if (sizeIndex != -1) return cursor.getLong(sizeIndex);
            }
        } catch (Exception ignored) {}
        try {
            android.os.ParcelFileDescriptor pfd =
                    appContext.getContentResolver().openFileDescriptor(uri, "r");
            if (pfd != null) {
                long size = pfd.getStatSize();
                pfd.close();
                return size;
            }
        } catch (Exception ignored) {}
        return -1;
    }
}
