package com.hafiztraveltours.app.models;

import org.json.JSONException;
import org.json.JSONObject;

public class AppNotification {
    public String id;
    public String titleEn;
    public String titleBm;
    public String messageEn;
    public String messageBm;
    public long timestamp;
    public boolean isRead;
    public String type; // "doc_verified", "prayer_reminder", "system"

    public AppNotification(String id, String titleEn, String titleBm, String messageEn, String messageBm, long timestamp, boolean isRead, String type) {
        this.id = id;
        this.titleEn = titleEn;
        this.titleBm = titleBm;
        this.messageEn = messageEn;
        this.messageBm = messageBm;
        this.timestamp = timestamp;
        this.isRead = isRead;
        this.type = type;
    }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", id);
            obj.put("titleEn", titleEn);
            obj.put("titleBm", titleBm);
            obj.put("messageEn", messageEn);
            obj.put("messageBm", messageBm);
            obj.put("timestamp", timestamp);
            obj.put("isRead", isRead);
            obj.put("type", type);
        } catch (JSONException ignored) {}
        return obj;
    }

    public static AppNotification fromJsonObject(JSONObject obj) {
        if (obj == null) return null;
        return new AppNotification(
                obj.optString("id"),
                obj.optString("titleEn"),
                obj.optString("titleBm"),
                obj.optString("messageEn"),
                obj.optString("messageBm"),
                obj.optLong("timestamp"),
                obj.optBoolean("isRead"),
                obj.optString("type")
        );
    }
}
