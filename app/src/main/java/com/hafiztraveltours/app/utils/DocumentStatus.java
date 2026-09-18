package com.hafiztraveltours.app.utils;

import android.content.Context;

import com.hafiztraveltours.app.R;

/**
 * Typed document status (M6). Backend values are matched case-insensitively;
 * unknown/null values map to UNKNOWN (never crash) and render with the
 * not-uploaded treatment. UI labels always come from string resources.
 *
 * <p>Backend vocabulary (unchanged): verified/approved, submitted/pending/under_review,
 * rejected, expired, not_required, not_uploaded (client-side absence marker).
 */
public enum DocumentStatus {

    VERIFIED,
    PENDING,
    REJECTED,
    EXPIRED,
    NOT_REQUIRED,
    NOT_UPLOADED,
    UNKNOWN;

    /** Null/blank-safe parser; never throws, never returns null. */
    public static DocumentStatus from(String raw) {
        if (raw == null) return NOT_UPLOADED;
        String s = raw.trim().toLowerCase();
        if (s.isEmpty() || s.equals("not_uploaded")) return NOT_UPLOADED;
        if (s.equals("verified") || s.equals("approved")) return VERIFIED;
        if (s.equals("submitted") || s.equals("pending") || s.equals("under_review")) return PENDING;
        if (s.equals("rejected")) return REJECTED;
        if (s.equals("expired")) return EXPIRED;
        if (s.equals("not_required")) return NOT_REQUIRED;
        return UNKNOWN;
    }

    public static DocumentStatus from(com.hafiztraveltours.app.models.DocumentDto doc) {
        if (doc == null) return NOT_UPLOADED;
        return from(doc.status);
    }

    /** Counts toward profile readiness: uploaded and not failed (rejected/expired must be fixed). */
    public boolean countsAsUploaded() {
        switch (this) {
            case VERIFIED:
            case PENDING:
            case NOT_REQUIRED:
                return true;
            default:
                return false;
        }
    }

    /** Localized status label. */
    public int labelRes() {
        switch (this) {
            case VERIFIED:
                return R.string.doc_status_verified;
            case PENDING:
                return R.string.doc_status_under_review;
            case REJECTED:
                return R.string.doc_status_rejected;
            case EXPIRED:
                return R.string.doc_status_expired;
            case NOT_REQUIRED:
                return R.string.doc_status_not_required;
            case NOT_UPLOADED:
            case UNKNOWN:
            default:
                return R.string.doc_status_not_uploaded;
        }
    }

    public String label(Context context) {
        if (context == null) return "";
        return context.getString(labelRes());
    }
}
