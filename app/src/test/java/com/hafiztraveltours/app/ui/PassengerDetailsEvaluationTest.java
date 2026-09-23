package com.hafiztraveltours.app.ui;

import com.hafiztraveltours.app.models.DocumentDto;
import com.hafiztraveltours.app.models.PackageDetail;
import com.hafiztraveltours.app.network.UserDto;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for PassengerDetailsViewModel lead completeness evaluation.
 *
 * Business rule:
 * "A customer MUST have their Edit Profile completed before they can book.
 * However, incomplete Travel Documents and passport validity must NOT prevent booking."
 *
 * Passport States:
 * - Passport expired -> ALLOW
 * - Passport < 6 months validity -> ALLOW
 * - Passport > 6 months validity -> ALLOW
 * - Passport expiry date not yet provided -> ALLOW
 * - Passport copy missing -> ALLOW
 */
public class PassengerDetailsEvaluationTest {

    private PackageDetail umrahPackage;
    private UserDto completeUser;
    private List<DocumentDto> completeDocs;

    @Before
    public void setUp() {
        umrahPackage = new PackageDetail();
        umrahPackage.isUmrah = true;
        umrahPackage.requiresPassport = true;
        umrahPackage.requiresIc = true;
        umrahPackage.requiresClothesSize = true;
        umrahPackage.passportValidityMonths = 6;

        // Future passport expiry date > 6 months
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, 2);
        String validExpiry = String.format("%04d-%02d-%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));

        completeUser = new UserDto();
        completeUser.name = "Muhammad Hafiz";
        completeUser.icNumber = "920101-14-1234";
        completeUser.passportNumber = "A99887766";
        completeUser.passportExpiryDate = validExpiry;
        completeUser.clothesSize = "L";

        completeDocs = new ArrayList<>();
        DocumentDto passportDoc = new DocumentDto();
        passportDoc.documentCode = "passport";
        passportDoc.filePath = "uploads/passport.jpg";
        passportDoc.status = "verified";
        completeDocs.add(passportDoc);

        DocumentDto icDoc = new DocumentDto();
        icDoc.documentCode = "ic";
        icDoc.filePath = "uploads/ic.jpg";
        icDoc.status = "verified";
        completeDocs.add(icDoc);

        DocumentDto photoDoc = new DocumentDto();
        photoDoc.documentCode = "passport_photo";
        photoDoc.filePath = "uploads/photo.jpg";
        photoDoc.status = "verified";
        completeDocs.add(photoDoc);
    }

    /**
     * Case 1: Profile incomplete + documents incomplete -> Cannot book.
     */
    @Test
    public void testCase1_profileIncomplete_and_docsIncomplete_cannotBook() {
        UserDto incompleteUser = new UserDto();
        incompleteUser.name = "Muhammad Hafiz";
        // Missing IC, Passport Number, Clothes Size
        List<DocumentDto> emptyDocs = new ArrayList<>();

        PassengerDetailsViewModel.LeadEvaluation eval =
                PassengerDetailsViewModel.checkLeadCompleteness(umrahPackage, incompleteUser, emptyDocs);

        assertFalse("Incomplete profile must NOT be allowed to book", eval.complete);
        assertFalse(eval.missing.isEmpty());
        assertFalse(eval.hasPassportDoc);
        assertFalse(eval.hasPhotoDoc);
    }

    /**
     * Case 2: Profile incomplete + documents complete -> Cannot book.
     */
    @Test
    public void testCase2_profileIncomplete_and_docsComplete_cannotBook() {
        UserDto incompleteUser = new UserDto();
        incompleteUser.name = "Muhammad Hafiz";
        // Missing IC, Passport Number
        incompleteUser.clothesSize = "L";

        PassengerDetailsViewModel.LeadEvaluation eval =
                PassengerDetailsViewModel.checkLeadCompleteness(umrahPackage, incompleteUser, completeDocs);

        assertFalse("Incomplete profile with complete documents must still NOT book", eval.complete);
        assertFalse(eval.missing.isEmpty());
        assertTrue(eval.hasPassportDoc);
        assertTrue(eval.hasPhotoDoc);
    }

    /**
     * Case 3: Profile complete + documents incomplete -> CAN book.
     */
    @Test
    public void testCase3_profileComplete_and_docsIncomplete_canBook() {
        // 0 documents uploaded
        List<DocumentDto> emptyDocs = new ArrayList<>();

        PassengerDetailsViewModel.LeadEvaluation eval =
                PassengerDetailsViewModel.checkLeadCompleteness(umrahPackage, completeUser, emptyDocs);

        assertTrue("Complete profile with missing documents MUST be allowed to book", eval.complete);
        assertTrue("Missing profile fields must be empty", eval.missing.isEmpty());
        assertFalse("Document status reflects actual state without blocking", eval.hasPassportDoc);
        assertFalse("Photo status reflects actual state without blocking", eval.hasPhotoDoc);
    }

