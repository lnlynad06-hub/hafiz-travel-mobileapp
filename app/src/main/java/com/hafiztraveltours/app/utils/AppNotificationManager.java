package com.hafiztraveltours.app.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.AppNotification;
import com.hafiztraveltours.app.ui.MainActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AppNotificationManager {

    private static final String PREF_NAME = "app_notifications_store";
    private static final String KEY_NOTIFS = "notifications_json";
    private static final String CHANNEL_ID = "hafiz_travel_notifications";
    private static final String CHANNEL_NAME = "Hafiz Travel Notifications";

    public static synchronized List<AppNotification> getNotifications(Context context) {
        List<AppNotification> list = new ArrayList<>();
        if (context == null) return list;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonStr = prefs.getString(KEY_NOTIFS, "[]");
        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                AppNotification notif = AppNotification.fromJsonObject(arr.getJSONObject(i));
                if (notif != null) list.add(notif);
            }
        } catch (Exception ignored) {}
        return list;
    }

    public static synchronized boolean hasUnread(Context context) {
        List<AppNotification> list = getNotifications(context);
        for (AppNotification n : list) {
            if (!n.isRead) return true;
        }
        return false;
    }

    public static synchronized void markAllRead(Context context) {
        List<AppNotification> list = getNotifications(context);
        for (AppNotification n : list) {
            n.isRead = true;
        }
        saveNotifications(context, list);
        if (context != null) {
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("has_unread_notifications", false).apply();
        }
    }

    public static synchronized void markRead(Context context, String notifId) {
        if (notifId == null) return;
        List<AppNotification> list = getNotifications(context);
        for (AppNotification n : list) {
            if (notifId.equals(n.id)) {
                n.isRead = true;
                break;
            }
        }
        saveNotifications(context, list);
    }

    private static void saveNotifications(Context context, List<AppNotification> list) {
        if (context == null) return;
        JSONArray arr = new JSONArray();
        for (AppNotification n : list) {
            arr.put(n.toJsonObject());
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_NOTIFS, arr.toString()).apply();
    }

    public static synchronized void postNotification(Context context, AppNotification notif) {
        if (context == null || notif == null) return;

        // 1. Save to in-app notification center store
        List<AppNotification> list = getNotifications(context);
        for (AppNotification existing : list) {
            if (existing.id != null && existing.id.equals(notif.id)) {
                return; // Prevent duplicate notifications
            }
        }
        list.add(0, notif);
        saveNotifications(context, list);

        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("has_unread_notifications", true).apply();

        // 2. Post system notification if enabled
        if (!OnboardingManager.isNotificationFeatureEnabled(context)) {
            return;
        }

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("System & Travel notifications");
            nm.createNotificationChannel(channel);
        }

        String currentLang = LocaleHelper.getSavedLanguage(context);
        boolean isMalay = LocaleHelper.LANGUAGE_MALAY.equalsIgnoreCase(currentLang);
        String displayTitle = isMalay ? notif.titleBm : notif.titleEn;
        String displayMsg = isMalay ? notif.messageBm : notif.messageEn;

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, Math.abs(notif.id.hashCode()), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(displayTitle)
                .setContentText(displayMsg)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(displayMsg))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);

        nm.notify(Math.abs(notif.id.hashCode()), builder.build());
    }

    public static void checkAndNotifyDocumentVerification(Context context, List<com.hafiztraveltours.app.models.DocumentDto> docs) {
        if (context == null || docs == null) return;
        for (com.hafiztraveltours.app.models.DocumentDto doc : docs) {
            if (doc != null && doc.status != null && "verified".equalsIgnoreCase(doc.status)) {
                String docId = doc.documentCode != null ? doc.documentCode : (doc.title != null ? doc.title : "default");
                String notifId = "doc_verified_" + docId;
                AppNotification notif = new AppNotification(
                        notifId,
                        "Travel Document Verified",
                        "Dokumen Perjalanan Disahkan",
                        "Your travel document has been verified successfully.",
                        "Dokumen perjalanan anda telah berjaya disahkan.",
                        System.currentTimeMillis(),
                        false,
                        "doc_verified"
                );
                postNotification(context, notif);
            }
        }
    }
}
