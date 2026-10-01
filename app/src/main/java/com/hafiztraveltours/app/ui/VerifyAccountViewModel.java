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

public class VerifyAccountViewModel extends AndroidViewModel {

    private final MutableLiveData<SingleEvent<ApiOpResult>> verifyOp = new MutableLiveData<>();
    private final MutableLiveData<SingleEvent<ApiOpResult>> resendOp = new MutableLiveData<>();

    private Call<?> verifyCall;
    private Call<?> resendCall;
    private boolean verifyBusy;
    private boolean resendBusy;

    public VerifyAccountViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<SingleEvent<ApiOpResult>> getVerifyOp() {
        return verifyOp;
    }

    public LiveData<SingleEvent<ApiOpResult>> getResendOp() {
        return resendOp;
    }

    private ApiService api() {
        return ApiClient.getApiService();
    }

    public void verifyAccount(String email, String code) {
        if (verifyBusy) return;
        verifyBusy = true;
        cancel(verifyCall);

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("code", code);

        Call<ApiResponse<Object>> call = api().verifyAccount(body);
        verifyCall = call;

        call.enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                verifyBusy = false;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    verifyOp.setValue(new SingleEvent<>(ApiOpResult.success()));
                } else {
                    verifyOp.setValue(new SingleEvent<>(
                            ApiOpResult.failure(response, R.string.err_signup_failed)));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                verifyBusy = false;
                verifyOp.setValue(new SingleEvent<>(
                        ApiOpResult.failure(t, R.string.err_network)));
            }
        });
    }

    public void resendVerification(String email) {
        if (resendBusy) return;
        resendBusy = true;
        cancel(resendCall);

        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        Call<ApiResponse<Object>> call = api().resendVerification(body);
        resendCall = call;

        call.enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                resendBusy = false;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    resendOp.setValue(new SingleEvent<>(ApiOpResult.success()));
                } else {
                    resendOp.setValue(new SingleEvent<>(
                            ApiOpResult.failure(response, R.string.err_signup_failed)));
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
