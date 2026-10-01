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

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerifyResetCodeViewModel extends AndroidViewModel {

    public static class VerifyResetResult {
        public final boolean success;
        public final String resetToken;
        public final String email;
        public final String errorMessage;
        public final int errorResId;

        public VerifyResetResult(boolean success, String resetToken, String email, String errorMessage, int errorResId) {
            this.success = success;
            this.resetToken = resetToken;
            this.email = email;
            this.errorMessage = errorMessage;
            this.errorResId = errorResId;
        }

        public static VerifyResetResult ok(String resetToken, String email) {
            return new VerifyResetResult(true, resetToken, email, null, 0);
        }

        public static VerifyResetResult err(String message, int resId) {
            return new VerifyResetResult(false, null, null, message, resId);
        }
    }

    private final MutableLiveData<SingleEvent<VerifyResetResult>> verifyOp = new MutableLiveData<>();
    private final MutableLiveData<SingleEvent<ApiOpResult>> resendOp = new MutableLiveData<>();

    private Call<?> verifyCall;
    private Call<?> resendCall;
    private boolean verifyBusy;
    private boolean resendBusy;

    public VerifyResetCodeViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<SingleEvent<VerifyResetResult>> getVerifyOp() {
        return verifyOp;
    }

    public LiveData<SingleEvent<ApiOpResult>> getResendOp() {
        return resendOp;
    }

    private ApiService api() {
        return ApiClient.getApiService();
    }

    public void verifyResetCode(String email, String code) {
        if (verifyBusy) return;
        verifyBusy = true;
        cancel(verifyCall);

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("code", code);

        Call<ApiResponse<Map<String, Object>>> call = api().verifyResetCode(body);
        verifyCall = call;

        call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                verifyBusy = false;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Map<String, Object> data = response.body().data;
                    String token = data != null && data.get("reset_token") != null ? String.valueOf(data.get("reset_token")) : "";
                    verifyOp.setValue(new SingleEvent<>(VerifyResetResult.ok(token, email)));
                } else {
                    String msg = com.hafiztraveltours.app.network.ApiErrors.userMessage(
                            getApplication(), response, R.string.reset_password_failed);
                    verifyOp.setValue(new SingleEvent<>(VerifyResetResult.err(msg, R.string.reset_password_failed)));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                verifyBusy = false;
                String msg = com.hafiztraveltours.app.network.ApiErrors.userMessage(
                        getApplication(), t, R.string.err_network);
                verifyOp.setValue(new SingleEvent<>(VerifyResetResult.err(msg, R.string.err_network)));
            }
        });
    }

    public void resendResetCode(String email) {
        if (resendBusy) return;
        resendBusy = true;
        cancel(resendCall);

        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        Call<ApiResponse<Object>> call = api().resendResetCode(body);
        resendCall = call;

        call.enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                resendBusy = false;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    resendOp.setValue(new SingleEvent<>(ApiOpResult.success()));
                } else {
                    resendOp.setValue(new SingleEvent<>(
                            ApiOpResult.failure(response, R.string.reset_password_failed)));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                resendBusy = false;
                resendOp.setValue(new SingleEvent<>(
                        ApiOpResult.failure(t, R.string.err_network)));
            }
        });
    }

    private static void cancel(Call<?> call) {
        if (call != null) call.cancel();
    }

    @Override
    protected void onCleared() {
        cancel(verifyCall);
        cancel(resendCall);
        super.onCleared();
    }
}
