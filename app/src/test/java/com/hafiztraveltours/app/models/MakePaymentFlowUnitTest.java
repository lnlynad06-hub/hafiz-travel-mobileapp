package com.hafiztraveltours.app.models;

import com.hafiztraveltours.app.utils.MoneyFormat;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MakePaymentFlowUnitTest {

    @Test
    public void testUmrahDepositAmountIs1000() {
        BookingDto umrahBooking = new BookingDto();
        umrahBooking.packageCategory = "umrah";
        umrahBooking.packageName = "Pakej Umrah Sukuk Lite";
        umrahBooking.totalPax = 1;
        umrahBooking.totalAmount = 7950.0;
        umrahBooking.paidAmount = 0.0;
        umrahBooking.balanceAmount = 7950.0;

        assertFalse("Should not be tour package", umrahBooking.isTourPackage());
        assertEquals("Umrah deposit must be RM1,000", 1000.0, umrahBooking.getRequiredDepositAmount(), 0.001);
        assertEquals("Initial deposit remaining must be RM1,000", 1000.0, umrahBooking.getDepositRemainingAmount(), 0.001);

        String formattedAmount = MoneyFormat.formatRMCents(umrahBooking.getRequiredDepositAmount());
        assertEquals("RM1,000.00", formattedAmount);
    }

    @Test
    public void testTourDepositAmountIs500() {
        BookingDto tourBooking = new BookingDto();
        tourBooking.packageCategory = "tour";
        tourBooking.packageName = "Pakej Pelancongan Switzerland";
        tourBooking.isTour = true;
        tourBooking.totalPax = 1;
        tourBooking.totalAmount = 9000.0;
        tourBooking.paidAmount = 0.0;
        tourBooking.balanceAmount = 9000.0;

        assertTrue("Should be recognized as tour package", tourBooking.isTourPackage());
        assertEquals("Tour deposit must be RM500", 500.0, tourBooking.getRequiredDepositAmount(), 0.001);
        assertEquals("Tour deposit remaining must be RM500", 500.0, tourBooking.getDepositRemainingAmount(), 0.001);

        String formattedAmount = MoneyFormat.formatRMCents(tourBooking.getRequiredDepositAmount());
        assertEquals("RM500.00", formattedAmount);
    }

    @Test
    public void testTourCategoryDetectionFromKeywords() {
        BookingDto b1 = new BookingDto();
        b1.packageName = "Pakej Vietnam Danang Muslim";
        assertTrue("Vietnam keyword should detect tour", b1.isTourPackage());
        assertEquals(500.0, b1.getRequiredDepositAmount(), 0.001);

        BookingDto b2 = new BookingDto();
        b2.packageName = "Pakej Balkan 7 Negara";
        assertTrue("Balkan keyword should detect tour", b2.isTourPackage());
        assertEquals(500.0, b2.getRequiredDepositAmount(), 0.001);

        BookingDto b3 = new BookingDto();
        b3.packageCategory = "outbound";
        assertTrue("Outbound category should detect tour", b3.isTourPackage());
        assertEquals(500.0, b3.getRequiredDepositAmount(), 0.001);
    }

    @Test
    public void testPaymentLifecycleDepositThenBalance() {
        BookingDto booking = new BookingDto();
        booking.packageCategory = "umrah";
        booking.packageName = "Pakej Umrah Sukuk Lite";
        booking.totalPax = 1;
        booking.totalAmount = 7950.0;
        booking.paidAmount = 0.0;
        booking.balanceAmount = 7950.0;

        // Stage 1: Initial Deposit required
        double firstPayment = booking.getDepositRemainingAmount();
        assertEquals(1000.0, firstPayment, 0.001);
        assertEquals("RM1,000.00", MoneyFormat.formatRMCents(firstPayment));

        // Simulate deposit payment recorded
        booking.paidAmount += firstPayment;
        booking.balanceAmount -= firstPayment;
        booking.depositPaid += firstPayment;
        booking.depositRemaining = Math.max(0, booking.depositRemaining - firstPayment);
        booking.isDepositPaid = true;

        assertEquals(1000.0, booking.paidAmount, 0.001);
        assertEquals(6950.0, booking.balanceAmount, 0.001);
        assertEquals(0.0, booking.getDepositRemainingAmount(), 0.001);

        // Stage 2: Deposit is complete, next payment is remaining package balance
        double nextPayment = booking.balanceAmount;
        assertEquals(6950.0, nextPayment, 0.001);
        assertEquals("RM6,950.00", MoneyFormat.formatRMCents(nextPayment));

        // Simulate full balance payment
        booking.paidAmount += nextPayment;
        booking.balanceAmount -= nextPayment;

        assertEquals(7950.0, booking.paidAmount, 0.001);
        assertEquals(0.0, booking.balanceAmount, 0.001);
    }

    @Test
    public void testMoneyFormatCentsEdgeCases() {
        assertEquals("RM0.00", MoneyFormat.formatRMCents(0.0));
        assertEquals("RM500.00", MoneyFormat.formatRMCents(500.0));
        assertEquals("RM1,000.00", MoneyFormat.formatRMCents(1000.0));
        assertEquals("RM1,234.50", MoneyFormat.formatRMCents(1234.5));
        assertEquals("RM12,500.00", MoneyFormat.formatRMCents(12500.0));
        assertEquals("RM0.00", MoneyFormat.formatRMCents(Double.NaN));
    }

    @Test
    public void testMultiInstallmentSequenceAndCalculations() {
        BookingDto booking = new BookingDto();
        booking.totalAmount = 10000.0;
        booking.paidAmount = 0.0;
        booking.balanceAmount = 10000.0;
        booking.packageCategory = "umrah";
        booking.totalPax = 1;

        // Payment #1: Initial Deposit RM 1,000
        double pay1 = 1000.0;
        booking.paidAmount += pay1;
        booking.balanceAmount = Math.max(0, booking.totalAmount - booking.paidAmount);
        assertEquals(1000.0, booking.paidAmount, 0.001);
        assertEquals(9000.0, booking.balanceAmount, 0.001);

        // Payment #2: Additional Payment RM 100
        double pay2 = 100.0;
        booking.paidAmount += pay2;
        booking.balanceAmount = Math.max(0, booking.totalAmount - booking.paidAmount);
        assertEquals(1100.0, booking.paidAmount, 0.001);
        assertEquals(8900.0, booking.balanceAmount, 0.001);

        // Payment #3: Additional Payment RM 200
        double pay3 = 200.0;
        booking.paidAmount += pay3;
        booking.balanceAmount = Math.max(0, booking.totalAmount - booking.paidAmount);
        assertEquals(1300.0, booking.paidAmount, 0.001);
        assertEquals(8700.0, booking.balanceAmount, 0.001);

        // Payment #4: Additional Payment RM 500
        double pay4 = 500.0;
        booking.paidAmount += pay4;
        booking.balanceAmount = Math.max(0, booking.totalAmount - booking.paidAmount);
        assertEquals(1800.0, booking.paidAmount, 0.001);
        assertEquals(8200.0, booking.balanceAmount, 0.001);

        // Payment #5: Final Payment RM 8,200 (reaches full package amount)
        double pay5 = booking.balanceAmount;
        booking.paidAmount += pay5;
        booking.balanceAmount = Math.max(0, booking.totalAmount - booking.paidAmount);
        assertEquals(10000.0, booking.paidAmount, 0.001);
        assertEquals(0.0, booking.balanceAmount, 0.001);
    }

    @Test
    public void testInvoiceAndPaymentDtoFieldsIntegrity() {
        BookingDetailDto detail = new BookingDetailDto();
        detail.id = 42;
        detail.bookingNo = "BKG-260930-TEST";
        detail.totalAmount = 5000.0;
        detail.paidAmount = 1500.0;
        detail.balanceAmount = 3500.0;

        BookingDetailDto.InvoiceInfo invoice = new BookingDetailDto.InvoiceInfo();
        invoice.id = 10;
        invoice.invoiceNo = "INV-2026-0042";
        invoice.issuedAt = "2026-09-30";
        invoice.dueAt = "2026-10-14";
        invoice.subtotal = 5000.0;
        invoice.totalAmount = 5000.0;
        invoice.status = "issued";

        BookingDetailDto.PaymentInfo payment = new BookingDetailDto.PaymentInfo();
        payment.id = 1;
        payment.installmentIndex = 1;
        payment.paymentTitle = "Payment #1";
        payment.paymentNo = "PAY-260930-001";
        payment.amount = 1000.0;
        payment.stage = "initial_deposit";
        payment.stageLabel = "Deposit Awal";
        payment.isVerified = true;
        payment.receiptNo = "RCT-260930-001";

        assertEquals(10, invoice.id);
        assertEquals("INV-2026-0042", invoice.invoiceNo);
        assertEquals("2026-09-30", invoice.issuedAt);
        assertEquals("Payment #1", payment.paymentTitle);
        assertEquals("initial_deposit", payment.stage);
        assertEquals("RCT-260930-001", payment.receiptNo);
        assertTrue(payment.isVerified);
    }
}
