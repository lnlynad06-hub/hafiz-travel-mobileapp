package com.hafiztraveltours.app.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PackagePricingCalculatorTest {

    @Test
    public void testTourChildPricingRules() {
        com.hafiztraveltours.app.models.PackageDetail detail = new com.hafiztraveltours.app.models.PackageDetail();
        detail.isUmrah = false;
        detail.price = "2800.0";
        detail.childUnder2Price = 500.0;
        detail.child211WithBedPrice = 2700.0;
        detail.child211NoBedPrice = 2600.0;

        com.hafiztraveltours.app.models.PackageDetail.DepartureOption dep = new com.hafiztraveltours.app.models.PackageDetail.DepartureOption(
                "1", "2026-10-02", "2026-10-12", "Oct 2", 40, 0, 40, false);
        detail.availableDepartures.add(dep);

        double adultPrice = 2800.0;

        // 1. Infant < 2 years -> ERP configured RM500
        double infantPrice = PackagePricingCalculator.calculatePassengerPrice(
                detail, "standard", false, adultPrice, "2025-05-01", "2026-10-02", null);
        assertEquals(500.0, infantPrice, 0.001);

        // 2. Child 2-11 with bed -> ERP configured RM2700
        double childWithBed = PackagePricingCalculator.calculatePassengerPrice(
                detail, "standard", false, adultPrice, "2020-01-01", "2026-10-02", true);
        assertEquals(2700.0, childWithBed, 0.001);

        // 3. Child 2-11 without bed -> ERP configured RM2600
        double childWithoutBed = PackagePricingCalculator.calculatePassengerPrice(
                detail, "standard", false, adultPrice, "2020-01-01", "2026-10-02", false);
        assertEquals(2600.0, childWithoutBed, 0.001);

        // 4. Passenger > 11 years -> Adult price RM2800
        double olderPassenger = PackagePricingCalculator.calculatePassengerPrice(
                detail, "standard", false, adultPrice, "2010-01-01", "2026-10-02", null);
        assertEquals(2800.0, olderPassenger, 0.001);
    }

    @Test
    public void testTourHighPeakPricingRules() {
        com.hafiztraveltours.app.models.PackageDetail detail = new com.hafiztraveltours.app.models.PackageDetail();
        detail.isUmrah = false;
        detail.price = "3300.0";
        detail.childUnder2Price = 500.0;
        detail.child211WithBedPrice = 3200.0;
        detail.child211NoBedPrice = 3100.0;

        double highPeakAdult = 3300.0;
        String departureDate = "2026-10-02";

        // Below 2 -> RM500
        assertEquals(500.0, PackagePricingCalculator.calculatePassengerPrice(
                detail, "high_peak", false, highPeakAdult, "2025-01-01", departureDate, null), 0.001);

        // Age 2-11 + bed -> RM3200
        assertEquals(3200.0, PackagePricingCalculator.calculatePassengerPrice(
                detail, "high_peak", false, highPeakAdult, "2018-05-01", departureDate, true), 0.001);

        // Age 2-11 without bed -> RM3100
        assertEquals(3100.0, PackagePricingCalculator.calculatePassengerPrice(
                detail, "high_peak", false, highPeakAdult, "2018-05-01", departureDate, false), 0.001);

        // Age > 11 -> RM3300
        assertEquals(3300.0, PackagePricingCalculator.calculatePassengerPrice(
                detail, "high_peak", false, highPeakAdult, "2005-01-01", departureDate, null), 0.001);
    }

    @Test
    public void testUmrahChildPricingRules() {
        com.hafiztraveltours.app.models.PackageDetail detail = new com.hafiztraveltours.app.models.PackageDetail();
        detail.isUmrah = true;
        detail.price = "3500.0";
        detail.childUnder2Price = 2000.0;
        detail.child2To4Price = 3200.0;

        double adultUmrah = 3500.0;
        String departureDate = "2026-10-02";

        // 1. Infant < 2 years -> ERP configured RM2000
        assertEquals(2000.0, PackagePricingCalculator.calculatePassengerPrice(
                detail, null, true, adultUmrah, "2025-05-01", departureDate, null), 0.001);

        // 2. Child 2y 1d to 4y -> ERP configured RM3200
        assertEquals(3200.0, PackagePricingCalculator.calculatePassengerPrice(
                detail, null, true, adultUmrah, "2023-01-01", departureDate, null), 0.001);

        // 3. Child > 4y 1d -> Adult room price RM3500
        assertEquals(3500.0, PackagePricingCalculator.calculatePassengerPrice(
                detail, null, true, adultUmrah, "2020-01-01", departureDate, null), 0.001);
    }

    @Test
    public void testUmrahExactDobBoundaries() {
        com.hafiztraveltours.app.models.PackageDetail detail = new com.hafiztraveltours.app.models.PackageDetail();
        detail.isUmrah = true;
        detail.price = "4000.0";
        detail.childUnder2Price = 2000.0;
        detail.child2To4Price = 3700.0;

        double adultUmrah = 4000.0;
        String departureDate = "2026-10-02";

        // 1 day before 2nd birthday: < 2 years -> Infant (RM2000)
        assertEquals(2000.0, PackagePricingCalculator.calculatePassengerPrice(
                detail, null, true, adultUmrah, "2024-10-03", departureDate, null), 0.001);

        // Exactly 2 years on departure: 2 years 0 days -> Child (RM3700)
        assertEquals(3700.0, PackagePricingCalculator.calculatePassengerPrice(
                detail, null, true, adultUmrah, "2024-10-02", departureDate, null), 0.001);

        // Exactly 4 years on departure: <= 4.0 years -> Child (RM3700)
        assertEquals(3700.0, PackagePricingCalculator.calculatePassengerPrice(
                detail, null, true, adultUmrah, "2022-10-02", departureDate, null), 0.001);

        // 4 years + 1 day before departure: > 4.0 years -> Adult room price (RM4000)
        assertEquals(4000.0, PackagePricingCalculator.calculatePassengerPrice(
                detail, null, true, adultUmrah, "2022-10-01", departureDate, null), 0.001);
    }
}
