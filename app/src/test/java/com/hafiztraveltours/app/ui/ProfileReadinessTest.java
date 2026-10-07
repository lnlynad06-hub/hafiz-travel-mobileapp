package com.hafiztraveltours.app.ui;

import com.hafiztraveltours.app.models.DocumentDto;
import com.hafiztraveltours.app.network.UserDto;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for ProfileViewModel Account Readiness calculation.
 *
 * Rules:
 * 1. Account Readiness percentage is based on ALL required Edit Profile information:
 *    - Personal Information (50%): Full Name (8%), Username (7%), Date of Birth (7%),
 *      Gender (7%), Country (7%), Phone (7%), Email (7%)
 *    - Passport Information (30%): Passport Number (10%), Passport Expiry Date (10%), Issuing Country (10%)
 *    - Emergency Contact (20%): Emergency Name (10%), Emergency Phone (10%)
 *    Total = 50 + 30 + 20 = 100%.
 * 2. Mahram Information is completely optional (0 points) and excluded from the calculation.
 * 3. When all required Edit Profile fields are complete, Account Readiness = 100%,
 *    even when Mahram Information is empty.
 * 4. Documents do not block 100% readiness score.
 */
public class ProfileReadinessTest {

    private Map<String, String> createCompleteRequiredExtras() {
        Map<String, String> extras = new HashMap<>();
        // Personal Information (50%)
        extras.put("name", "Muhammad Hafiz");
        extras.put("nickname", "hafiz");
        extras.put("date_of_birth", "1990-01-01");
        extras.put("gender", "Male");
        extras.put("nationality", "Malaysia");
        extras.put("phone", "+60123456789");
        extras.put("email", "hafiz@example.com");

        // Passport Information (30%)
        extras.put("passport_no", "A12345678");
        extras.put("passport_expiry", "2030-01-01");
        extras.put("issuing_country", "Malaysia");

        // Emergency Contact (20%)
        extras.put("emergency_name", "Fatimah");
        extras.put("emergency_phone", "+60198765432");

        return extras;
    }

    /**
     * Requirement 1: All required Edit Profile fields empty -> Account Readiness below 100% (0%).
     */
    @Test
    public void computeReadiness_empty_returnsZero() {
        ProfileViewModel.Readiness r = ProfileViewModel.computeReadiness(null, null, null);
        assertEquals(0, r.score);
    }

    @Test
    public void computeReadiness_whitespaceOnly_returnsZero() {
        Map<String, String> extras = new HashMap<>();
        extras.put("name", "   ");
        extras.put("nickname", " \t ");
        extras.put("date_of_birth", " ");
        extras.put("gender", " ");
        extras.put("nationality", "  ");
        extras.put("phone", " ");
        extras.put("email", "   ");
        extras.put("passport_no", "  ");
        extras.put("passport_expiry", " ");
        extras.put("issuing_country", " ");
        extras.put("emergency_name", " ");
        extras.put("emergency_phone", " ");

        UserDto user = new UserDto();
        user.name = "   ";
        user.nickname = " ";
        user.dateOfBirth = " ";
        user.gender = " ";
        user.nationality = " ";
        user.phone = " ";
        user.email = " ";
        user.passportNumber = " ";
        user.passportExpiryDate = " ";
        user.issuingCountry = " ";
        user.emergencyName = " ";
        user.emergencyPhone = " ";

        ProfileViewModel.Readiness r = ProfileViewModel.computeReadiness(user, extras, null);
        assertEquals(0, r.score);
    }

    /**
     * Requirement 2: Some required fields missing -> Account Readiness below 100%.
     */
    @Test
    public void computeReadiness_partialFields_calculatesCorrectSum() {
        Map<String, String> extras = new HashMap<>();
        extras.put("name", "Muhammad Hafiz"); // 8
        extras.put("nickname", "hafiz");       // 7

        ProfileViewModel.Readiness r = ProfileViewModel.computeReadiness(null, extras, null);
        assertEquals(15, r.score);
        assertTrue(r.score < 100);
    }

