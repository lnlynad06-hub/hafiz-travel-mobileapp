package com.hafiztraveltours.app.utils;

import com.hafiztraveltours.app.models.DocumentDto;
import com.hafiztraveltours.app.network.UserDto;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for BookingEligibility according to the business requirement:
 * "A customer MUST have their Edit Profile completed before they can book.
 * However, incomplete Travel Documents must NOT prevent booking."
 *
 * Testing Matrix:
 * 1. Profile incomplete + documents incomplete -> Cannot book.
 * 2. Profile incomplete + documents complete -> Cannot book.
 * 3. Profile complete + documents incomplete -> CAN book.
 * 4. Profile complete + documents complete -> CAN book.
 * 5. Profile complete + Mahram empty -> CAN book.
 * 6. Profile complete + Travel Visa unavailable -> CAN book.
 * 7. Profile complete + expired passport -> CAN book.
 * 8. Profile complete + passport < 6 months validity -> CAN book.
 */
public class BookingEligibilityTest {

    private Map<String, String> completeExtras;
    private List<DocumentDto> completeDocs;

    @Before
    public void setUp() {
        completeExtras = new HashMap<>();
        // 1. Personal Information
        completeExtras.put("name", "Muhammad Hafiz");
        completeExtras.put("nickname", "hafiz");
        completeExtras.put("date_of_birth", "1990-01-01");
        completeExtras.put("gender", "Male");
        completeExtras.put("nationality", "Malaysia");
        completeExtras.put("phone", "+60123456789");
        completeExtras.put("email", "hafiz@example.com");

        // 2. Passport Information
        completeExtras.put("passport_no", "A99887766");
        completeExtras.put("passport_expiry", "2030-01-01");
        completeExtras.put("issuing_country", "Malaysia");

        // 3. Emergency Contact
        completeExtras.put("emergency_name", "Aishah");
        completeExtras.put("emergency_phone", "+60198765432");

        completeDocs = new ArrayList<>();

        DocumentDto passport = new DocumentDto();
        passport.documentCode = "passport";
        passport.status = "verified";
        completeDocs.add(passport);

        DocumentDto ic = new DocumentDto();
        ic.documentCode = "ic";
        ic.status = "pending";
        completeDocs.add(ic);

        DocumentDto photo = new DocumentDto();
        photo.documentCode = "passport_photo";
        photo.status = "verified";
        completeDocs.add(photo);
    }

    @Test
    public void testCase0_notLoggedIn_blocksBooking() {
        BookingEligibility.Status status = BookingEligibility.check(false, null, completeExtras, completeDocs);
        assertFalse(status.isEligible());
        assertEquals(BookingEligibility.Reason.NOT_LOGGED_IN, status.reason);
    }

    /**
     * Matrix Case 1: Profile incomplete + documents incomplete -> Cannot book.
     */
    @Test
    public void testCase1_profileIncomplete_and_docsIncomplete_blocksBooking() {
        Map<String, String> incompleteExtras = new HashMap<>();
        incompleteExtras.put("name", "Muhammad Hafiz"); // missing nickname, passport, emergency, etc.

        List<DocumentDto> emptyDocs = new ArrayList<>();

        BookingEligibility.Status status = BookingEligibility.check(true, null, incompleteExtras, emptyDocs);
        assertFalse(status.isEligible());
        assertEquals(BookingEligibility.Reason.PROFILE_INCOMPLETE, status.reason);
        assertFalse(status.isProfileComplete);
        assertFalse(status.areDocsComplete);
        assertTrue(status.missingProfileFields.contains("nickname"));
        assertTrue(status.missingProfileFields.contains("passport_no"));
    }

    /**
     * Matrix Case 2: Profile incomplete + documents complete -> Cannot book.
     */
    @Test
    public void testCase2_profileIncomplete_and_docsComplete_blocksBooking() {
        Map<String, String> incompleteExtras = new HashMap<>(completeExtras);
        incompleteExtras.remove("emergency_name"); // missing emergency contact

        BookingEligibility.Status status = BookingEligibility.check(true, null, incompleteExtras, completeDocs);
        assertFalse(status.isEligible());
        assertEquals(BookingEligibility.Reason.PROFILE_INCOMPLETE, status.reason);
        assertFalse(status.isProfileComplete);
        assertTrue(status.areDocsComplete);
        assertTrue(status.missingProfileFields.contains("emergency_name"));
    }

