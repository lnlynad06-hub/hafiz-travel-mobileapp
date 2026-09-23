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

        if (!profileComplete) {
            return new Status(Reason.PROFILE_INCOMPLETE, false, docsComplete, missingProfile, missingDocs);
        }

        // Required Edit Profile complete: eligible to book even if travel documents are incomplete.
        return new Status(Reason.ELIGIBLE, true, docsComplete, missingProfile, missingDocs);
    }

    public static List<String> checkMissingProfileFields(UserDto user, Map<String, String> ex) {
        List<String> missing = new ArrayList<>();

        // 1. Personal Information
        String fullName = str(ex, "name");
        if (fullName.isEmpty() && user != null && user.name != null) fullName = user.name.trim();
        if (fullName.isEmpty()) missing.add("name");

        String username = str(ex, "nickname");
        if (username.isEmpty()) username = str(ex, "username");
        if (username.isEmpty() && user != null && user.nickname != null) username = user.nickname.trim();
        if (username.isEmpty()) missing.add("nickname");

        String dob = str(ex, "date_of_birth");
        if (dob.isEmpty()) dob = str(ex, "dob");
        if (dob.isEmpty() && user != null && user.dateOfBirth != null) dob = user.dateOfBirth.trim();
        if (dob.isEmpty()) missing.add("date_of_birth");

        String gender = str(ex, "gender");
        if (gender.isEmpty() && user != null && user.gender != null) gender = user.gender.trim();
        if (gender.isEmpty()) missing.add("gender");

        String country = str(ex, "nationality");
        if (country.isEmpty()) country = str(ex, "country");
        if (country.isEmpty() && user != null && user.nationality != null) country = user.nationality.trim();
        if (country.isEmpty() && user != null && user.country != null) country = user.country.trim();
        if (country.isEmpty()) missing.add("nationality");

        String phone = str(ex, "phone");
        if (phone.isEmpty() && user != null && user.phone != null) phone = user.phone.trim();
        if (phone.isEmpty()) missing.add("phone");

        String email = str(ex, "email");
        if (email.isEmpty() && user != null && user.email != null) email = user.email.trim();
        if (email.isEmpty()) missing.add("email");

        // 2. Passport Information
        String passportNo = str(ex, "passport_no");
        if (passportNo.isEmpty()) passportNo = str(ex, "passport_number");
        if (passportNo.isEmpty() && user != null && user.passportNumber != null) {
            passportNo = user.passportNumber.trim();
        }
        if (passportNo.isEmpty()) missing.add("passport_no");

        String passportExpiry = str(ex, "passport_expiry");
        if (passportExpiry.isEmpty()) passportExpiry = str(ex, "passport_expiry_date");
        if (passportExpiry.isEmpty() && user != null && user.passportExpiryDate != null) {
            passportExpiry = user.passportExpiryDate.trim();
        }
        if (passportExpiry.isEmpty()) missing.add("passport_expiry");

        String issuingCountry = str(ex, "issuing_country");
        if (issuingCountry.isEmpty()) issuingCountry = str(ex, "passport_issuing_country");
        if (issuingCountry.isEmpty() && user != null && user.issuingCountry != null) {
            issuingCountry = user.issuingCountry.trim();
        }
        if (issuingCountry.isEmpty()) missing.add("issuing_country");

        // 3. Emergency Contact
        String emergName = str(ex, "emergency_name");
        if (emergName.isEmpty() && user != null && user.emergencyName != null) {
            emergName = user.emergencyName.trim();
        }
        if (emergName.isEmpty()) missing.add("emergency_name");

        String emergPhone = str(ex, "emergency_phone");
        if (emergPhone.isEmpty() && user != null && user.emergencyPhone != null) {
            emergPhone = user.emergencyPhone.trim();
        }
        if (emergPhone.isEmpty()) missing.add("emergency_phone");

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
