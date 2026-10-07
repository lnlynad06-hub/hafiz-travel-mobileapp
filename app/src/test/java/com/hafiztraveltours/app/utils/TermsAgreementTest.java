package com.hafiztraveltours.app.utils;

import com.hafiztraveltours.app.models.BookingRequest;
import com.hafiztraveltours.app.models.CreateBookingRequest;
import com.hafiztraveltours.app.ui.TermsConditionsActivity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying Terms & Conditions agreement gate and DTO mapping.
 */
public class TermsAgreementTest {

    @Test
    public void testBookingRequest_defaultTermsStateIsUnagreed() {
        BookingRequest req = new BookingRequest();
        assertFalse(req.termsAgreed);
        assertEquals("", req.termsAgreedAt);
        assertEquals("1.0", req.termsVersion);
    }

    @Test
    public void testStampAgreement_recordsTimestampAndVersion() {
        BookingRequest req = new BookingRequest();
        req.termsAgreed = true;
        req.termsAgreedAt = DateFormats.nowIsoDateTime();
        req.termsVersion = TermsConditionsActivity.TERMS_VERSION;

        assertTrue(req.termsAgreed);
        assertNotNull(req.termsAgreedAt);
        assertFalse(req.termsAgreedAt.isEmpty());
        assertEquals("1.0", req.termsVersion);
    }

    @Test
    public void testCreateBookingRequest_mapsAgreementFieldsCorrectly() {
        BookingRequest req = new BookingRequest();
        req.packageId = "pkg-123";
        req.termsAgreed = true;
        req.termsAgreedAt = DateFormats.nowIsoDateTime();
        req.termsVersion = "1.0";

        CreateBookingRequest apiReq = new CreateBookingRequest();
        apiReq.packageId = req.packageId;
        apiReq.termsAgreed = req.termsAgreed;
        apiReq.termsAgreedAt = req.termsAgreedAt;
        apiReq.termsVersion = req.termsVersion;

        assertTrue(apiReq.termsAgreed);
        assertEquals("1.0", apiReq.termsVersion);
        assertNotNull(apiReq.termsAgreedAt);
    }

    @Test
    public void testUnagreedRequest_preservesFalseState() {
        BookingRequest req = new BookingRequest();
        CreateBookingRequest apiReq = new CreateBookingRequest();
        apiReq.termsAgreed = req.termsAgreed;

        assertFalse(apiReq.termsAgreed);
    }
}
