package com.hafiztraveltours.app.network;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.R;


import com.hafiztraveltours.app.models.UmrahPackage;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @GET("v1/home")
    Call<ApiResponse<HomeDataResponse>> getHomeData();

    @GET("v1/packages")
    Call<ApiResponse<List<UmrahPackage>>> getPackages(
            @Query("category") String category,
            @Query("featured") Boolean featured,
            @Query("search") String search,
            @Query("page") Integer page
    );

    @GET("v1/packages/{id}")
    Call<ApiResponse<UmrahPackage>> getPackageDetail(@Path("id") String packageId);

    @retrofit2.http.POST("v1/auth/login")
    Call<ApiResponse<AuthResponse>> login(@retrofit2.http.Body LoginRequest request);

    @retrofit2.http.POST("v1/auth/google")
    Call<ApiResponse<AuthResponse>> googleLogin(@retrofit2.http.Body GoogleLoginRequest request);

    @retrofit2.http.POST("v1/auth/register")
    Call<ApiResponse<AuthResponse>> register(@retrofit2.http.Body RegisterRequest request);

    @retrofit2.http.POST("v1/auth/forgot-password")
    Call<ApiResponse<Object>> forgotPassword(@retrofit2.http.Body java.util.Map<String, String> body);

    @retrofit2.http.GET("v1/auth/me")
    Call<ApiResponse<ProfileResponseDto>> getMe();

    @retrofit2.http.POST("v1/auth/logout")
    Call<ApiResponse<Object>> logout();

    @retrofit2.http.GET("v1/profile")
    Call<ApiResponse<ProfileResponseDto>> getProfile();

    @retrofit2.http.PUT("v1/profile")
    Call<ApiResponse<ProfileResponseDto>> updateProfile(@retrofit2.http.Body java.util.Map<String, String> body);

    @retrofit2.http.GET("v1/profile/stats")
    Call<ApiResponse<com.hafiztraveltours.app.models.ProfileStatsDto>> getProfileStats();

    @retrofit2.http.GET("v1/bookings")
    Call<ApiResponse<com.hafiztraveltours.app.models.BookingListPage>> getBookings(
            @Query("status") String status,
            @Query("per_page") Integer perPage
    );

    @retrofit2.http.GET("v1/bookings/{booking}")
    Call<ApiResponse<com.hafiztraveltours.app.models.BookingDetailDto>> getBookingDetail(
            @Path("booking") int bookingId
    );

    @retrofit2.http.POST("v1/bookings")
    Call<ApiResponse<com.hafiztraveltours.app.models.BookingDetailDto>> createBooking(
            @retrofit2.http.Body com.hafiztraveltours.app.models.CreateBookingRequest request
    );

    @retrofit2.http.GET("v1/user-documents")
    Call<ApiResponse<List<com.hafiztraveltours.app.models.DocumentDto>>> getUserDocuments();

    @retrofit2.http.Multipart
    @retrofit2.http.POST("v1/user-documents/upload")
    Call<ApiResponse<com.hafiztraveltours.app.models.DocumentDto>> uploadUserDocument(
            @retrofit2.http.Part("document_code") okhttp3.RequestBody documentCode,
            @retrofit2.http.Part okhttp3.MultipartBody.Part file
    );

    @retrofit2.http.GET("v1/bookings/{booking}/documents")
    Call<ApiResponse<List<com.hafiztraveltours.app.models.DocumentDto>>> getBookingDocuments(
            @Path("booking") int bookingId
    );

    @retrofit2.http.Multipart
    @retrofit2.http.POST("v1/bookings/{booking}/documents/upload")
    Call<ApiResponse<com.hafiztraveltours.app.models.DocumentDto>> uploadBookingDocument(
            @Path("booking") int bookingId,
            @retrofit2.http.Part("document_code") okhttp3.RequestBody documentCode,
            @retrofit2.http.Part okhttp3.MultipartBody.Part file
    );
}
