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
            obj.put("id", pkg.id);
            obj.put("name", pkg.name);
            obj.put("durationDays", pkg.durationDays);
            obj.put("nightsCount", pkg.nightsCount);
            obj.put("price", pkg.price);
            obj.put("url", pkg.url);
            obj.put("imageUrl", pkg.imageUrl);
            return obj.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    private static UmrahPackage fromJson(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            return new UmrahPackage(
                    obj.getString("id"), obj.getString("name"),
                    obj.getInt("durationDays"), obj.getInt("nightsCount"),
                    obj.getString("price"), obj.getString("url"), obj.getString("imageUrl"));
        } catch (Exception e) {
            return null;
        }
    }

    public interface OnFavoriteChangeListener {
        void onChanged(boolean isFavoriteNow);
    }

    /**
     * Bungkus toggleFavorite() dengan UX yang betul:
     * - ADD (belum favorite) -> terus tambah + Toast "Ditambah ke Favorite"
     * - REMOVE (dah favorite) -> confirm dialog dulu, cuma buang kalau user tekan "Buang"
     */
    public static void handleFavoriteToggle(Context context, UmrahPackage pkg, OnFavoriteChangeListener listener) {
        boolean isFav = isFavorite(context, pkg.id);

        if (isFav) {
            new android.app.AlertDialog.Builder(context)
                    .setTitle("Buang dari Favorite?")
                    .setMessage("Pakej \"" + pkg.name + "\" akan dibuang daripada senarai Favorite anda.")
                    .setPositiveButton("Buang", (dialog, which) -> {
                        toggleFavorite(context, pkg);
                        android.widget.Toast.makeText(context, "Dibuang dari Favorite", android.widget.Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onChanged(false);
                    })
                    .setNegativeButton("Batal", null)
                    .show();
        } else {
            toggleFavorite(context, pkg);
            android.widget.Toast.makeText(context, "Ditambah ke Favorite", android.widget.Toast.LENGTH_SHORT).show();
            if (listener != null) listener.onChanged(true);
        }
    }
}