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
}
