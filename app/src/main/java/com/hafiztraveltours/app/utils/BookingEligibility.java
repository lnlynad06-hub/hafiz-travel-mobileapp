package com.hafiztraveltours.app.utils;

import android.content.Context;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.DocumentDto;
import com.hafiztraveltours.app.network.UserDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Customer booking eligibility gatekeeper.
 *
 * <p>Requirements:
 * 1. Customer profile must be 100% complete (all required fields valid and present).
 * 2. Required travel documents (passport, IC, passport photo) must be uploaded and valid/pending.
 *
 * If either is incomplete, package registration/booking is blocked with direct resolution actions.
 */
public final class BookingEligibility {

    public enum Reason {
        NOT_LOGGED_IN,
        PROFILE_INCOMPLETE,
        DOCUMENTS_INCOMPLETE,
        ELIGIBLE
    }

    public static final class Status {
        public final Reason reason;
        public final boolean isProfileComplete;
        public final boolean areDocsComplete;
        public final List<String> missingProfileFields;
        public final List<String> missingDocCodes;

        public Status(Reason reason, boolean isProfileComplete, boolean areDocsComplete,
                      List<String> missingProfileFields, List<String> missingDocCodes) {
            this.reason = reason;
            this.isProfileComplete = isProfileComplete;
            this.areDocsComplete = areDocsComplete;
            this.missingProfileFields = missingProfileFields != null ? missingProfileFields : new ArrayList<>();
            this.missingDocCodes = missingDocCodes != null ? missingDocCodes : new ArrayList<>();
        }

        public boolean isEligible() {
            return reason == Reason.ELIGIBLE;
        }
    }

    private BookingEligibility() {}

    public static Status check(boolean loggedIn, UserDto user, Map<String, String> extras, List<DocumentDto> docs) {
        if (!loggedIn) {
            return new Status(Reason.NOT_LOGGED_IN, false, false, null, null);
        }

        List<String> missingProfile = checkMissingProfileFields(user, extras);
        boolean profileComplete = missingProfile.isEmpty();

        List<String> missingDocs = checkMissingDocuments(docs);
        boolean docsComplete = missingDocs.isEmpty();

        // Profile completion and travel documents are not booking blockers.
        return new Status(Reason.ELIGIBLE, profileComplete, docsComplete, missingProfile, missingDocs);
    }

    public static List<String> checkMissingProfileFields(UserDto user, Map<String, String> ex) {
        List<String> missing = new ArrayList<>();

        String fullName = str(ex, "name");
        if (fullName.isEmpty() && user != null && user.name != null) fullName = user.name.trim();
        if (fullName.isEmpty()) missing.add("name");

        String icNo = str(ex, "ic_no");
        if (icNo.isEmpty() && user != null && user.icNumber != null) icNo = user.icNumber.trim();
        if (icNo.isEmpty()) missing.add("ic_no");

        String passportNo = str(ex, "passport_no");
        if (passportNo.isEmpty() && user != null && user.passportNumber != null) {
            passportNo = user.passportNumber.trim();
        }
        if (passportNo.isEmpty()) missing.add("passport_no");

        String address = str(ex, "address");
        if (address.isEmpty() && user != null && user.address != null) address = user.address.trim();
        if (address.isEmpty()) address = str(ex, "address_line_1");
        if (address.isEmpty() && user != null && user.addressLine1 != null) address = user.addressLine1.trim();
        if (address.isEmpty()) missing.add("address");

        String emergName = str(ex, "emergency_name");
        if (emergName.isEmpty() && user != null && user.emergencyName != null) {
            emergName = user.emergencyName.trim();
        }
        if (emergName.isEmpty()) missing.add("emergency_name");

        return missing;
    }

    public static List<String> checkMissingDocuments(List<DocumentDto> docs) {
        List<String> missing = new ArrayList<>();
        String[] requiredCodes = {"passport", "ic", "passport_photo"};

        Map<String, DocumentDto> byCode = new HashMap<>();
        if (docs != null) {
            for (DocumentDto d : docs) {
                if (d != null && d.documentCode != null) {
                    byCode.put(d.documentCode.toLowerCase().trim(), d);
                }
            }
        }

        for (String code : requiredCodes) {
            DocumentDto doc = byCode.get(code);
            DocumentStatus status = DocumentStatus.from(doc);
            if (!status.countsAsUploaded()) {
                missing.add(code);
            }
        }
        return missing;
    }

    public static String formatMissingDocs(Context context, List<String> codes) {
        if (codes == null || codes.isEmpty() || context == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String code : codes) {
            String name;
            if ("passport".equalsIgnoreCase(code)) {
                name = context.getString(R.string.doc_name_passport);
            } else if ("ic".equalsIgnoreCase(code)) {
                name = context.getString(R.string.doc_name_ic);
            } else if ("passport_photo".equalsIgnoreCase(code)) {
                name = context.getString(R.string.doc_name_passport_photo);
            } else {
                name = code;
            }
            sb.append("• ").append(name).append("\n");
        }
        return sb.toString().trim();
    }

    private static String str(Map<String, String> ex, String key) {
        if (ex == null) return "";
        String v = ex.get(key);
        return v != null ? v.trim() : "";
    }
}
