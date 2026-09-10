package com.hafiztraveltours.app.network;

import com.hafiztraveltours.app.UmrahPackage;

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
}
