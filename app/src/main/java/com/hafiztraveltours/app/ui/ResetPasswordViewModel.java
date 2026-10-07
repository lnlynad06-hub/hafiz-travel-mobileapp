package com.hafiztraveltours.app.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.network.ApiService;
import com.hafiztraveltours.app.utils.ApiOpResult;
import com.hafiztraveltours.app.utils.SingleEvent;
import com.hafiztraveltours.app.utils.Validator;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordViewModel extends AndroidViewModel {

    public static class ResetErrors {
        public final int passwordErr;
        public final int confirmErr;

        public ResetErrors(int passwordErr, int confirmErr) {
            this.passwordErr = passwordErr;
            this.confirmErr = confirmErr;
        }

        public boolean hasErrors() {
            return passwordErr != 0 || confirmErr != 0;
        }
    }

    private final MutableLiveData<SingleEvent<ApiOpResult>> resetOp = new MutableLiveData<>();
    private Call<?> resetCall;
    private boolean resetBusy;

    public ResetPasswordViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<SingleEvent<ApiOpResult>> getResetOp() {
        return resetOp;
    }

    public ResetErrors validateInputs(String password, String confirmPassword) {
        int passErr = Validator.newPassword(password, R.string.err_password_short, R.string.err_password_short);
        int confErr = Validator.passwordConfirm(password, confirmPassword, R.string.err_password_mismatch);
        return new ResetErrors(passErr, confErr);
    }

    public void resetPassword(String email, String resetToken, String password, String confirmPassword) {
        if (resetBusy) return;
        resetBusy = true;
        cancel(resetCall);

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("reset_token", resetToken);
        body.put("password", password);
        body.put("password_confirmation", confirmPassword);

        Call<ApiResponse<Object>> call = ApiClient.getApiService().resetPassword(body);
        resetCall = call;

        call.enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                resetBusy = false;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    resetOp.setValue(new SingleEvent<>(ApiOpResult.success()));
                } else {
                    resetOp.setValue(new SingleEvent<>(
                            ApiOpResult.failure(response, R.string.reset_password_failed)));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                resetBusy = false;
                resetOp.setValue(new SingleEvent<>(
                        ApiOpResult.failure(t, R.string.err_network)));
            }
        });
    }

    private static void cancel(Call<?> call) {
        if (call != null) call.cancel();
    }

    @Override
    protected void onCleared() {
        cancel(resetCall);
        super.onCleared();
    }
}
