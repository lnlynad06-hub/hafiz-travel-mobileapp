package com.hafiztraveltours.app.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.data.repository.PackageRepository;
import com.hafiztraveltours.app.models.PackageDetail;
import com.hafiztraveltours.app.models.UmrahPackage;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.utils.ApiOpResult;
import com.hafiztraveltours.app.utils.SingleEvent;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Package detail screen state (H1/Step 3). No Views, no Activity/Context, no Toasts,
 * no navigation. Owns both Retrofit Calls (single-flight + {@link #onCleared()}).
 */
public class PackageDetailViewModel extends ViewModel {

    /** Detail payload: raw API DTO (flight/route display) + mapped UI model. */
    public static final class LoadedPackage {
        public final UmrahPackage raw;
        public final PackageDetail detail;
        public final String collection;

        public LoadedPackage(UmrahPackage raw, PackageDetail detail, String collection) {
            this.raw = raw;
            this.detail = detail;
            this.collection = collection;
        }
    }

    private final PackageRepository repository = new PackageRepository();

    private final MutableLiveData<LoadedPackage> detailData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> detailLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<UmrahPackage>> relatedData = new MutableLiveData<>();
    private final MutableLiveData<SingleEvent<ApiOpResult>> detailError = new MutableLiveData<>();

    private Call<?> detailCall;
    private Call<?> relatedCall;

    public LiveData<LoadedPackage> getDetailData() {
        return detailData;
    }

    public LiveData<Boolean> getDetailLoading() {
        return detailLoading;
    }

    public LiveData<List<UmrahPackage>> getRelatedData() {
        return relatedData;
    }

    public LiveData<SingleEvent<ApiOpResult>> getDetailError() {
        return detailError;
    }

    /** Loads package detail (cancels any previous identical request). */
    public void loadPackage(String collection, String packageId) {
        cancel(detailCall);
        detailLoading.setValue(true);
        Call<ApiResponse<UmrahPackage>> call = repository.detailCall(packageId);
        detailCall = call;
        call.enqueue(new Callback<ApiResponse<UmrahPackage>>() {
            @Override
            public void onResponse(Call<ApiResponse<UmrahPackage>> call,
                                   Response<ApiResponse<UmrahPackage>> response) {
                detailLoading.setValue(false);
                UmrahPackage raw =
                        (response.isSuccessful() && response.body() != null
                                && response.body().data != null) ? response.body().data : null;
                if (raw != null) {
                    detailData.setValue(new LoadedPackage(
                            raw, repository.mapDetail(raw, collection), collection));
                } else {
                    detailError.setValue(new SingleEvent<>(
                            ApiOpResult.failure(response, R.string.err_package_not_found)));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UmrahPackage>> call, Throwable t) {
                if (call.isCanceled()) {
                    return;
                }
                detailLoading.setValue(false);
                detailError.setValue(new SingleEvent<>(
                        ApiOpResult.failure(t, R.string.err_package_load_failed)));
            }
        });
    }

    /**
     * Loads related packages excluding {@code excludeId} (cancels any previous
     * identical request). Failures stay silent, as before.
     */
    public void loadRelated(String category, String excludeId) {
        if (category == null || category.trim().isEmpty()) return;
        cancel(relatedCall);
        Call<ApiResponse<List<UmrahPackage>>> call =
                repository.relatedCall(category.trim());
        relatedCall = call;
        call.enqueue(new Callback<ApiResponse<List<UmrahPackage>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<UmrahPackage>>> call,
                                   Response<ApiResponse<List<UmrahPackage>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    List<UmrahPackage> filtered = new ArrayList<>();
                    for (UmrahPackage p : response.body().data) {
                        if (p != null && p.id != null && !p.id.equals(excludeId)) {
                            filtered.add(p);
                        }
                    }
                    relatedData.setValue(filtered);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<UmrahPackage>>> call, Throwable t) {
                // Ignore failure gracefully (same as before).
            }
        });
    }

    private static void cancel(Call<?> call) {
        if (call != null) call.cancel();
    }

    @Override
    protected void onCleared() {
        cancel(detailCall);
        cancel(relatedCall);
        super.onCleared();
    }
}