    @Test
    public void computeReadiness_missingOneRequiredField_below100() {
        // Missing email from personal info (score should be 100 - 7 = 93)
        Map<String, String> extras = createCompleteRequiredExtras();
        extras.remove("email");
        ProfileViewModel.Readiness r1 = ProfileViewModel.computeReadiness(null, extras, null);
        assertEquals(93, r1.score);
        assertTrue(r1.score < 100);

        // Missing passport expiry from passport info (score should be 100 - 10 = 90)
        extras = createCompleteRequiredExtras();
        extras.remove("passport_expiry");
        ProfileViewModel.Readiness r2 = ProfileViewModel.computeReadiness(null, extras, null);
        assertEquals(90, r2.score);
        assertTrue(r2.score < 100);

        // Missing emergency phone from emergency info (score should be 100 - 10 = 90)
        extras = createCompleteRequiredExtras();
        extras.remove("emergency_phone");
        ProfileViewModel.Readiness r3 = ProfileViewModel.computeReadiness(null, extras, null);
        assertEquals(90, r3.score);
        assertTrue(r3.score < 100);
    }

    /**
     * Requirement 3: All required fields complete + Mahram empty -> Account Readiness 100%.
     */
    @Test
    public void computeReadiness_allRequiredFieldsComplete_mahramEmpty_returns100Percent() {
        Map<String, String> extras = createCompleteRequiredExtras();
        // Mahram explicitly omitted / empty
        extras.put("mahram_name", "");
        extras.put("mahram_relationship", "");

        ProfileViewModel.Readiness r = ProfileViewModel.computeReadiness(null, extras, null);
        assertEquals(100, r.score);
    }

    /**
     * Requirement 4: All required fields complete + Mahram complete -> Account Readiness 100%.
     */
    @Test
    public void computeReadiness_allRequiredFieldsComplete_mahramComplete_returns100Percent() {
        Map<String, String> extras = createCompleteRequiredExtras();
        // Mahram provided
        extras.put("mahram_name", "Haji Ali");
        extras.put("mahram_relationship", "Bapa");

        ProfileViewModel.Readiness r = ProfileViewModel.computeReadiness(null, extras, null);
        assertEquals(100, r.score);
    }

    /**
     * Verification that Document Uploads do not alter or reduce the 100% readiness score.
     */
    @Test
    public void computeReadiness_documentsDoNotBlock100PercentReadiness() {
        Map<String, String> extras = createCompleteRequiredExtras();

        // With null or empty documents, score is still 100
        ProfileViewModel.Readiness rNoDocs = ProfileViewModel.computeReadiness(null, extras, null);
        assertEquals(100, rNoDocs.score);

        // With documents present, counts are recorded and score remains 100
        Map<String, DocumentDto> docs = new HashMap<>();
        DocumentDto passportDoc = new DocumentDto();
        passportDoc.documentCode = "passport";
        passportDoc.status = "verified";
        docs.put("passport", passportDoc);

        ProfileViewModel.Readiness rWithDocs = ProfileViewModel.computeReadiness(null, extras, docs);
        assertEquals(100, rWithDocs.score);
        assertEquals(1, rWithDocs.verified);
    }

    /**
     * Fallback to UserDto when extras are empty.
     */
    @Test
    public void computeReadiness_fallbackToUserDto_whenExtrasEmpty() {
        UserDto user = new UserDto();
        user.name = "Siti Nurhaliza";
        user.nickname = "ct";
        user.dateOfBirth = "1979-01-11";
        user.gender = "Female";
        user.nationality = "Malaysia";
        user.phone = "+60123456789";
        user.email = "ct@example.com";
        user.passportNumber = "A87654321";
        user.passportExpiryDate = "2029-01-01";
        user.issuingCountry = "Malaysia";
        user.emergencyName = "Datuk K";
        user.emergencyPhone = "+60199998888";

        ProfileViewModel.Readiness r = ProfileViewModel.computeReadiness(user, new HashMap<>(), null);
        assertEquals(100, r.score);
    }
}
