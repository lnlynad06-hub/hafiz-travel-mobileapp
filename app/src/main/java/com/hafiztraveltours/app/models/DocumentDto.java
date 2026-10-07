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

    @SerializedName("rejection_reason")
    public String rejectionReason;

    @SerializedName("submitted_at")
    public String submittedAt;

    @SerializedName("verified_at")
    public String verifiedAt;
}
