package com.hafiztraveltours.app.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.data.repository.SignUpRepository;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.network.AuthResponse;
import com.hafiztraveltours.app.network.GoogleLoginRequest;
import com.hafiztraveltours.app.network.RegisterRequest;
import com.hafiztraveltours.app.network.UserDto;
import com.hafiztraveltours.app.utils.ApiOpResult;
import com.hafiztraveltours.app.utils.SingleEvent;
import com.hafiztraveltours.app.utils.Validator;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Registration screen state + coordination (H1/Phase 10). Holds no Views and no
 * Activity references — only the Application context owned by the Repository.
 *
 * <p>Request discipline: one active Call per flow, no automatic retries (a retry
 * could create a duplicate account), in-flight Calls cancelled in
 * {@link #onCleared()} only (never on rotation). The UI button disable remains
 * the duplicate-tap guard, mirrored by an internal busy flag.
 */
public class SignUpViewModel extends AndroidViewModel {

    /** Submit-time validation errors as string IDs (0 = valid). */
    public static final class SignUpErrors {
        public final int nameErr;
        public final int nickErr;
        public final int emailErr;
        public final int phoneErr;
        public final int passErr;
        public final int confirmErr;

        public SignUpErrors(int nameErr, int nickErr, int emailErr,
                            int phoneErr, int passErr, int confirmErr) {
            this.nameErr = nameErr;
            this.nickErr = nickErr;
            this.emailErr = emailErr;
            this.phoneErr = phoneErr;
            this.passErr = passErr;
            this.confirmErr = confirmErr;
        }

        public boolean hasErrors() {
            return nameErr != 0 || nickErr != 0 || emailErr != 0
                    || phoneErr != 0 || passErr != 0 || confirmErr != 0;
        }
    }

    private final SignUpRepository repository;

    private final MutableLiveData<SingleEvent<ApiOpResult>> registerOp = new MutableLiveData<>();
    private final MutableLiveData<SingleEvent<ApiOpResult>> googleOp = new MutableLiveData<>();

    private Call<?> registerCall;
    private Call<?> googleCall;
    private boolean registerBusy;
    private boolean googleBusy;

    public SignUpViewModel(@NonNull Application application) {
        super(application);
        repository = new SignUpRepository(application);
    }

    public LiveData<SingleEvent<ApiOpResult>> getRegisterOp() {
        return registerOp;
    }

    public LiveData<SingleEvent<ApiOpResult>> getGoogleOp() {
        return googleOp;
    }

    public void clearSession() {
        repository.clearSession();
    }

    // ---------- validation (same rules/messages as before) ----------

    public SignUpErrors validateSignUp(String name, String nickname, String email,
                                       String rawPhone, String password, String confirmPassword) {
        return new SignUpErrors(
                Validator.fullName(name, R.string.err_name_required),
                Validator.username(nickname, R.string.err_username_required),
                Validator.email(email, R.string.err_email_invalid),
                Validator.phone(rawPhone, false, R.string.err_phone_invalid),
                Validator.newPassword(password, R.string.err_password_short, R.string.err_password_short),
                Validator.passwordConfirm(password, confirmPassword, R.string.err_password_mismatch));
    }

    /**
     * Normalizes a raw phone number with the selected country code
     * (same rule as before: keeps existing +, strips leading 0, else prepends).
     */
    public static String normalizePhone(String rawPhone, String countryCode) {
        String raw = rawPhone != null ? rawPhone : "";
        String code = countryCode != null ? countryCode : "";
        String digits = raw.replaceAll("[^\\d]", "");
        if (raw.startsWith("+")) {
            return raw;
        } else if (raw.startsWith("0")) {
            return digits.length() > 1 ? code + digits.substring(1) : code + digits;
        } else {
            return code + digits;
        }
    }

    // ---------- registration ----------

    /** Submits registration once; ignores re-entry while busy (no duplicate accounts). */
    public void register(String name, String nickname, String email, String normalizedPhone,
                         String password, String confirmPassword) {
        if (registerBusy) return;
        registerBusy = true;
        cancel(registerCall);
        RegisterRequest request = new RegisterRequest(
                name, nickname, email, normalizedPhone, password, confirmPassword);
        Call<ApiResponse<AuthResponse>> call = repository.registerCall(request);
        registerCall = call;
        call.enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call,
                                   Response<ApiResponse<AuthResponse>> response) {
                registerBusy = false;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    registerOp.setValue(new SingleEvent<>(ApiOpResult.success()));
                } else {
                    registerOp.setValue(new SingleEvent<>(
                            ApiOpResult.failure(response, R.string.err_signup_failed)));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                registerBusy = false;
                registerOp.setValue(new SingleEvent<>(
                        ApiOpResult.failure(t, R.string.err_network)));
            }
        });
    }

    /** Submits Google login once; ignores re-entry while busy. */
    public void googleLogin(String email, String name, String googleId, String avatar) {
        if (googleBusy) return;
        googleBusy = true;
        cancel(googleCall);
        GoogleLoginRequest request = new GoogleLoginRequest(email, name, googleId, avatar);
        Call<ApiResponse<AuthResponse>> call = repository.googleLoginCall(request);
        googleCall = call;
        call.enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call,
                                   Response<ApiResponse<AuthResponse>> response) {
                googleBusy = false;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AuthResponse authData = response.body().data;
                    String token = authData != null ? authData.token : "";
                    UserDto user = authData != null ? authData.user : null;
                    repository.persistAuthResult(token, user,
                            new UserDto("1", name, email, ""));
                    googleOp.setValue(new SingleEvent<>(ApiOpResult.success()));
                } else {
                    googleOp.setValue(new SingleEvent<>(
                            ApiOpResult.failure(response, R.string.login_google_failed)));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                googleBusy = false;
                googleOp.setValue(new SingleEvent<>(
                        ApiOpResult.failure(t, R.string.err_network)));
            }
        });
    }

    private static void cancel(Call<?> call) {
        if (call != null) call.cancel();
    }

    @Override
    protected void onCleared() {
        cancel(registerCall);
        cancel(googleCall);
        super.onCleared();
    }
}
