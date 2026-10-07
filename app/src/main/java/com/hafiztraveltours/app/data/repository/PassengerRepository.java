package com.hafiztraveltours.app.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.hafiztraveltours.app.models.DocumentDto;
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.network.ApiService;
import com.hafiztraveltours.app.network.ProfileResponseDto;
import com.hafiztraveltours.app.network.UserDto;
import com.hafiztraveltours.app.utils.SecurePrefs;
import com.hafiztraveltours.app.utils.SessionManager;

import java.util.List;

import retrofit2.Call;

/**
 * Passenger-details data access (H1/Phase 8). Owns the traveller-gate API calls
 * plus session/profile reads for this screen. No Views, Toasts, Dialogs, Intents
 * or Activity handling — callers track and cancel the returned Calls.
 *
 * <p>Phase-5 contract: refreshed profile persistence preserves any stored token
 * (same rule as before); nothing here writes tokens directly.
 */
public class PassengerRepository {

    private final Context appContext;
    private final SessionManager session;

    public PassengerRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.session = SessionManager.getInstance(this.appContext);
    }

    private ApiService api() {
        return ApiClient.getApiService();
    }

    /** Fresh documents Call per invocation; caller owns cancellation. */
    public Call<ApiResponse<List<DocumentDto>>> documentsCall() {
        return api().getUserDocuments();
    }

    /** Fresh profile Call per invocation; caller owns cancellation. */
    public Call<ApiResponse<ProfileResponseDto>> profileCall() {
        return api().getMe();
    }

    public boolean isLoggedIn() {
        return session.isLoggedIn();
    }

    public UserDto getSessionUser() {
        return session.getUser();
    }

    public String getUserPhone() {
        return session.getUserPhone();
    }

    public String getUserEmail() {
        return session.getUserEmail();
    }

    /**
     * Persists a freshly fetched profile, preserving any stored token
     * (identical rule to the previous inline implementation).
     */
    public void persistRefreshedUser(UserDto user) {
        if (user == null) return;
        String tok = session.getToken();
        if (tok != null && !tok.trim().isEmpty()) {
            session.saveAuthSession(tok, user);
        } else {
            session.saveUser(user);
        }
    }

    public void saveDocuments(List<DocumentDto> docs) {
        session.saveDocuments(docs);
    }

    public List<DocumentDto> getDocuments() {
        return session.getDocuments();
    }

    private SharedPreferences userPrefs() {
        return SecurePrefs.wrap(appContext, "user_profile");
    }

    private static String pref(SharedPreferences prefs, String key) {
        try {
            String v = prefs.getString(key, "");
            return v != null ? v : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Effective lead profile: session user with blank fields backfilled from the
     * locally kept profile extras (same merge as before; never mutates the session).
     */
    public UserDto getEffectiveProfile() {
        return mergeExtras(getSessionUser());
    }

    /** Same extras merge applied to a freshly fetched profile (never mutates the session). */
    public UserDto mergeExtras(UserDto user) {
        if (user == null) user = new UserDto();
        SharedPreferences prefs = userPrefs();
        if (isBlank(user.name)) user.name = pref(prefs, "name");
        if (isBlank(user.phone)) user.phone = pref(prefs, "phone");
        if (isBlank(user.gender)) user.gender = pref(prefs, "gender");
        if (isBlank(user.dateOfBirth)) user.dateOfBirth = pref(prefs, "date_of_birth");
        if (isBlank(user.icNumber)) user.icNumber = pref(prefs, "ic_no");
        if (isBlank(user.passportNumber)) user.passportNumber = pref(prefs, "passport_no");
        if (isBlank(user.passportExpiryDate)) user.passportExpiryDate = pref(prefs, "passport_expiry");
        if (isBlank(user.issuingCountry)) user.issuingCountry = pref(prefs, "issuing_country");
        if (isBlank(user.addressLine1)) user.addressLine1 = pref(prefs, "address_line_1");
        if (isBlank(user.address)) user.address = pref(prefs, "address");
        if (isBlank(user.clothesSize)) user.clothesSize = pref(prefs, "clothes_size");
        if (isBlank(user.nationality)) user.nationality = pref(prefs, "nationality");
        if (isBlank(user.emergencyName)) user.emergencyName = pref(prefs, "emergency_name");
        if (isBlank(user.emergencyPhone)) user.emergencyPhone = pref(prefs, "emergency_phone");
        return user;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