    /**
     * Matrix Case 3: Profile complete + documents incomplete -> CAN book.
     */
    @Test
    public void testCase3_profileComplete_and_docsIncomplete_canBook() {
        List<DocumentDto> partialDocs = new ArrayList<>();
        DocumentDto passport = new DocumentDto();
        passport.documentCode = "passport";
        passport.status = "verified";
        partialDocs.add(passport); // missing IC and Passport Photo

        BookingEligibility.Status status = BookingEligibility.check(true, null, completeExtras, partialDocs);
        assertTrue(status.isEligible());
        assertEquals(BookingEligibility.Reason.ELIGIBLE, status.reason);
        assertTrue(status.isProfileComplete);
        assertFalse(status.areDocsComplete);
        assertTrue(status.missingDocCodes.contains("ic"));
        assertTrue(status.missingDocCodes.contains("passport_photo"));
        assertFalse(status.missingDocCodes.contains("passport"));
    }

    /**
     * Matrix Case 4: Profile complete + documents complete -> CAN book.
     */
    @Test
    public void testCase4_profileComplete_and_docsComplete_canBook() {
        BookingEligibility.Status status = BookingEligibility.check(true, null, completeExtras, completeDocs);
        assertTrue(status.isEligible());
        assertTrue(status.isProfileComplete);
        assertTrue(status.areDocsComplete);
        assertEquals(BookingEligibility.Reason.ELIGIBLE, status.reason);
        assertTrue(status.missingProfileFields.isEmpty());
        assertTrue(status.missingDocCodes.isEmpty());
    }

    /**
     * Matrix Case 5: Profile complete + Mahram empty -> CAN book.
     */
    @Test
    public void testCase5_profileComplete_and_mahramEmpty_canBook() {
        Map<String, String> extras = new HashMap<>(completeExtras);
        // Mahram explicitly omitted or blank
        extras.put("mahram_name", "");
        extras.put("mahram_relationship", "");

        BookingEligibility.Status status = BookingEligibility.check(true, null, extras, new ArrayList<>());
        assertTrue(status.isEligible());
        assertEquals(BookingEligibility.Reason.ELIGIBLE, status.reason);
        assertTrue(status.isProfileComplete);
        assertFalse(status.missingProfileFields.contains("mahram_name"));
        assertFalse(status.missingProfileFields.contains("mahram_relationship"));
    }

    /**
     * Matrix Case 6: Profile complete + Travel Visa unavailable -> CAN book.
     */
    @Test
    public void testCase6_profileComplete_and_visaUnavailable_canBook() {
        // Complete profile, 0 documents (no visa, no passport copy)
        BookingEligibility.Status status = BookingEligibility.check(true, null, completeExtras, new ArrayList<>());
        assertTrue(status.isEligible());
        assertEquals(BookingEligibility.Reason.ELIGIBLE, status.reason);
        assertTrue(status.isProfileComplete);
        assertFalse(status.missingDocCodes.contains("visa"));
    }

    @Test
    public void testCase7_profileFieldsTracking_blocksWhenMissingField_unblocksWhenFilled() {
        Map<String, String> extras = new HashMap<>(completeExtras);
        extras.remove("email");

        BookingEligibility.Status before = BookingEligibility.check(true, null, extras, completeDocs);
        assertFalse(before.isEligible());
        assertEquals(BookingEligibility.Reason.PROFILE_INCOMPLETE, before.reason);
        assertFalse(before.isProfileComplete);
        assertTrue(before.missingProfileFields.contains("email"));

        // User fills in email
        extras.put("email", "hafiz@example.com");
        BookingEligibility.Status after = BookingEligibility.check(true, null, extras, completeDocs);
        assertTrue(after.isEligible());
        assertEquals(BookingEligibility.Reason.ELIGIBLE, after.reason);
        assertTrue(after.isProfileComplete);
        assertTrue(after.missingProfileFields.isEmpty());
    }

