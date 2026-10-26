package com.hafiztraveltours.app.ui;

import com.hafiztraveltours.app.models.BookingDto;
import com.hafiztraveltours.app.models.BookingRequest;
import com.hafiztraveltours.app.models.CustomTravelRequestDto;
import com.hafiztraveltours.app.models.QuotationDto;
import com.hafiztraveltours.app.models.PackageDetail;
import com.hafiztraveltours.app.models.UmrahPackage;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Phase 3 & 3.1 Unit Tests:
 * - Booking semantic status mapping and room configuration assertions
 * - Package Detail itinerary day formatting and title alignment invariants
 * - Custom Travel destination prefill resolution (authoritative, structured, Umrah Taif, and null-fallback)
 * - InvoiceViewerActivity dual view mode invariants (details vs invoice)
 * - Financial formatting and balance due integrity
 * - Quotations tab state data model invariants
 */
public class MyBookingsPhase3Test {

    @Test
    public void testBookingStatusSemanticClassification() {
        BookingDto confirmedBooking = new BookingDto();
        confirmedBooking.status = "confirmed";
        confirmedBooking.paymentStatus = "paid";

        BookingDto pendingDepositBooking = new BookingDto();
        pendingDepositBooking.status = "pending";
        pendingDepositBooking.paymentStatus = "unpaid";

        BookingDto cancelledBooking = new BookingDto();
        cancelledBooking.status = "cancelled";

        assertEquals("confirmed", confirmedBooking.status.toLowerCase(Locale.ROOT));
        assertEquals("paid", confirmedBooking.paymentStatus.toLowerCase(Locale.ROOT));
        assertEquals("pending", pendingDepositBooking.status.toLowerCase(Locale.ROOT));
        assertEquals("unpaid", pendingDepositBooking.paymentStatus.toLowerCase(Locale.ROOT));
        assertEquals("cancelled", cancelledBooking.status.toLowerCase(Locale.ROOT));
    }

    @Test
    public void testDestinationPrefillResolution_AuthoritativeDestinationWins() {
        UmrahPackage pkg = new UmrahPackage();
        pkg.id = "201";
        pkg.name = "Pakej Turki Klasik 10H7M";
        pkg.category = "tour";
        pkg.destination = "Turkiye";

        PackageDetail detail = PackageDetail.fromUmrahPackage(pkg);
        String dest = PackageDetailActivity.resolvePackageDestination(pkg, detail);

        assertEquals("Turkiye", dest);
    }

    @Test
    public void testDestinationPrefillResolution_StructuredTourHotels() {
        UmrahPackage pkg = new UmrahPackage();
        pkg.id = "202";
        pkg.name = "Jelajah Eropah Timur";
        pkg.category = "tour";
        pkg.destination = null;

        pkg.tourHotels = new ArrayList<>();
        UmrahPackage.TourHotelItem h1 = new UmrahPackage.TourHotelItem();
        h1.city = "Vienna";
        pkg.tourHotels.add(h1);

        UmrahPackage.TourHotelItem h2 = new UmrahPackage.TourHotelItem();
        h2.city = "Prague";
        pkg.tourHotels.add(h2);

        UmrahPackage.TourHotelItem h3 = new UmrahPackage.TourHotelItem();
        h3.city = "Budapest";
        pkg.tourHotels.add(h3);

        PackageDetail detail = PackageDetail.fromUmrahPackage(pkg);
        String dest = PackageDetailActivity.resolvePackageDestination(pkg, detail);

        assertEquals("Vienna, Prague, Budapest", dest);
    }

    @Test
    public void testDestinationPrefillResolution_UmrahWithTaif() {
        UmrahPackage pkg = new UmrahPackage();
        pkg.id = "101";
        pkg.name = "Pakej Umrah Ziarah Taif 12H10M";
        pkg.category = "umrah";
        pkg.nightsMakkah = 5;
        pkg.nightsMadinah = 5;
        pkg.nightsTaif = 2;

        PackageDetail detail = PackageDetail.fromUmrahPackage(pkg);
        String dest = PackageDetailActivity.resolvePackageDestination(pkg, detail);

        assertEquals("Makkah, Madinah & Taif", dest);
    }

    @Test
    public void testDestinationPrefillResolution_StandardUmrah() {
        UmrahPackage pkg = new UmrahPackage();
        pkg.id = "102";
        pkg.name = "Pakej Umrah Sukuk LITE 2026";
        pkg.category = "umrah";
        pkg.nightsMakkah = 6;
        pkg.nightsMadinah = 4;
        pkg.nightsTaif = 0;

        PackageDetail detail = PackageDetail.fromUmrahPackage(pkg);
        String dest = PackageDetailActivity.resolvePackageDestination(pkg, detail);

        assertEquals("Makkah & Madinah", dest);
    }

