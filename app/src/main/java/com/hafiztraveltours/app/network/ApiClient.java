package com.hafiztraveltours.app.network;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.R;


import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    /**
     * Base URL konfigurasi:
     * - Untuk Telefon Sebenar (USB ADB Reverse): "http://127.0.0.1:8000/api/"
     * - Untuk Telefon Sebenar (Wi-Fi sama): "http://192.168.50.127:8000/api/"
     * - Untuk Android Studio Emulator: "http://10.0.2.2:8000/api/"
     */
    public static String BASE_URL = "http://127.0.0.1:8000/api/";

    private static Retrofit retrofit = null;
    private static ApiService apiService = null;
    private static volatile String authToken = null;

    public static synchronized void setAuthToken(String token) {
        authToken = (token != null && !token.isEmpty()) ? token : null;
    }

    public static synchronized ApiService getApiService() {
        if (apiService == null) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            // PII-safe default: no BODY logging (would leak Bearer token + passwords).
            // No BuildConfig in this module, so keep logging off unconditionally.
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .addInterceptor(chain -> {
                        okhttp3.Request original = chain.request();
                        okhttp3.Request.Builder builder = original.newBuilder()
                                .header("Accept", "application/json");
                        if (authToken != null) {
                            builder.header("Authorization", "Bearer " + authToken);
                        }
                        return chain.proceed(builder.build());
                    })
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }

    /**
     * Tukar Base URL semasa runtime jika menguji di peranti fizikal
     */
    public static synchronized void setBaseUrl(String newBaseUrl) {
        if (newBaseUrl != null && !newBaseUrl.endsWith("/")) {
            newBaseUrl = newBaseUrl + "/";
        }
        BASE_URL = newBaseUrl;
        retrofit = null;
        apiService = null;
    }
}