    @Test
    public void testCase8_rejectedDocument_tracksIncompleteDoc_allowsBooking() {
        List<DocumentDto> docs = new ArrayList<>(completeDocs);
        // Change passport status to rejected
        DocumentDto rejectedPassport = new DocumentDto();
        rejectedPassport.documentCode = "passport";
        rejectedPassport.status = "rejected";
        rejectedPassport.rejectionReason = "Blurry image";
        docs.set(0, rejectedPassport);

        BookingEligibility.Status status = BookingEligibility.check(true, null, completeExtras, docs);
        assertTrue(status.isEligible());
        assertEquals(BookingEligibility.Reason.ELIGIBLE, status.reason);
        assertFalse(status.areDocsComplete);
        assertTrue(status.missingDocCodes.contains("passport"));
    }

    @Test
    public void testCase9_whitespaceFields_countAsIncomplete_blocksBooking() {
        Map<String, String> whitespaceExtras = new HashMap<>();
        whitespaceExtras.put("name", "   ");
        whitespaceExtras.put("nickname", "   ");
        whitespaceExtras.put("passport_no", "   ");
        whitespaceExtras.put("email", "   ");
        whitespaceExtras.put("emergency_name", "   ");

        BookingEligibility.Status status = BookingEligibility.check(true, null, whitespaceExtras, completeDocs);
        assertFalse(status.isEligible());
        assertEquals(BookingEligibility.Reason.PROFILE_INCOMPLETE, status.reason);
        assertFalse(status.isProfileComplete);
        assertTrue(status.missingProfileFields.contains("name"));
        assertTrue(status.missingProfileFields.contains("nickname"));
        assertTrue(status.missingProfileFields.contains("passport_no"));
        assertTrue(status.missingProfileFields.contains("email"));
        assertTrue(status.missingProfileFields.contains("emergency_name"));
    }

    @Test
    public void testCase10_userDtoFallback_whenExtrasEmpty() {
        UserDto user = new UserDto();
        user.name = "Muhammad Hafiz";
        user.nickname = "hafiz";
        user.dateOfBirth = "1990-01-01";
        user.gender = "Male";
        user.nationality = "Malaysia";
        user.phone = "+60123456789";
        user.email = "hafiz@example.com";
        user.passportNumber = "A99887766";
        user.passportExpiryDate = "2030-01-01";
        user.issuingCountry = "Malaysia";
        user.emergencyName = "Aishah";
        user.emergencyPhone = "+60198765432";

        BookingEligibility.Status status = BookingEligibility.check(true, user, new HashMap<>(), completeDocs);
        assertTrue(status.isEligible());
        assertEquals(BookingEligibility.Reason.ELIGIBLE, status.reason);
        assertTrue(status.isProfileComplete);
    }

    @Test
    public void testCase11_accountReadiness100_travelDocsIncomplete_canBook() {
        // Complete profile (Account Readiness = 100%), 0 travel documents uploaded
        BookingEligibility.Status status = BookingEligibility.check(true, null, completeExtras, new ArrayList<>());
        assertTrue("Account readiness 100% with incomplete travel documents CAN book", status.isEligible());
        assertEquals(BookingEligibility.Reason.ELIGIBLE, status.reason);
        assertTrue(status.isProfileComplete);
        assertFalse(status.areDocsComplete);
    }

    @Test
    public void testCase12_accountReadiness100_expiredPassport_canBook() {
        Map<String, String> extras = new HashMap<>(completeExtras);
        extras.put("passport_expiry", "2020-01-01"); // Expired date in the past

        BookingEligibility.Status status = BookingEligibility.check(true, null, extras, completeDocs);
        assertTrue("Account readiness 100% with expired passport CAN book", status.isEligible());
        assertEquals(BookingEligibility.Reason.ELIGIBLE, status.reason);
        assertTrue(status.isProfileComplete);
    }

    @Test
    public void testCase13_accountReadiness100_passportLessThan6MonthsValidity_canBook() {
        Map<String, String> extras = new HashMap<>(completeExtras);
        extras.put("passport_expiry", "2026-10-01"); // Less than 6 months remaining

        BookingEligibility.Status status = BookingEligibility.check(true, null, extras, completeDocs);
        assertTrue("Account readiness 100% with passport < 6 months validity CAN book", status.isEligible());
        assertEquals(BookingEligibility.Reason.ELIGIBLE, status.reason);
        assertTrue(status.isProfileComplete);
    }
}
