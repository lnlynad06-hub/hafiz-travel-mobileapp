package com.hafiztraveltours.app.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.data.repository.PassengerRepository;
import com.hafiztraveltours.app.models.BookingRequest;
import com.hafiztraveltours.app.models.DocumentDto;
import com.hafiztraveltours.app.models.PackageDetail;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.network.ProfileResponseDto;
import com.hafiztraveltours.app.network.UserDto;
import com.hafiztraveltours.app.utils.DateFormats;
import com.hafiztraveltours.app.utils.TravellerMapper;
import com.hafiztraveltours.app.utils.Validator;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Passenger-details screen state + coordination (H1/Phase 8). Holds no Views and
 * no Activity references — only the Application context owned by the Repository.
 * Survives configuration changes; cancels in-flight calls in {@link #onCleared()}.
 *
 * <p>Rules preserved: lead comes from the master profile (rule A), exact pax count
 * (rule C), package-driven requirements (rule F), snapshot-at-creation (rule G),
 * departure/room data passes through untouched (rule I).
 */
public class PassengerDetailsViewModel extends AndroidViewModel {

    /** Lead-completeness gap (Activity maps each kind to a localized string). */
    public enum MissingKind {
        NOT_LOGGED_IN,
        NAME,
        IC,
        PASSPORT,
        PASSPORT_EXPIRY,
        PASSPORT_VALIDITY,
        PASSPORT_DOC,
        IC_DOC,
        CLOTHES_SIZE
    }

    public static final class MissingField {
        public final MissingKind kind;
        public final int validityMonths;

        public MissingField(MissingKind kind, int validityMonths) {
            this.kind = kind;
            this.validityMonths = validityMonths;
        }
    }

    /** Lead evaluation result (pure data; Activity renders). */
    public static final class LeadEvaluation {
        public final boolean complete;
        public final List<MissingField> missing;
        public final boolean hasPassportDoc;
        public final boolean hasIcDoc;
        public final boolean hasPhotoDoc;
        public final boolean reqPassport;
        public final boolean reqIc;
        public final boolean hasPassportValidityWarning;
        public final boolean hasPassportExpiryMissing;
        public final int reqValidityMonths;

        public LeadEvaluation(boolean complete, List<MissingField> missing,
                              boolean hasPassportDoc, boolean hasIcDoc,
                              boolean reqPassport, boolean reqIc) {
            this(complete, missing, hasPassportDoc, hasIcDoc, false, reqPassport, reqIc, false, false, 6);
        }

        public LeadEvaluation(boolean complete, List<MissingField> missing,
                              boolean hasPassportDoc, boolean hasIcDoc, boolean hasPhotoDoc,
                              boolean reqPassport, boolean reqIc) {
            this(complete, missing, hasPassportDoc, hasIcDoc, hasPhotoDoc, reqPassport, reqIc, false, false, 6);
        }

        public LeadEvaluation(boolean complete, List<MissingField> missing,
                              boolean hasPassportDoc, boolean hasIcDoc, boolean hasPhotoDoc,
                              boolean reqPassport, boolean reqIc,
                              boolean hasPassportValidityWarning, boolean hasPassportExpiryMissing,
                              int reqValidityMonths) {
            this.complete = complete;
            this.missing = missing;
            this.hasPassportDoc = hasPassportDoc;
            this.hasIcDoc = hasIcDoc;
            this.hasPhotoDoc = hasPhotoDoc;
            this.reqPassport = reqPassport;
            this.reqIc = reqIc;
            this.hasPassportValidityWarning = hasPassportValidityWarning;
            this.hasPassportExpiryMissing = hasPassportExpiryMissing;
            this.reqValidityMonths = reqValidityMonths;
        }
    }

    /** Raw additional-traveller form values (plain data; Activity reads card inputs). */
    public static final class TravellerInput {
        public String title = "";
        public String fullName = "";
        public String icNumber = "";
        public String passportNumber = "";
        public String passportExpiryDate = "";
        public String issuingCountry = "";
        public String dateOfBirth = "";
        public String gender = "";
        public String nationality = "";
        public String clothesSize = "";
        public String mahramText = "";
        public String relationship = "";
    }

    /** Per-traveller validation errors as string IDs (0 = valid). */
    public static final class TravellerErrors {
        public final int nameErr;
        public final int icErr;
        public final int passportErr;
        public final int expiryErr;

        public TravellerErrors(int nameErr, int icErr, int passportErr, int expiryErr) {
            this.nameErr = nameErr;
            this.icErr = icErr;
            this.passportErr = passportErr;
            this.expiryErr = expiryErr;
        }

        public boolean hasErrors() {
            return nameErr != 0 || icErr != 0 || passportErr != 0 || expiryErr != 0;
        }
    }

    /** Pax-count check outcome. */
    public enum PaxCheck {
        OK,
        UNDER,
        OVER
    }

    private final PassengerRepository repository;

    private final MutableLiveData<UserDto> profileData = new MutableLiveData<>();
    private final MutableLiveData<List<DocumentDto>> docsData = new MutableLiveData<>();
    private final MutableLiveData<LeadEvaluation> evaluation = new MutableLiveData<>();

    private Call<?> docsCall;
    private Call<?> profileCall;

    public PassengerDetailsViewModel(@NonNull Application application) {
        super(application);
        repository = new PassengerRepository(application);
    }

    public LiveData<UserDto> getProfileData() {
        return profileData;
    }

    public LiveData<List<DocumentDto>> getDocsData() {
        return docsData;
    }

    public LiveData<LeadEvaluation> getEvaluation() {
        return evaluation;
    }

    // ---------- synchronous repository passthroughs (rendering/prefill) ----------

    public boolean isLoggedIn() {
        return repository.isLoggedIn();
    }

    public String getUserPhone() {
        return repository.getUserPhone();
    }

    public String getUserEmail() {
        return repository.getUserEmail();
    }

    // ---------- loads (docs → profile chain, same sequence as before) ----------

    /**
     * Reloads documents then the fresh profile, then re-evaluates.
     * Failures fall back to cached/session data and still re-evaluate (as before).
     */
    public void loadProfile(PackageDetail pkg) {
        if (!repository.isLoggedIn()) {
            List<MissingField> missing = new ArrayList<>();
            missing.add(new MissingField(MissingKind.NOT_LOGGED_IN, 0));
            profileData.setValue(null);
            evaluation.setValue(new LeadEvaluation(
                    false, missing, false, false, true, true));
            return;
        }
        cancel(docsCall);
        cancel(profileCall);
        Call<ApiResponse<List<DocumentDto>>> documentsRequest = repository.documentsCall();
        docsCall = documentsRequest;
        documentsRequest.enqueue(new Callback<ApiResponse<List<DocumentDto>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<DocumentDto>>> call,
                                   Response<ApiResponse<List<DocumentDto>>> response) {
                List<DocumentDto> docs = null;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    docs = response.body().data != null ? response.body().data : new ArrayList<>();
                }
                if (docs != null) docsData.setValue(docs);
                fetchFreshProfile(pkg);
            }

            @Override
            public void onFailure(Call<ApiResponse<List<DocumentDto>>> call, Throwable t) {
                fetchFreshProfile(pkg);
            }
        });
    }

    private void fetchFreshProfile(PackageDetail pkg) {
        cancel(profileCall);
        Call<ApiResponse<ProfileResponseDto>> profileRequest = repository.profileCall();
        profileCall = profileRequest;
        profileRequest.enqueue(new Callback<ApiResponse<ProfileResponseDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<ProfileResponseDto>> call,
                                   Response<ApiResponse<ProfileResponseDto>> response) {
                UserDto fresh = null;
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().data != null
                        && response.body().data.user != null) {
                    fresh = response.body().data.user;
                    repository.persistRefreshedUser(fresh);
                }
                UserDto effective = (fresh != null)
                        ? repository.mergeExtras(fresh)
                        : repository.getEffectiveProfile();
                profileData.setValue(effective);
                evaluateLead(pkg, effective, docsData.getValue());
            }

            @Override
            public void onFailure(Call<ApiResponse<ProfileResponseDto>> call, Throwable t) {
                UserDto effective = repository.getEffectiveProfile();
                profileData.setValue(effective);
                evaluateLead(pkg, effective, docsData.getValue());
            }
        });
    }

    // ---------- lead evaluation (same rules/flags as before) ----------

    /** Evaluates lead completeness against package-driven requirements. */
    public void evaluateLead(PackageDetail pkg, UserDto user, List<DocumentDto> docs) {
        evaluation.setValue(checkLeadCompleteness(pkg, user, docs));
    }

    /** Pure evaluator for lead completeness (can book = required edit profile info complete). */
    public static LeadEvaluation checkLeadCompleteness(PackageDetail pkg, UserDto user, List<DocumentDto> docs) {
        boolean reqPassport = pkg != null ? pkg.requiresPassport : true;
        boolean reqIc = pkg != null ? pkg.requiresIc : true;
        boolean reqClothesSize = pkg != null ? pkg.requiresClothesSize : (pkg != null && pkg.isUmrah);
        int reqValidityMonths = pkg != null && pkg.passportValidityMonths > 0 ? pkg.passportValidityMonths : 6;

        if (user == null) user = new UserDto();
        List<DocumentDto> documents = docs != null ? docs : new ArrayList<>();
        List<MissingField> missing = new ArrayList<>();

        if (isBlank(user.name)) {
            missing.add(new MissingField(MissingKind.NAME, 0));
        }
        if (reqIc && isBlank(user.icNumber)) {
            missing.add(new MissingField(MissingKind.IC, 0));
        }
        if (reqPassport) {
            if (isBlank(user.passportNumber)) {
                missing.add(new MissingField(MissingKind.PASSPORT, 0));
            }
        }
        if (reqClothesSize && isBlank(user.clothesSize)) {
            missing.add(new MissingField(MissingKind.CLOTHES_SIZE, 0));
        }

        boolean hasPassport = hasDocWithFile(documents, "passport");
        boolean hasIc = hasDocWithFile(documents, "ic");
        boolean hasPhoto = hasDocWithFile(documents, "passport_photo");

        boolean hasPassportExpiryMissing = reqPassport && isBlank(user.passportExpiryDate);
        boolean hasPassportValidityWarning = false;
        if (reqPassport && !isBlank(user.passportExpiryDate)) {
            hasPassportValidityWarning = !DateFormats.meetsValidityMonths(
                    user.passportExpiryDate.trim(), reqValidityMonths);
        }

        return new LeadEvaluation(
                missing.isEmpty(), missing,
                hasPassport,
                hasIc,
                hasPhoto,
                reqPassport, reqIc,
                hasPassportValidityWarning,
                hasPassportExpiryMissing,
                reqValidityMonths);
    }

    private static boolean hasDocWithFile(List<DocumentDto> docs, String code) {
        for (DocumentDto d : docs) {
            if (d != null && code.equalsIgnoreCase(d.documentCode)) {
                if ((d.filePath != null && !d.filePath.isEmpty())
                        || com.hafiztraveltours.app.utils.DocumentStatus.from(d).countsAsUploaded()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // ---------- traveller validation + snapshot assembly ----------

    public PaxCheck checkPaxCount(int totalPax, int requiredPax) {
        if (totalPax < requiredPax) return PaxCheck.UNDER;
        if (totalPax > requiredPax) return PaxCheck.OVER;
        return PaxCheck.OK;
    }

    /** Validates one additional traveller (same rules/messages as before). */
    public TravellerErrors validateTraveller(TravellerInput in, boolean reqPassport, boolean reqIc) {
        return validateTravellerInput(in, reqPassport, reqIc);
    }

    /** Pure evaluator for additional traveller input. */
    public static TravellerErrors validateTravellerInput(TravellerInput in, boolean reqPassport, boolean reqIc) {
        int nameErr = Validator.fullName(in.fullName, R.string.passenger_err_name_required);
        int icErr = 0;
        if (reqIc) {
            icErr = Validator.travelId(in.icNumber, R.string.passenger_err_ic_required);
        }
        int passportErr = 0;
        int expiryErr = 0;
        if (reqPassport) {
            passportErr = Validator.travelId(in.passportNumber, R.string.passenger_err_passport_required);
            if (in.passportExpiryDate != null && !in.passportExpiryDate.trim().isEmpty()) {
                if (!DateFormats.isValidApiDate(in.passportExpiryDate.trim())) {
                    expiryErr = R.string.passenger_err_passport_expiry_required;
                }
            }
        }
        return new TravellerErrors(nameErr, icErr, passportErr, expiryErr);
    }

    /** Lead snapshot via TravellerMapper (frozen; later profile edits can't mutate it). */
    public BookingRequest.Passenger buildLeadPassenger(UserDto user) {
        return TravellerMapper.leadFromUser(
                getApplication(),
                user,
                repository.getUserPhone(),
                repository.getUserEmail(),
                "");
    }

    /** Additional-traveller snapshot from form values (same field mapping as before). */
    public BookingRequest.Passenger buildAdditionalPassenger(TravellerInput in) {
        BookingRequest.Passenger p = new BookingRequest.Passenger();
        p.isLead = false;
        p.title = in.title != null ? in.title.trim()
                : TravellerMapper.defaultTitle(getApplication());
        p.fullName = in.fullName != null ? in.fullName.trim() : "";
        p.icNumber = in.icNumber != null ? in.icNumber.trim() : "";
        p.passportNumber = in.passportNumber != null ? in.passportNumber.trim() : "";
        p.passportExpiryDate = in.passportExpiryDate != null ? in.passportExpiryDate.trim() : "";
        p.issuingCountry = in.issuingCountry != null ? in.issuingCountry.trim()
                : TravellerMapper.defaultCountry(getApplication());
        p.dateOfBirth = in.dateOfBirth != null ? in.dateOfBirth.trim() : "";
        p.gender = in.gender != null ? in.gender.trim() : "";
        p.nationality = in.nationality != null ? in.nationality.trim()
                : TravellerMapper.defaultNationality(getApplication());
        p.clothesSize = in.clothesSize != null ? in.clothesSize.trim() : "";
        p.icPassportNumber = (!p.passportNumber.isEmpty()) ? p.passportNumber : p.icNumber;
        p.mahramIndex = parseMahramIndex(in.mahramText);
        p.relationship = in.relationship != null ? in.relationship.trim() : "";
        p.isComplete = true;
        return p;
    }

    /** Parses "N. Name" mahram selections to 0-based indices (same rule as before). */
    static Integer parseMahramIndex(String mahramText) {
        if (mahramText == null) return null;
        String text = mahramText.trim();
        if (text.isEmpty()) return null;
        try {
            String idxStr = text.split("\\.")[0].trim();
            return Integer.parseInt(idxStr) - 1;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void cancel(Call<?> call) {
        if (call != null) call.cancel();
    }

    @Override
    protected void onCleared() {
        cancel(docsCall);
        cancel(profileCall);
        super.onCleared();
    }
}
