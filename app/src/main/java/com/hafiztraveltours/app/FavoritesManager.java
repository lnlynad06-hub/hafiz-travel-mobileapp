package com.hafiztraveltours.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Favorites disimpan sepenuhnya dalam telefon (SharedPreferences) - guest pun
 * boleh guna tanpa log masuk, sama pattern macam checklist_state_umrah/tour
 * dalam MainActivity. Setiap pakej yang di-favorite-kan disimpan penuh
 * sebagai JSON supaya FavoriteActivity boleh papar semula tanpa perlu
 * query Firestore lagi.
 */
public class FavoritesManager {

    private static final String PREFS_NAME = "favorites";
    private static final String KEY_ID_SET = "favorite_ids";

    public static boolean isFavorite(Context context, String packageId) {
        Set<String> ids = getPrefs(context).getStringSet(KEY_ID_SET, new HashSet<>());
        return ids.contains(packageId);
    }

    public static int getFavoriteCount(Context context) {
        Set<String> ids = getPrefs(context).getStringSet(KEY_ID_SET, new HashSet<>());
        return ids.size();
    }

    /** Toggle status; return the NEW favorite state (true = now favorited). */
    public static boolean toggleFavorite(Context context, UmrahPackage pkg) {
        SharedPreferences prefs = getPrefs(context);
        Set<String> ids = new HashSet<>(prefs.getStringSet(KEY_ID_SET, new HashSet<>()));
        SharedPreferences.Editor editor = prefs.edit();

        boolean nowFavorited;
        if (ids.contains(pkg.id)) {
            ids.remove(pkg.id);
            editor.remove("favorite_data_" + pkg.id);
            nowFavorited = false;
        } else {
            ids.add(pkg.id);
            editor.putString("favorite_data_" + pkg.id, toJson(pkg));
            nowFavorited = true;
        }
        editor.putStringSet(KEY_ID_SET, ids);
        editor.apply();
        return nowFavorited;
    }

    public static List<UmrahPackage> getAllFavorites(Context context) {
        SharedPreferences prefs = getPrefs(context);
        Set<String> ids = prefs.getStringSet(KEY_ID_SET, new HashSet<>());
        List<UmrahPackage> result = new ArrayList<>();
        for (String id : ids) {
            String json = prefs.getString("favorite_data_" + id, null);
            if (json != null) {
                UmrahPackage pkg = fromJson(json);
                if (pkg != null) result.add(pkg);
            }
        }
        return result;
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String toJson(UmrahPackage pkg) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", pkg.id != null ? pkg.id : "");
            obj.put("name", pkg.name != null ? pkg.name : "");
            obj.put("durationDays", pkg.durationDays);
            obj.put("nightsCount", pkg.nightsCount);
            obj.put("price", pkg.price != null ? pkg.price : "");
            obj.put("url", pkg.url != null ? pkg.url : "");
            obj.put("imageUrl", pkg.imageUrl != null ? pkg.imageUrl : "");
            obj.put("collectionName", pkg.collectionName != null ? pkg.collectionName : "umrah_packages");
            return obj.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    private static UmrahPackage fromJson(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            UmrahPackage pkg = new UmrahPackage(
                    obj.optString("id", ""),
                    obj.optString("name", ""),
                    obj.optInt("durationDays", 0),
                    obj.optInt("nightsCount", 0),
                    obj.optString("price", ""),
                    obj.optString("url", ""),
                    obj.optString("imageUrl", "")
            );
            pkg.collectionName = obj.optString("collectionName", "umrah_packages");
            return pkg;
        } catch (Exception e) {
            return null;
        }
    }

    public interface OnFavoriteChangeListener {
        void onChanged(boolean isFavoriteNow);
    }

    /**
     * Animate heart icon with a tactile bounce / spring effect.
     */
    public static void animateHeart(android.view.View heartIcon, boolean isNowFavorited) {
        if (heartIcon == null) return;
        heartIcon.animate().cancel();
        if (isNowFavorited) {
            heartIcon.setScaleX(0.7f);
            heartIcon.setScaleY(0.7f);
            heartIcon.animate()
                    .scaleX(1.35f)
                    .scaleY(1.35f)
                    .setDuration(160)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(2.5f))
                    .withEndAction(() -> {
                        heartIcon.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(120)
                                .start();
                    })
                    .start();
        } else {
            heartIcon.animate()
                    .scaleX(0.7f)
                    .scaleY(0.7f)
                    .setDuration(120)
                    .withEndAction(() -> {
                        heartIcon.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(100)
                                .start();
                    })
                    .start();
        }
    }

    /**
     * Instant smooth favorite toggle with tactile feedback.
     */
    public static void handleFavoriteToggle(Context context, UmrahPackage pkg, OnFavoriteChangeListener listener) {
        handleFavoriteToggle(context, pkg, null, listener);
    }

    public static void handleFavoriteToggle(Context context, UmrahPackage pkg, android.view.View heartIcon, OnFavoriteChangeListener listener) {
        boolean newFavState = toggleFavorite(context, pkg);
        if (heartIcon != null) {
            animateHeart(heartIcon, newFavState);
        }
        if (newFavState) {
            android.widget.Toast.makeText(context, context.getString(R.string.added_to_favorites), android.widget.Toast.LENGTH_SHORT).show();
        }
        if (listener != null) {
            listener.onChanged(newFavState);
        }
    }
}