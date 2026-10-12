package com.hafiztraveltours.app.models;

import com.google.gson.annotations.SerializedName;

public class DocumentDto {
    @SerializedName("document_code")
    public String documentCode;

    @SerializedName("title")
    public String title;

    @SerializedName("status")
    public String status;

    @SerializedName("is_auto_reused")
    public boolean isAutoReused;

    @SerializedName("file_path")
    public String filePath;

    @SerializedName("file_name")
    public String fileName;

    @SerializedName("document_id")
    public Long documentId;

    @SerializedName("preview_url")
    public String previewUrl;

    @SerializedName("download_url")
    public String downloadUrl;

    @SerializedName("has_file")
    public boolean hasFile;

    @SerializedName("mime_type")
    public String mimeType;

    @SerializedName("rejection_reason")
    public String rejectionReason;

    @SerializedName("submitted_at")
    public String submittedAt;

    @SerializedName("verified_at")
    public String verifiedAt;

    public String getEffectiveUrl() {
        if (previewUrl != null && !previewUrl.trim().isEmpty()) {
            return previewUrl.trim();
        }
        if (downloadUrl != null && !downloadUrl.trim().isEmpty()) {
            return downloadUrl.trim();
        }
        if (filePath != null && !filePath.trim().isEmpty()) {
            return filePath.trim();
        }
        return null;
    }
}
