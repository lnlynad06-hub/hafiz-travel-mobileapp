package com.hafiztraveltours.app.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.data.repository.ProfileRepository;
import com.hafiztraveltours.app.models.DocumentDto;
import com.hafiztraveltours.app.models.ProfileStatsDto;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.network.ProfileResponseDto;
import com.hafiztraveltours.app.network.UserDto;
import com.hafiztraveltours.app.utils.ApiOpResult;
import com.hafiztraveltours.app.utils.DocumentStatus;
import com.hafiztraveltours.app.utils.SingleEvent;
import com.hafiztraveltours.app.utils.Validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Profile screen state + coordination (H1/Step 3). Holds no Views and no Activity
 * references — only the Application context owned by the Repository. Survives
 * configuration changes; cancels in-flight calls in {@link #onCleared()}.
 *
 * <p>Error UX stays centralized: operations post {@link ApiOpResult} carrying the
 * raw Retrofit response/throwable plus a fallback string ID, and the Activity
 * resolves the message with {@code ApiErrors} (which needs a Context).
 */
public class ProfileViewModel extends AndroidViewModel {

    /** Readiness score + per-status counts (pure data; Activity renders). */
    public static final class Readiness {
        public final int score;
        public final int verified;
        public final int underReview;
        public final int rejected;

        public Readiness(int score, int verified, int underReview, int rejected) {
            this.score = score;
            this.verified = verified;
            this.underReview = underReview;
            this.rejected = rejected;
        }
    }

    /** Profile form validation errors as string IDs (0 = valid). */
    public static final class FormErrors {
        public final int nameErr;
        public final int nickErr;

        public FormErrors(int nameErr, int nickErr) {
            this.nameErr = nameErr;
            this.nickErr = nickErr;
        }

        public boolean hasErrors() {
            return nameErr != 0 || nickErr != 0;
        }
    }

    /** Change-password validation errors as string IDs (0 = valid). */
    public static final class PasswordErrors {
        public final int currentErr;
        public final int newErr;
        public final int confirmErr;

        public PasswordErrors(int currentErr, int newErr, int confirmErr) {
            this.currentErr = currentErr;
            this.newErr = newErr;
            this.confirmErr = confirmErr;
        }

        public boolean hasErrors() {
            return currentErr != 0 || newErr != 0 || confirmErr != 0;
        }
    }

    /** Edit-profile form snapshot (plain data; Activity fills it from inputs). */
    public static final class ProfileForm {
        public String name = "";
        public String nickname = "";
        public String phone = "";
        public String gender = "";
        public String ic = "";
        public String passport = "";
        public String expiry = "";
        public String issuingCountry = "";
        public String dob = "";
        public String nationality = "";
        public String addressLine1 = "";
        public String addressLine2 = "";
        public String postcode = "";
        public String city = "";
        public String state = "";
        public String country = "";
        public String combinedAddress = "";
        public String emergencyName = "";
        public String emergencyPhone = "";
        public boolean mahramYes;
        public String mahramName = "";
        public String mahramRelationship = "";

        /** API body — same keys/emptiness rules as before. */
        public Map<String, String> toApiBody() {
            Map<String, String> body = new HashMap<>();
            body.put("name", name);
            if (!nickname.isEmpty()) body.put("nickname", nickname);
            body.put("phone", phone);
            if (!gender.isEmpty()) body.put("gender", gender);
            if (!ic.isEmpty()) body.put("ic_number", ic);
            if (!passport.isEmpty()) body.put("passport_number", passport);
            if (!expiry.isEmpty()) body.put("passport_expiry_date", expiry);
            if (!issuingCountry.isEmpty()) body.put("issuing_country", issuingCountry);
            if (!dob.isEmpty()) body.put("date_of_birth", dob);
            if (!nationality.isEmpty()) body.put("nationality", nationality);
            body.put("address_line_1", addressLine1);
            body.put("address_line_2", addressLine2);
            body.put("postcode", postcode);
            body.put("city", city);
            body.put("state", state);
            body.put("country", country);
            body.put("address", combinedAddress);
            if (!emergencyName.isEmpty()) body.put("emergency_name", emergencyName);
            if (!emergencyPhone.isEmpty()) body.put("emergency_phone", emergencyPhone);
            return body;
        }

        /** Local extras persistence payload (same keys as before). */
        public Map<String, String> toExtras() {
            Map<String, String> out = new HashMap<>();
            out.put("ic_no", ic);
            out.put("gender", gender);
            out.put("date_of_birth", dob);
            out.put("nationality", nationality);
            out.put("address_line_1", addressLine1);
            out.put("address_line_2", addressLine2);
            out.put("postcode", postcode);
            out.put("city", city);
            out.put("state", state);
            out.put("country", country);
            out.put("address", combinedAddress);
            out.put("passport_no", passport);
            out.put("passport_expiry", expiry);
            out.put("issuing_country", issuingCountry);
            out.put("emergency_name", emergencyName);
            out.put("emergency_phone", emergencyPhone);
            out.put("mahram_name", mahramName);
            out.put("mahram_relationship", mahramRelationship);
            return out;
        }

        public Map<String, Boolean> toFlags() {
            Map<String, Boolean> out = new HashMap<>();
            out.put("mahram_applicable", mahramYes);
            return out;
        }
    }

    private final ProfileRepository repository;

    private final MutableLiveData<ProfileStatsDto> statsData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> statsLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<DocumentDto>> docsData = new MutableLiveData<>();
    private final MutableLiveData<UserDto> userData = new MutableLiveData<>();
    private final MutableLiveData<Readiness> readiness = new MutableLiveData<>();
    private final MutableLiveData<SingleEvent<ApiOpResult>> saveOp = new MutableLiveData<>();
    private final MutableLiveData<SingleEvent<ApiOpResult>> passwordOp = new MutableLiveData<>();
    private final MutableLiveData<SingleEvent<ApiOpResult>> uploadOp = new MutableLiveData<>();

    private Call<?> statsCall;
    private Call<?> docsCall;
    private Call<?> saveCall;
    private Call<?> passwordCall;
    private Call<?> uploadCall;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        repository = new ProfileRepository(application);
    }

    public LiveData<ProfileStatsDto> getStatsData() {
        return statsData;
    }

    public LiveData<Boolean> getStatsLoading() {
        return statsLoading;
    }

    public LiveData<List<DocumentDto>> getDocsData() {
        return docsData;
    }

    public LiveData<UserDto> getUserData() {
        return userData;
    }

    public LiveData<Readiness> getReadiness() {
        return readiness;
    }

    public LiveData<SingleEvent<ApiOpResult>> getSaveOp() {
        return saveOp;
    }

    public LiveData<SingleEvent<ApiOpResult>> getPasswordOp() {
        return passwordOp;
    }

    public LiveData<SingleEvent<ApiOpResult>> getUploadOp() {
        return uploadOp;
    }

    // ---------- synchronous repository passthroughs (rendering/prefill) ----------

    public boolean isLoggedIn() {
        return repository.isLoggedIn();
    }

    public UserDto getSessionUser() {
        return repository.getSessionUser();
    }

    public String getUserName() {
        return repository.getUserName();
    }

    public String getUserNickname() {
        return repository.getUserNickname();
    }

    public String getUserEmail() {
        return repository.getUserEmail();
    }

    public String getUserPhone() {
        return repository.getUserPhone();
    }

    public ProfileStatsDto cachedStats() {
        return repository.cachedStats();
    }

    public Map<String, String> readProfileExtras() {
        return repository.readProfileExtras();
    }

    public boolean readProfileFlag(String key, boolean defValue) {
        return repository.readProfileFlag(key, defValue);
    }

    public void refreshLocalUser() {
        userData.setValue(repository.getSessionUser());
    }

    public void logout() {
        repository.logout();
        userData.setValue(null);
    }

    // ---------- loads ----------

    /** Stats + chained silent documents refresh (same sequence as before). */
    public void loadStats() {
        if (!repository.isLoggedIn()) {
            statsLoading.setValue(false);
            return;
        }
        cancel(statsCall);
        statsLoading.setValue(true);
        Call<ApiResponse<ProfileStatsDto>> call = repository.statsCall();
        statsCall = call;
        call.enqueue(new Callback<ApiResponse<ProfileStatsDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<ProfileStatsDto>> call,
                                   Response<ApiResponse<ProfileStatsDto>> response) {
                statsLoading.setValue(false);
                ProfileStatsDto stats =
                        (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) ? response.body().data : null;
                if (stats != null) {
                    repository.persistStats(stats);
                    statsData.setValue(stats);
                    fetchDocumentsInternal();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ProfileStatsDto>> call, Throwable t) {
                statsLoading.setValue(false);
            }
        });
    }

    /** Documents refresh for the vault sheet (shares the chained fetch's call slot). */
    public void refreshDocuments() {
        if (!repository.isLoggedIn()) return;
        fetchDocumentsInternal();
    }

    private void fetchDocumentsInternal() {
        cancel(docsCall);
        Call<ApiResponse<List<DocumentDto>>> call = repository.documentsCall();
        docsCall = call;
        call.enqueue(new Callback<ApiResponse<List<DocumentDto>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<DocumentDto>>> call,
                                   Response<ApiResponse<List<DocumentDto>>> response) {
                List<DocumentDto> docs =
                        (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) ? response.body().data : null;
                if (docs != null) {
                    docsData.setValue(docs);
                    refreshReadiness(docs);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<DocumentDto>>> call, Throwable t) {
            }
        });
    }

    /** Recomputes readiness from session user + local extras + latest docs. */
    public void refreshReadiness() {
        List<DocumentDto> docs = docsData.getValue();
        refreshReadiness(docs != null ? docs : new ArrayList<>());
    }

    private void refreshReadiness(List<DocumentDto> docs) {
        Map<String, DocumentDto> byCode = new HashMap<>();
        for (DocumentDto d : docs) {
            if (d != null && d.documentCode != null) {
                byCode.put(d.documentCode.toLowerCase(), d);
            }
        }
        readiness.setValue(computeReadiness(
                repository.getSessionUser(), repository.readProfileExtras(), byCode));
    }

    /** Pure readiness math — identical weights to the previous inline implementation. */
    static Readiness computeReadiness(UserDto user, Map<String, String> ex,
                                      Map<String, DocumentDto> docsByCode) {
        int score = 0;
        int verified = 0;
        int underReview = 0;
        int rejected = 0;

        String fullName = str(ex, "name");
        if (fullName.isEmpty() && user != null && user.name != null) fullName = user.name.trim();
        String passportNo = str(ex, "passport_no");
        if (passportNo.isEmpty() && user != null && user.passportNumber != null) {
            passportNo = user.passportNumber.trim();
        }
        String icNo = str(ex, "ic_no");
        if (icNo.isEmpty() && user != null && user.icNumber != null) icNo = user.icNumber.trim();
        String emergName = str(ex, "emergency_name");
        String address = str(ex, "address");
        if (address.isEmpty() && user != null && user.address != null) address = user.address.trim();

        if (!fullName.isEmpty()) score += 15;
        if (!icNo.isEmpty()) score += 20;
        if (!passportNo.isEmpty()) score += 20;
        if (!address.isEmpty()) score += 15;
        if (!emergName.isEmpty()) score += 10;

        if (docsByCode != null) {
            for (Map.Entry<String, DocumentDto> e : docsByCode.entrySet()) {
                DocumentStatus st = DocumentStatus.from(e.getValue());
                if (st == DocumentStatus.VERIFIED) verified++;
                else if (st == DocumentStatus.PENDING) underReview++;
                else if (st == DocumentStatus.REJECTED) rejected++;
            }
            if (DocumentStatus.from(docsByCode.get("passport")).countsAsUploaded()) score += 7;
            if (DocumentStatus.from(docsByCode.get("ic")).countsAsUploaded()) score += 7;
            if (DocumentStatus.from(docsByCode.get("passport_photo")).countsAsUploaded()) score += 6;
        }
        if (score >= 100) score = 100;
        return new Readiness(score, verified, underReview, rejected);
    }

    private static String str(Map<String, String> ex, String key) {
        if (ex == null) return "";
        String v = ex.get(key);
        return v != null ? v.trim() : "";
    }

    // ---------- mutations ----------

    /** Local-first extras write (same order as before: prefs first, then API). */
    public void saveProfileExtras(ProfileForm form) {
        repository.writeProfileExtras(form.toExtras(), form.toFlags());
    }

    public void saveNotificationEnabled(boolean enabled) {
        Map<String, Boolean> flags = new HashMap<>();
        flags.put("notifications_enabled", enabled);
        repository.writeProfileExtras(null, flags);
    }

    public void updateProfile(ProfileForm form) {
        cancel(saveCall);
        Call<ApiResponse<ProfileResponseDto>> call =
                repository.updateProfileCall(form.toApiBody());
        saveCall = call;
        call.enqueue(new Callback<ApiResponse<ProfileResponseDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<ProfileResponseDto>> call,
                                   Response<ApiResponse<ProfileResponseDto>> response) {
                ProfileResponseDto updated =
                        (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) ? response.body().data : null;
                if (updated == null || updated.user == null) {
                    saveOp.setValue(new SingleEvent<>(
                            ApiOpResult.failure(response, R.string.profile_update_failed)));
                    return;
                }
                // Phase-5 contract: user only — token untouched.
                repository.persistUser(updated.user);
                userData.setValue(updated.user);
                refreshReadiness();
                saveOp.setValue(new SingleEvent<>(ApiOpResult.success()));
            }

            @Override
            public void onFailure(Call<ApiResponse<ProfileResponseDto>> call, Throwable t) {
                saveOp.setValue(new SingleEvent<>(
                        ApiOpResult.failure(t, R.string.profile_update_failed)));
            }
        });
    }

    public void changePassword(String current, String next, String confirm) {
        cancel(passwordCall);
        Map<String, String> body = new HashMap<>();
        body.put("current_password", current);
        body.put("new_password", next);
        body.put("new_password_confirmation", confirm);
        Call<ApiResponse<Object>> call = repository.changePasswordCall(body);
        passwordCall = call;
        call.enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call,
                                   Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    passwordOp.setValue(new SingleEvent<>(ApiOpResult.success()));
                } else {
                    passwordOp.setValue(new SingleEvent<>(
                            ApiOpResult.failure(response, R.string.password_update_failed)));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                passwordOp.setValue(new SingleEvent<>(
                        ApiOpResult.failure(t, R.string.err_network)));
            }
        });
    }

    public void uploadDocument(String docCode, android.net.Uri uri,
                               String fileName, String mimeType) {
        cancel(uploadCall);
        ProfileRepository.UploadPayload payload = repository.readUploadFile(uri);
        if (payload == null || payload.bytes == null) {
            uploadOp.setValue(new SingleEvent<>(
                    ApiOpResult.failure((Throwable) null, R.string.doc_read_error)));
            return;
        }
        Call<ApiResponse<DocumentDto>> call = repository.uploadDocumentCall(
                docCode,
                fileName != null ? fileName : payload.fileName,
                mimeType != null ? mimeType : payload.mimeType,
                payload.bytes);
        uploadCall = call;
        call.enqueue(new Callback<ApiResponse<DocumentDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<DocumentDto>> call,
                                   Response<ApiResponse<DocumentDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    uploadOp.setValue(new SingleEvent<>(ApiOpResult.success()));
                } else {
                    uploadOp.setValue(new SingleEvent<>(
                            ApiOpResult.failure(response, R.string.doc_upload_failed_retry)));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<DocumentDto>> call, Throwable t) {
                uploadOp.setValue(new SingleEvent<>(
                        ApiOpResult.failure(t, R.string.doc_upload_network_error)));
            }
        });
    }

    // ---------- pure validation + formatting (same rules/messages as before) ----------

    public FormErrors validateProfileForm(String name, String nickname) {
        return new FormErrors(
                Validator.fullName(name, R.string.err_name_required),
                Validator.username(nickname, R.string.err_nickname_required));
    }

    public PasswordErrors validatePasswordForm(String current, String next, String confirm) {
        return new PasswordErrors(
                Validator.required(current, R.string.err_current_password_required),
                Validator.newPassword(next, R.string.err_password_short, R.string.err_password_short),
                Validator.passwordConfirm(next, confirm, R.string.err_password_mismatch));
    }

    /** Upload file rules mirror the previous inline checks exactly. */
    public int validateUploadFile(String fileName, String mimeType, long fileSize, boolean photoOnly) {
        String lower = fileName != null ? fileName.toLowerCase(java.util.Locale.ROOT) : "";
        boolean valid = lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".png") || (!photoOnly && lower.endsWith(".pdf"));
        if (mimeType != null) {
            if (mimeType.contains("image/jpeg") || mimeType.contains("image/png")) {
                valid = true;
            } else if (!photoOnly && mimeType.contains("application/pdf")) {
                valid = true;
            }
        }
        if (!valid) {
            return photoOnly ? R.string.err_file_format_photo : R.string.err_file_format;
        }
        if (fileSize > 5 * 1024 * 1024) return R.string.err_file_size;
        if (fileSize <= 0) return R.string.err_file_empty;
        return 0;
    }

    /** Backend gender value from display text (same mapping as before). */
    public static String normalizeGender(String display) {
        if (display == null) return "";
        if (display.equalsIgnoreCase("Lelaki") || display.equalsIgnoreCase("Male")) return "Male";
        if (display.equalsIgnoreCase("Perempuan") || display.equalsIgnoreCase("Female")) return "Female";
        return display;
    }

    /** Address summary line from parts (same join rules as before). */
    public static String combineAddress(String line1, String line2, String postcode,
                                        String city, String state, String country) {
        StringBuilder sb = new StringBuilder();
        if (!isEmpty(line1)) sb.append(line1);
        if (!isEmpty(line2)) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(line2);
        }
        if (!isEmpty(postcode) || !isEmpty(city)) {
            if (sb.length() > 0) sb.append(", ");
            if (!isEmpty(postcode)) sb.append(postcode).append(" ");
            if (!isEmpty(city)) sb.append(city);
        }
        if (!isEmpty(state)) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(state);
        }
        if (!isEmpty(country)) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(country);
        }
        return sb.toString();
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private static void cancel(Call<?> call) {
        if (call != null) call.cancel();
    }

    @Override
    protected void onCleared() {
        cancel(statsCall);
        cancel(docsCall);
        cancel(saveCall);
        cancel(passwordCall);
        cancel(uploadCall);
        super.onCleared();
    }
}
