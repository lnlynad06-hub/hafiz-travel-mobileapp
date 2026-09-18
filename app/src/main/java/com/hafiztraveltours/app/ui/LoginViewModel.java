package com.hafiztraveltours.app.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.data.repository.LoginRepository;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.network.AuthResponse;
import com.hafiztraveltours.app.network.GoogleLoginRequest;
import com.hafiztraveltours.app.network.LoginRequest;
import com.hafiztraveltours.app.network.UserDto;
import com.hafiztraveltours.app.utils.ApiOpResult;
import com.hafiztraveltours.app.utils.SingleEvent;
import com.hafiztraveltours.app.utils.Validator;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Login screen state + coordination (H1/Phase 11). Holds no Views and no Activity
 * references — only the Application context owned by the Repository.
 *
 * <p>Request discipline: one active Call per flow, no automatic retries (a retry
 * could duplicate server-side effects), in-flight Calls cancelled in
 * {@link #onCleared()} only (never on rotation). The UI button disable remains
 * the duplicate-tap guard, mirrored by internal busy flags.
 */
public class LoginViewModel extends AndroidViewModel {

    /** Submit-time validation errors as string IDs (0 = valid). */
    public static final class LoginErrors {
        public final int emailErr;
        public final int passErr;

        public LoginErrors(int emailErr, int passErr) {
            this.emailErr = emailErr;
            this.passErr = passErr;
        }

        public boolean hasErrors() {
            return emailErr != 0 || passErr != 0;
        }
    }

    /** Remember-me UI state (plain data; Activity renders). */
    public static final class RememberState {
        public final boolean remember;
        public final String savedEmail;

        public RememberState(boolean remember, String savedEmail) {
            this.remember = remember;
            this.savedEmail = savedEmail != null ? savedEmail : "";
        }
    }

    private final LoginRepository repository;

    private final MutableLiveData<SingleEvent<ApiOpResult>> loginOp = new MutableLiveData<>();
    private final MutableLiveData<SingleEvent<ApiOpResult>> googleOp = new MutableLiveData<>();
    private final MutableLiveData<SingleEvent<ApiOpResult>> forgotOp = new MutableLiveData<>();

    private Call<?> loginCall;
    private Call<?> googleCall;
    private Call<?> forgotCall;
    private boolean loginBusy;
    private boolean googleBusy;
    private boolean forgotBusy;

    public LoginViewModel(@NonNull Application application) {
        super(application);
        repository = new LoginRepository(application);
    }

    public LiveData<SingleEvent<ApiOpResult>> getLoginOp() {
        return loginOp;
    }

    public LiveData<SingleEvent<ApiOpResult>> getGoogleOp() {
        return googleOp;
    }

    public LiveData<SingleEvent<ApiOpResult>> getForgotOp() {
        return forgotOp;
    }

    public void clearSession() {
        repository.clearSession();
    }

    public RememberState rememberState() {
        return new RememberState(
                repository.isRememberMeChecked(), repository.savedEmail());
    }

    public void saveRememberMe(String email, boolean remember) {
        repository.saveRememberMe(email, remember);
    }

    // ---------- validation (same rules/messages as before) ----------

    public LoginErrors validateLogin(String email, String password) {
        return new LoginErrors(
                Validator.email(email, R.string.login_email_invalid),
                Validator.loginPassword(password, R.string.login_password_required));
    }

    public int validateForgotEmail(String email) {
        return Validator.email(email, R.string.reset_password_invalid_email);
    }

    // ---------- login ----------

    /** Submits login once; ignores re-entry while busy. */
    public void login(String email, String password) {
        if (loginBusy) return;
        loginBusy = true;
        cancel(loginCall);
        Call<ApiResponse<AuthResponse>> call =
                repository.loginCall(new LoginRequest(email, password));
        loginCall = call;
        final String emailSnapshot = email;
        call.enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call,
                                   Response<ApiResponse<AuthResponse>> response) {
                loginBusy = false;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AuthResponse authData = response.body().data;
                    String token = authData != null ? authData.token : "";
                    UserDto user = authData != null ? authData.user : null;
                    repository.persistAuthResult(token, user,
                            new UserDto("1", usernameOf(emailSnapshot), emailSnapshot, ""));
                    loginOp.setValue(new SingleEvent<>(ApiOpResult.success()));
                } else {
                    loginOp.setValue(new SingleEvent<>(
                            ApiOpResult.failure(response, R.string.login_failed_default)));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                loginBusy = false;
                loginOp.setValue(new SingleEvent<>(
                        ApiOpResult.failure(t, R.string.err_network)));
            }
        });
    }

    /** Submits Google login once; ignores re-entry while busy. */
    public void googleLogin(String email, String name, String googleId, String avatar) {
        if (googleBusy) return;
        googleBusy = true;
        cancel(googleCall);
        Call<ApiResponse<AuthResponse>> call =
                repository.googleLoginCall(new GoogleLoginRequest(email, name, googleId, avatar));
        googleCall = call;
        final String emailSnapshot = email;
        final String nameSnapshot = name;
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
                            new UserDto("1", nameSnapshot, emailSnapshot, ""));
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

    /** Submits forgot-password once; ignores re-entry while busy. */
    public void forgotPassword(String email) {
        if (forgotBusy) return;
        forgotBusy = true;
        cancel(forgotCall);
        Call<ApiResponse<Object>> call = repository.forgotPasswordCall(email);
        forgotCall = call;
        call.enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call,
                                   Response<ApiResponse<Object>> response) {
                forgotBusy = false;
                if (response.isSuccessful()) {
                    forgotOp.setValue(new SingleEvent<>(ApiOpResult.success()));
                } else {
                    forgotOp.setValue(new SingleEvent<>(
                            ApiOpResult.failure(response, R.string.reset_password_failed)));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                forgotBusy = false;
                forgotOp.setValue(new SingleEvent<>(
                        ApiOpResult.failure(t, R.string.err_network)));
            }
        });
    }

    private static String usernameOf(String email) {
        if (email == null || !email.contains("@")) return email != null ? email : "";
        return email.split("@")[0];
    }

    private static void cancel(Call<?> call) {
        if (call != null) call.cancel();
    }

    @Override
    protected void onCleared() {
        cancel(loginCall);
        cancel(googleCall);
        cancel(forgotCall);
        super.onCleared();
    }
}
