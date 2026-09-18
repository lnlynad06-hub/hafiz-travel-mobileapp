package com.hafiztraveltours.app.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.hafiztraveltours.app.data.repository.MainRepository;
import com.hafiztraveltours.app.models.UmrahPackage;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.network.HomeDataResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Home screen state + coordination (H1/Phase 9). Holds no Views and no Activity
 * references — only the Application context owned by the Repository. Survives
 * configuration changes; cancels the in-flight home call in {@link #onCleared()}.
 *
 * <p>Scope note: the prayer-times subsystem (JAKIM HTTP, location, handlers) stays
 * in the Activity — it is device-sensor + handler-driven UI state, not Laravel
 * API coordination. Home failures stay silent (no error UI exists on this screen).
 */
public class MainViewModel extends AndroidViewModel {

    /** Greeting inputs (raw; Activity applies localized defaults/texts). */
    public static final class SessionInfo {
        public final boolean loggedIn;
        public final String nickname;

        public SessionInfo(boolean loggedIn, String nickname) {
            this.loggedIn = loggedIn;
            this.nickname = nickname;
        }
    }

    /** Mapped home payload: popular list + hero showcase list. Null = load failed. */
    public static final class HomeContent {
        public final List<UmrahPackage> popular;
        public final List<UmrahPackage> showcase;

        public HomeContent(List<UmrahPackage> popular, List<UmrahPackage> showcase) {
            this.popular = popular;
            this.showcase = showcase;
        }
    }

    private final MainRepository repository;

    private final MutableLiveData<SessionInfo> sessionInfo = new MutableLiveData<>();
    private final MutableLiveData<HomeContent> homeContent = new MutableLiveData<>();
    private final MutableLiveData<Boolean> homeLoading = new MutableLiveData<>(false);

    private Call<?> homeCall;

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = new MainRepository(application);
    }

    public LiveData<SessionInfo> getSessionInfo() {
        return sessionInfo;
    }

    public LiveData<HomeContent> getHomeContent() {
        return homeContent;
    }

    public LiveData<Boolean> getHomeLoading() {
        return homeLoading;
    }

    public boolean isLoggedIn() {
        return repository.isLoggedIn();
    }

    public boolean hasUnreadNotifications() {
        return repository.hasUnreadNotifications();
    }

    public void markNotificationsRead() {
        repository.markNotificationsRead();
    }

    /** Refreshes greeting inputs from the session (same reads as before). */
    public void refreshSession() {
        sessionInfo.setValue(new SessionInfo(
                repository.isLoggedIn(), repository.getUserNickname()));
    }

    /** Home data load (cancels any previous identical request). */
    public void loadHome() {
        cancel(homeCall);
        homeLoading.setValue(true);
        Call<ApiResponse<HomeDataResponse>> call = repository.homeCall();
        homeCall = call;
        call.enqueue(new Callback<ApiResponse<HomeDataResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<HomeDataResponse>> call,
                                   Response<ApiResponse<HomeDataResponse>> response) {
                homeLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().data != null) {
                    homeContent.setValue(mapHome(response.body().data));
                }
                // Else (unsuccessful payload): keep stale lists/hero untouched (as before);
                // the loading observer still stops the shimmer.
            }

            @Override
            public void onFailure(Call<ApiResponse<HomeDataResponse>> call, Throwable t) {
                homeLoading.setValue(false);
                homeContent.setValue(null);
            }
        });
    }

    /** Pure mapping: featured/popular lists + collection tagging (same rules as before). */
    static HomeContent mapHome(HomeDataResponse homeData) {
        List<UmrahPackage> popular = new ArrayList<>();
        List<UmrahPackage> showcase = new ArrayList<>();
        if (homeData == null) return new HomeContent(popular, showcase);
        if (homeData.featured != null && !homeData.featured.isEmpty()) {
            for (UmrahPackage p : homeData.featured) {
                if (p == null) continue;
                if (p.collectionName == null || p.collectionName.trim().isEmpty()) {
                    p.collectionName = p.isUmrah() ? "umrah_packages" : "tour_packages";
                }
                popular.add(p);
                showcase.add(p);
            }
        }
        if (homeData.popularUmrah != null) {
            for (UmrahPackage p : homeData.popularUmrah) {
                if (p == null) continue;
                p.collectionName = "umrah_packages";
                popular.add(p);
            }
        }
        if (homeData.popularTour != null) {
            for (UmrahPackage p : homeData.popularTour) {
                if (p == null) continue;
                p.collectionName = "tour_packages";
                popular.add(p);
            }
        }
        return new HomeContent(popular, showcase);
    }

    private static void cancel(Call<?> call) {
        if (call != null) call.cancel();
    }

    @Override
    protected void onCleared() {
        cancel(homeCall);
        super.onCleared();
    }
}
