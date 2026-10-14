package com.hafiztraveltours.app.models;

import com.google.gson.Gson;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BookingEligibilityAndCancellationTest {

    private final Gson gson = new Gson();

    @Test
    public void testBookingEligibilityBlockedDeserialization() {
        String json = "{\n" +
                "  \"can_book\": false,\n" +
                "  \"reason\": \"UNPAID_DEPOSIT_EXISTS\",\n" +
                "  \"message\": \"Deposit required\",\n" +
                "  \"blocking_booking\": {\n" +
                "    \"id\": 42,\n" +
                "    \"booking_no\": \"BKG-202609-0042\",\n" +
                "    \"required_deposit\": 1000.0,\n" +
                "    \"paid_amount\": 0.0,\n" +
                "    \"deposit_remaining\": 1000.0\n" +
                "  }\n" +
                "}";

        BookingEligibilityDto dto = gson.fromJson(json, BookingEligibilityDto.class);
        assertNotNull(dto);
        assertFalse(dto.canBook);
        assertEquals("UNPAID_DEPOSIT_EXISTS", dto.reason);
        assertNotNull(dto.blockingBooking);
        assertEquals(42, dto.blockingBooking.id);
        assertEquals("BKG-202609-0042", dto.blockingBooking.bookingNo);
        assertEquals(1000.0, dto.blockingBooking.requiredDeposit, 0.001);
        assertEquals(1000.0, dto.blockingBooking.depositRemaining, 0.001);
    }

    @Test
    public void testBookingEligibilityAllowedDeserialization() {
        String json = "{\n" +
                "  \"can_book\": true,\n" +
                "  \"blocking_booking\": null\n" +
                "}";

        BookingEligibilityDto dto = gson.fromJson(json, BookingEligibilityDto.class);
        assertNotNull(dto);
        assertTrue(dto.canBook);
        assertNull(dto.blockingBooking);
    }

    @Test
    public void testCancelBookingRequestSerialization() {
        CancelBookingRequest req = new CancelBookingRequest("Customer requested change of plan");
        String json = gson.toJson(req);
        assertTrue(json.contains("Customer requested change of plan"));
        assertTrue(json.contains("reason"));
    }

    @Test
    public void testBookingDtoCancellationFields() {
        String json = "{\n" +
                "  \"id\": 10,\n" +
                "  \"booking_no\": \"BKG-202609-0010\",\n" +
                "  \"status\": \"confirmed\",\n" +
                "  \"is_cancellable\": true,\n" +
                "  \"cancellation_reason\": null,\n" +
                "  \"cancelled_at\": null\n" +
                "}";

        BookingDto dto = gson.fromJson(json, BookingDto.class);
        assertNotNull(dto);
        assertEquals(10, dto.id);
        assertTrue(dto.isCancellable);

        String cancelledJson = "{\n" +
                "  \"id\": 10,\n" +
                "  \"booking_no\": \"BKG-202609-0010\",\n" +
                "  \"status\": \"cancelled\",\n" +
                "  \"is_cancellable\": false,\n" +
                "  \"cancellation_reason\": \"Cancelled by customer\",\n" +
                "  \"cancelled_at\": \"2026-09-29 15:30:00\"\n" +
                "}";

        BookingDto cancelledDto = gson.fromJson(cancelledJson, BookingDto.class);
        assertNotNull(cancelledDto);
        assertFalse(cancelledDto.isCancellable);
        assertEquals("Cancelled by customer", cancelledDto.cancellationReason);
        assertEquals("2026-09-29 15:30:00", cancelledDto.cancelledAt);
    }

    @Test
    public void testPendingCancellationDisablesPaymentAndCarriesRefundState() {
        BookingDto dto = gson.fromJson("{\"status\":\"cancellation_requested\",\"total_amount\":5000,\"paid_amount\":1000,\"balance_amount\":4000,\"cancellation_status\":\"requested\",\"refund_status\":\"requested\",\"refund_requested_amount\":1000}", BookingDto.class);
        assertFalse(dto.canMakePayment());
        assertTrue(dto.isPendingCancellation());
        assertFalse(dto.isCancelled());
        assertTrue(dto.isRefundPendingOrProcessing());
        assertFalse(dto.isRefundCompleted());
        assertEquals("requested", dto.cancellationStatus);
        assertEquals("requested", dto.refundStatus);
        assertEquals(1000.0, dto.refundRequestedAmount, 0.001);
    }

    @Test
    public void testApprovedCancellationWithRefundProcessing() {
        String json = "{\"status\":\"cancelled\",\"total_amount\":5000,\"paid_amount\":3000,\"balance_amount\":2000,\"cancellation_status\":\"approved\",\"refund_status\":\"processing\",\"refund_approved_amount\":2500}";
        BookingDto dto = gson.fromJson(json, BookingDto.class);
        assertFalse(dto.canMakePayment());
        assertFalse(dto.isPendingCancellation());
        assertTrue(dto.isCancelled());
        assertTrue(dto.isRefundPendingOrProcessing());
        assertFalse(dto.isRefundCompleted());
        assertEquals("approved", dto.cancellationStatus);
        assertEquals("processing", dto.refundStatus);
        assertEquals(2500.0, dto.refundApprovedAmount, 0.001);
    }

    @Test
    public void testApprovedCancellationWithRefundCompleted() {
        String json = "{\"status\":\"cancelled\",\"total_amount\":5000,\"paid_amount\":3000,\"balance_amount\":2000,\"cancellation_status\":\"approved\",\"refund_status\":\"processed\",\"refund_approved_amount\":2500}";
        BookingDto dto = gson.fromJson(json, BookingDto.class);
        assertFalse(dto.canMakePayment());
        assertFalse(dto.isPendingCancellation());
        assertTrue(dto.isCancelled());
        assertFalse(dto.isRefundPendingOrProcessing());
        assertTrue(dto.isRefundCompleted());
        assertEquals("processed", dto.refundStatus);
    }

    @Test
    public void testRejectedCancellationRestoresActiveBookingAndPayment() {
        String json = "{\"status\":\"confirmed\",\"total_amount\":5000,\"paid_amount\":1000,\"balance_amount\":4000,\"cancellation_status\":\"rejected\",\"cancellation_review_reason\":\"Departure is within 7 days, policy forbids refund\",\"refund_status\":\"rejected\"}";
        BookingDto dto = gson.fromJson(json, BookingDto.class);
        assertFalse(dto.isPendingCancellation());
        assertFalse(dto.isCancelled());
        assertTrue(dto.isCancellationRejected());
        assertEquals("Departure is within 7 days, policy forbids refund", dto.cancellationReviewReason);
        assertTrue(dto.canMakePayment()); // Pay now eligibility restored!
    }

    @Test
    public void testUnpaidBookingPendingCancellation() {
        String json = "{\"status\":\"cancellation_requested\",\"total_amount\":5000,\"paid_amount\":0,\"balance_amount\":5000,\"cancellation_status\":\"requested\",\"refund_status\":null,\"refund_requested_amount\":null}";
        BookingDto dto = gson.fromJson(json, BookingDto.class);
        assertTrue(dto.isPendingCancellation());
        assertFalse(dto.isCancelled());
        assertFalse(dto.canMakePayment());
        assertNull(dto.refundStatus);
        assertNull(dto.refundRequestedAmount);
    }
}