    /**
     * Case 4: Profile complete + documents complete -> CAN book.
     */
    @Test
    public void testCase4_profileComplete_and_docsComplete_canBook() {
        PassengerDetailsViewModel.LeadEvaluation eval =
                PassengerDetailsViewModel.checkLeadCompleteness(umrahPackage, completeUser, completeDocs);

        assertTrue("Complete profile with complete documents can book", eval.complete);
        assertTrue(eval.missing.isEmpty());
        assertTrue(eval.hasPassportDoc);
        assertTrue(eval.hasPhotoDoc);
        assertTrue(eval.hasIcDoc);
    }

    /**
     * Case 5: Profile complete + Mahram empty -> CAN book.
     */
    @Test
    public void testCase5_profileComplete_and_mahramEmpty_canBook() {
        // Mahram is optional; not a field in UserDto / lead evaluation
        PassengerDetailsViewModel.LeadEvaluation eval =
                PassengerDetailsViewModel.checkLeadCompleteness(umrahPackage, completeUser, new ArrayList<>());

        assertTrue("Complete profile with empty Mahram MUST be allowed to book", eval.complete);
        assertTrue(eval.missing.isEmpty());
    }

    /**
     * Case 6: Profile complete + Travel Visa unavailable -> CAN book.
     */
    @Test
    public void testCase6_profileComplete_and_visaUnavailable_canBook() {
        // Visa is never a prerequisite for booking
        PassengerDetailsViewModel.LeadEvaluation eval =
                PassengerDetailsViewModel.checkLeadCompleteness(umrahPackage, completeUser, new ArrayList<>());

        assertTrue("Complete profile without travel visa MUST be allowed to book", eval.complete);
        assertTrue(eval.missing.isEmpty());
    }

    /**
     * Case 7: Passport expired -> CAN book (informational warning only).
     */
    @Test
    public void testCase7_passportExpired_canBookWithWarning() {
        completeUser.passportExpiryDate = "2020-01-01"; // Expired in the past

        PassengerDetailsViewModel.LeadEvaluation eval =
                PassengerDetailsViewModel.checkLeadCompleteness(umrahPackage, completeUser, completeDocs);

        assertTrue("Expired passport must NOT block booking", eval.complete);
        assertTrue("Missing required profile fields must be empty", eval.missing.isEmpty());
        assertTrue("Must report informational passport validity warning", eval.hasPassportValidityWarning);
    }

    /**
     * Case 8: Passport < 6 months validity -> CAN book (informational warning only).
     */
    @Test
    public void testCase8_passportLessThan6MonthsValidity_canBookWithWarning() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 2); // Valid for only 2 months (< 6 months)
        completeUser.passportExpiryDate = String.format("%04d-%02d-%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));

        PassengerDetailsViewModel.LeadEvaluation eval =
                PassengerDetailsViewModel.checkLeadCompleteness(umrahPackage, completeUser, completeDocs);

        assertTrue("Passport < 6 months validity must NOT block booking", eval.complete);
        assertTrue(eval.missing.isEmpty());
        assertTrue("Must report informational passport validity warning", eval.hasPassportValidityWarning);
    }

    /**
     * Case 9: Passport > 6 months validity -> CAN book (no validity warning).
     */
    @Test
    public void testCase9_passportMoreThan6MonthsValidity_canBookWithoutWarning() {
        PassengerDetailsViewModel.LeadEvaluation eval =
                PassengerDetailsViewModel.checkLeadCompleteness(umrahPackage, completeUser, completeDocs);

        assertTrue("Valid passport can book", eval.complete);
        assertTrue(eval.missing.isEmpty());
        assertFalse("No validity warning for valid passport", eval.hasPassportValidityWarning);
        assertFalse("Expiry date is present", eval.hasPassportExpiryMissing);
    }

    /**
     * Case 10: Passport expiry date not yet provided -> CAN book (informational pending only).
     */
    @Test
    public void testCase10_passportExpiryNotYetProvided_canBook() {
        completeUser.passportExpiryDate = ""; // Not provided yet

        PassengerDetailsViewModel.LeadEvaluation eval =
                PassengerDetailsViewModel.checkLeadCompleteness(umrahPackage, completeUser, completeDocs);

        assertTrue("Missing passport expiry date must NOT block booking", eval.complete);
        assertTrue(eval.missing.isEmpty());
        assertTrue("Reports informational expiry missing flag", eval.hasPassportExpiryMissing);
        assertFalse(eval.hasPassportValidityWarning);
    }

    /**
     * Case 11: Additional traveller with empty passport expiry date does not fail validation.
     */
    @Test
    public void testCase11_additionalTraveller_emptyPassportExpiry_valid() {
        PassengerDetailsViewModel.TravellerInput input = new PassengerDetailsViewModel.TravellerInput();
        input.fullName = "Aishah binti Ali";
        input.icNumber = "950505-10-5555";
        input.passportNumber = "A11223344";
        input.passportExpiryDate = ""; // Not yet provided

        PassengerDetailsViewModel.TravellerErrors errors =
                PassengerDetailsViewModel.validateTravellerInput(input, true, true);

        assertEquals("Name must have no error", 0, errors.nameErr);
        assertEquals("IC must have no error", 0, errors.icErr);
        assertEquals("Passport must have no error", 0, errors.passportErr);
        assertEquals("Expiry must have no error when not yet provided", 0, errors.expiryErr);
    }
}
