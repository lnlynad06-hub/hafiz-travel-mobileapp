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
 * Tests for BookingEligibility according to business rules:
 * 1. Profile completion and Travel Documents are NOT booking requirements.
 * 2. Customers may register/book a package even when profile or documents are incomplete.
 * 3. Not logged in users remain blocked from booking.
 * 4. Profile & document completeness are still tracked accurately for status/dashboard reporting.
 */
public class BookingEligibilityTest {

    private Map<String, String> completeExtras;
    private List<DocumentDto> completeDocs;

    @Before
    public void setUp() {
        completeExtras = new HashMap<>();
        completeExtras.put("name", "Muhammad Hafiz");
        completeExtras.put("ic_no", "920101-14-1234");
        completeExtras.put("passport_no", "A99887766");
        completeExtras.put("address", "No 10, Jalan Ampang, 50450 Kuala Lumpur");
        completeExtras.put("emergency_name", "Aishah");

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
    public void testCase1_notLoggedIn_blocksBooking() {
        BookingEligibility.Status status = BookingEligibility.check(false, null, completeExtras, completeDocs);
        assertFalse(status.isEligible());
        assertEquals(BookingEligibility.Reason.NOT_LOGGED_IN, status.reason);
    }

    @Test
    public void testCase2_profileIncomplete_and_docsIncomplete_allowsBookingWhileReportingIncomplete() {
        Map<String, String> incompleteExtras = new HashMap<>();
        incompleteExtras.put("name", "Muhammad Hafiz"); // missing IC, Passport, Address, Emergency

        List<DocumentDto> emptyDocs = new ArrayList<>();

        BookingEligibility.Status status = BookingEligibility.check(true, null, incompleteExtras, emptyDocs);
        // Booking is NOT blocked based on profile/document completion
        assertTrue(status.isEligible());
        assertEquals(BookingEligibility.Reason.ELIGIBLE, status.reason);
        // Completeness tracking remains accurate for profile / vault status
        assertFalse(status.isProfileComplete);
        assertFalse(status.areDocsComplete);
        assertTrue(status.missingProfileFields.contains("ic_no"));
        assertTrue(status.missingProfileFields.contains("passport_no"));
    }

    @Test
    public void testCase3_profileComplete_and_docsIncomplete_allowsBookingWhileReportingMissingDocs() {
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

    @Test
    public void testCase4_profileIncomplete_and_docsComplete_allowsBooking() {
        Map<String, String> incompleteExtras = new HashMap<>(completeExtras);
        incompleteExtras.remove("emergency_name");

        BookingEligibility.Status status = BookingEligibility.check(true, null, incompleteExtras, completeDocs);
        assertTrue(status.isEligible());
        assertEquals(BookingEligibility.Reason.ELIGIBLE, status.reason);
        assertFalse(status.isProfileComplete);
        assertTrue(status.areDocsComplete);
        assertTrue(status.missingProfileFields.contains("emergency_name"));
    }

    @Test
    public void testCase5_profileComplete_and_docsComplete_allowsBooking() {
        BookingEligibility.Status status = BookingEligibility.check(true, null, completeExtras, completeDocs);
        assertTrue(status.isEligible());
        assertTrue(status.isProfileComplete);
        assertTrue(status.areDocsComplete);
        assertEquals(BookingEligibility.Reason.ELIGIBLE, status.reason);
        assertTrue(status.missingProfileFields.isEmpty());
        assertTrue(status.missingDocCodes.isEmpty());
    }

    @Test
    public void testCase6_profileFieldsTracking_updatesAccurately() {
        Map<String, String> extras = new HashMap<>(completeExtras);
        extras.remove("address");

        BookingEligibility.Status before = BookingEligibility.check(true, null, extras, completeDocs);
        assertTrue(before.isEligible());
        assertFalse(before.isProfileComplete);
        assertTrue(before.missingProfileFields.contains("address"));

        // User fills in address
        extras.put("address", "123 Jalan Ampang, KL");
        BookingEligibility.Status after = BookingEligibility.check(true, null, extras, completeDocs);
        assertTrue(after.isEligible());
        assertTrue(after.isProfileComplete);
        assertTrue(after.missingProfileFields.isEmpty());
    }

    @Test
    public void testCase7_documentUploadTracking_updatesAccurately() {
        List<DocumentDto> docs = new ArrayList<>();
        DocumentDto passport = new DocumentDto();
        passport.documentCode = "passport";
        passport.status = "verified";
        docs.add(passport);

        DocumentDto ic = new DocumentDto();
        ic.documentCode = "ic";
        ic.status = "pending";
        docs.add(ic);

        BookingEligibility.Status before = BookingEligibility.check(true, null, completeExtras, docs);
        assertTrue(before.isEligible());
        assertFalse(before.areDocsComplete);
        assertEquals(1, before.missingDocCodes.size());
        assertEquals("passport_photo", before.missingDocCodes.get(0));

        // User uploads passport photo
        DocumentDto photo = new DocumentDto();
        photo.documentCode = "passport_photo";
        photo.status = "pending";
        docs.add(photo);

        BookingEligibility.Status after = BookingEligibility.check(true, null, completeExtras, docs);
        assertTrue(after.isEligible());
        assertTrue(after.areDocsComplete);
        assertTrue(after.missingDocCodes.isEmpty());
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
        assertFalse(status.areDocsComplete);
        assertTrue(status.missingDocCodes.contains("passport"));
    }

    @Test
    public void testCase9_whitespaceFields_countAsIncomplete() {
        Map<String, String> whitespaceExtras = new HashMap<>();
        whitespaceExtras.put("name", "   ");
        whitespaceExtras.put("ic_no", "   ");
        whitespaceExtras.put("passport_no", "   ");
        whitespaceExtras.put("address", "   ");
        whitespaceExtras.put("emergency_name", "   ");

        BookingEligibility.Status status = BookingEligibility.check(true, null, whitespaceExtras, completeDocs);
        assertTrue(status.isEligible());
        assertFalse(status.isProfileComplete);
        assertEquals(5, status.missingProfileFields.size());
    }

    @Test
    public void testCase10_optionalVisaDoc_isNotCustomerUploadRequirement() {
        // Company handles visa process; visa is not required from customer
        BookingEligibility.Status status = BookingEligibility.check(true, null, completeExtras, completeDocs);
        assertTrue(status.isEligible());
        assertFalse(status.missingDocCodes.contains("visa"));
    }

    @Test
    public void testCase11_userDtoFallback_whenExtrasEmpty() {
        UserDto user = new UserDto();
        user.name = "Muhammad Hafiz";
        user.icNumber = "920101-14-1234";
        user.passportNumber = "A99887766";
        user.address = "No 10, Jalan Ampang, KL";
        user.emergencyName = "Aishah";

        BookingEligibility.Status status = BookingEligibility.check(true, user, new HashMap<>(), completeDocs);
        assertTrue(status.isEligible());
        assertTrue(status.isProfileComplete);
    }
}
