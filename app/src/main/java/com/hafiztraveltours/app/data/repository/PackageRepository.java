package com.hafiztraveltours.app.data.repository;

import com.hafiztraveltours.app.models.PackageDetail;
import com.hafiztraveltours.app.models.UmrahPackage;
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.network.ApiService;

import java.util.List;

import retrofit2.Call;

/**
 * Package data access (H1/Step 2). Owns package Retrofit calls plus the
 * UmrahPackage → PackageDetail mapping (C1 departure IDs and H4 requirement
 * fields are preserved by {@code PackageDetail.fromUmrahPackage} — see its docs).
 * No Views, no Toasts, no Activity handling; callers track/cancel the Calls.
 */
public class PackageRepository {

    public PackageRepository() {
    }

    private ApiService api() {
        return ApiClient.getApiService();
    }

    /** Fresh detail Call per invocation; caller owns cancellation. */
    public Call<ApiResponse<UmrahPackage>> detailCall(String packageId) {
        return api().getPackageDetail(packageId);
    }

    /** Fresh related-packages Call per invocation; caller owns cancellation. */
    public Call<ApiResponse<List<UmrahPackage>>> relatedCall(String category) {
        return api().getPackages(category, null, null, 1);
    }

    /**
     * Maps the API DTO to the booking UI model, tagging the source collection.
     * Pure data mapping — no Android dependencies.
     */
    public PackageDetail mapDetail(UmrahPackage raw, String collection) {
        if (raw != null) {
            raw.collectionName = collection;
        }
        return PackageDetail.fromUmrahPackage(raw);
    }
}
