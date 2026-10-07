package com.hafiztraveltours.app.network;

import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.UmrahPackage;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
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

    @POST("v1/auth/login")
    Call<ApiResponse<AuthResponse>> login(@Body LoginRequest request);

    @POST("v1/auth/google")
    Call<ApiResponse<AuthResponse>> googleLogin(@Body GoogleLoginRequest request);

    @POST("v1/auth/register")
    Call<ApiResponse<AuthResponse>> register(@Body RegisterRequest request);

    @POST("v1/auth/verify-account")
    Call<ApiResponse<Object>> verifyAccount(@Body Map<String, String> body);

    @POST("v1/auth/resend-verification")
    Call<ApiResponse<Object>> resendVerification(@Body Map<String, String> body);

    @POST("v1/auth/forgot-password")
    Call<ApiResponse<Object>> forgotPassword(@Body Map<String, String> body);

    @POST("v1/auth/verify-reset-code")
    Call<ApiResponse<Map<String, Object>>> verifyResetCode(@Body Map<String, String> body);

    @POST("v1/auth/resend-reset-code")
    Call<ApiResponse<Object>> resendResetCode(@Body Map<String, String> body);

    @POST("v1/auth/reset-password")
    Call<ApiResponse<Object>> resetPassword(@Body Map<String, String> body);

    @GET("v1/auth/me")
    Call<ApiResponse<ProfileResponseDto>> getMe();

    @POST("v1/auth/logout")
    Call<ApiResponse<Object>> logout();

    @PUT("v1/profile")
    Call<ApiResponse<ProfileResponseDto>> updateProfile(@Body Map<String, String> body);

    @PUT("v1/profile/change-password")
    Call<ApiResponse<Object>> changePassword(@Body Map<String, String> body);

    @GET("v1/profile/stats")
    Call<ApiResponse<ProfileStatsDto>> getProfileStats();

    @GET("v1/bookings")
    Call<ApiResponse<BookingListPage>> getBookings(
            @Query("status") String status,
            @Query("per_page") Integer perPage
    );

    @GET("v1/bookings/{booking}")
    Call<ApiResponse<BookingDetailDto>> getBookingDetail(
            @Path("booking") int bookingId
    );

    @POST("v1/bookings")
    Call<ApiResponse<BookingDetailDto>> createBooking(
            @Body CreateBookingRequest request
    );

    @GET("v1/user-documents")
    Call<ApiResponse<List<DocumentDto>>> getUserDocuments();

    @Multipart
    @POST("v1/user-documents/upload")
    Call<ApiResponse<DocumentDto>> uploadUserDocument(
            @Part("document_code") RequestBody documentCode,
            @Part MultipartBody.Part file
    );

    @GET("v1/bookings/{booking}/documents")
    Call<BookingDocumentsResponse> getBookingDocuments(
            @Path("booking") int bookingId,
            @Query("traveller_id") Integer travellerId
    );

    @Multipart
    @POST("v1/bookings/{booking}/documents/upload")
    Call<ApiResponse<DocumentDto>> uploadBookingDocument(
            @Path("booking") int bookingId,
            @Part("document_code") RequestBody documentCode,
            @Part("traveller_id") RequestBody travellerId,
            @Part MultipartBody.Part file
    );

    @POST("v1/bookings/{booking}/pay")
    Call<ApiResponse<BookingDetailDto>> payBooking(
            @Path("booking") int bookingId,
            @Body Map<String, Object> body
    );

    @GET("v1/bookings/check-eligibility")
    Call<ApiResponse<BookingEligibilityDto>> checkBookingEligibility();

    @GET("v1/bookings/{booking}/cancellation-quote")
    Call<ApiResponse<CancellationQuoteDto>> getCancellationQuote(
            @Path("booking") int bookingId
    );

    @POST("v1/bookings/{booking}/cancel")
    Call<ApiResponse<BookingDetailDto>> cancelBooking(
            @Path("booking") int bookingId,
            @Body CancelBookingRequest body
    );
}