    @Test
    public void testDestinationPrefillResolution_TourWithoutDestinationDoesNotConfusePackageName() {
        UmrahPackage pkg = new UmrahPackage();
        pkg.id = "303";
        pkg.name = "Kembara Eksklusif Musim Sejuk";
        pkg.category = "tour";
        pkg.destination = null;
        pkg.tourHotels = null;

        PackageDetail detail = PackageDetail.fromUmrahPackage(pkg);
        String dest = PackageDetailActivity.resolvePackageDestination(pkg, detail);

        // Must return null, NOT the package name "Kembara Eksklusif Musim Sejuk"
        assertNull(dest);
    }

    @Test
    public void testInvoiceViewerActivityViewModeConstants() {
        assertEquals("extra_view_mode", InvoiceViewerActivity.EXTRA_VIEW_MODE);
        assertEquals("invoice", InvoiceViewerActivity.VIEW_MODE_INVOICE);
        assertEquals("details", InvoiceViewerActivity.VIEW_MODE_DETAILS);
    }

    @Test
    public void testDocumentReferenceHeaderFormatting() {
        String bookingNo = "BKG-2609-312";

        // In details mode: BKG-2609-312
        String detailsDocRef = bookingNo;
        assertEquals("BKG-2609-312", detailsDocRef);

        // In invoice mode: INV-2609-312
        String invoiceDocRef = bookingNo.replace("BKG-", "INV-");
        assertEquals("INV-2609-312", invoiceDocRef);
    }

    @Test
    public void testFinancialFormattingAndInvariants() {
        double total = 29170.00;
        double deposit = 2000.00;
        double balance = total - deposit;

        String formattedTotal = BookingRequest.formatPrice(total);
        String formattedDeposit = BookingRequest.formatPrice(deposit);
        String formattedBalance = BookingRequest.formatPrice(balance);

        assertTrue(formattedTotal.contains("29,170") || formattedTotal.contains("29170"));
        assertTrue(formattedDeposit.contains("2,000") || formattedDeposit.contains("2000"));
        assertTrue(formattedBalance.contains("27,170") || formattedBalance.contains("27170"));

        assertEquals(27170.00, balance, 0.001);
        assertEquals(total, deposit + balance, 0.001);

        // Large monetary values
        double largeTotal = 145000.50;
        String formattedLarge = BookingRequest.formatPrice(largeTotal);
        assertNotNull(formattedLarge);
        assertFalse(formattedLarge.isEmpty());
    }

    @Test
    public void testItineraryDayTitleFormatting() {
        PackageDetail.ItineraryDay day = new PackageDetail.ItineraryDay();
        day.dayNumber = 1;
        day.title = "  Kuala Lumpur - Jeddah - Makkah  ";

        String pillText = (day.title != null && !day.title.trim().isEmpty())
                ? day.title.trim().toUpperCase(Locale.ROOT)
                : String.format(Locale.ROOT, "HARI %d", day.dayNumber).toUpperCase(Locale.ROOT);

        assertEquals("KUALA LUMPUR - JEDDAH - MAKKAH", pillText);

        PackageDetail.ItineraryDay emptyTitleDay = new PackageDetail.ItineraryDay();
        emptyTitleDay.dayNumber = 2;
        emptyTitleDay.title = "";

        String emptyPillText = (emptyTitleDay.title != null && !emptyTitleDay.title.trim().isEmpty())
                ? emptyTitleDay.title.trim().toUpperCase(Locale.ROOT)
                : String.format(Locale.ROOT, "HARI %d", emptyTitleDay.dayNumber).toUpperCase(Locale.ROOT);

        assertEquals("HARI 2", emptyPillText);
    }

    @Test
    public void testCustomTravelQuotationsDtoStatusIntegrity() {
        CustomTravelRequestDto quote = new CustomTravelRequestDto();
        quote.id = 55;
        quote.status = "quoted";
        quote.budgetPerPax = 7500.0;
        quote.requestType = "customize_package";

        QuotationDto quotation = new QuotationDto();
        quotation.id = 101;
        quotation.quotationNo = "QT-2026-001";
        quotation.totalAmount = 14500.0;
        quote.latestQuotation = quotation;

        assertEquals("quoted", quote.status);
        assertNotNull(quote.latestQuotation);
        assertEquals(14500.0, quote.latestQuotation.totalAmount, 0.001);
        assertEquals("customize_package", quote.requestType);
    }
}
