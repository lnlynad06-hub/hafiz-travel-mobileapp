package com.hafiztraveltours.app.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class PackagePricingCalculatorTest {

    @Test
    public void testTourChildPricingRulesAndBoundaries() {
        com.hafiztraveltours.app.models.PackageDetail detail = new com.hafiztraveltours.app.models.PackageDetail();
        detail.isUmrah = false;
        detail.price = "3000.0";
        detail.childUnder2Price = 500.0;
        detail.child211WithBedDiscount = 200.0;  // Final: 3000 - 200 = 2800.0
        detail.child211NoBedDiscount = 300.0;    // Final: 3000 - 300 = 2700.0

        double adultPrice = 3000.0;
        String departureDate = "2026-10-02";

        // 1. Tour under 2 years (dob: 2025-05-01) -> Fixed RM500
        double infantPrice = PackagePricingCalculator.calculatePassengerPrice(
                detail, "standard", false, adultPrice, "2025-05-01", departureDate, null);
        assertEquals(500.0, infantPrice, 0.001);

        // 2. Tour exactly 2 years on departure (dob: 2024-10-02) + Bed -> RM2800
        double exact2yWithBed = PackagePricingCalculator.calculatePassengerPrice(
                detail, "standard", false, adultPrice, "2024-10-02", departureDate, true);
        assertEquals(2800.0, exact2yWithBed, 0.001);

        // 3. Tour 2 years 1 day on departure (dob: 2024-10-01) No Bed -> RM2700
        double age2y1dNoBed = PackagePricingCalculator.calculatePassengerPrice(
                detail, "standard", false, adultPrice, "2024-10-01", departureDate, false);
        assertEquals(2700.0, age2y1dNoBed, 0.001);

        // 4. Tour age 10: One day before 11th birthday (dob: 2015-10-03) + Bed -> RM2800
        double dayBefore11yWithBed = PackagePricingCalculator.calculatePassengerPrice(
                detail, "standard", false, adultPrice, "2015-10-03", departureDate, true);
        assertEquals(2800.0, dayBefore11yWithBed, 0.001);

        // 5. Tour exactly 11th birthday on departure (dob: 2015-10-02) -> Full Adult Price RM3000
        double exact11yAdult = PackagePricingCalculator.calculatePassengerPrice(
                detail, "standard", false, adultPrice, "2015-10-02", departureDate, true);
        assertEquals(3000.0, exact11yAdult, 0.001);

        // 6. Tour exactly 12 years on departure (dob: 2014-10-02) -> Full Adult Price RM3000
        double exact12yAdult = PackagePricingCalculator.calculatePassengerPrice(
                detail, "standard", false, adultPrice, "2014-10-02", departureDate, null);
        assertEquals(3000.0, exact12yAdult, 0.001);
    }

    @Test
    public void testTourHighPeakPricingRules() {
        com.hafiztraveltours.app.models.PackageDetail detail = new com.hafiztraveltours.app.models.PackageDetail();
        detail.isUmrah = false;
        detail.price = "3300.0";
        detail.childUnder2Price = 500.0;
        detail.child211WithBedDiscount = 100.0; // 3300 - 100 = 3200
        detail.child211NoBedDiscount = 200.0;   // 3300 - 200 = 3100

        double highPeakAdult = 3300.0;
        String departureDate = "2026-10-02";

        // Below 2 -> RM500
        assertEquals(500.0, PackagePricingCalculator.calculatePassengerPrice(
                detail, "high_peak", false, highPeakAdult, "2025-01-01", departureDate, null), 0.001);

        // Age 2-10 + bed (e.g. 8yo) -> RM3200
        assertEquals(3200.0, PackagePricingCalculator.calculatePassengerPrice(
                detail, "high_peak", false, highPeakAdult, "2018-05-01", departureDate, true), 0.001);

        // Age 2-10 without bed -> RM3100
        assertEquals(3100.0, PackagePricingCalculator.calculatePassengerPrice(
                detail, "high_peak", false, highPeakAdult, "2018-05-01", departureDate, false), 0.001);

        // Age >= 11 (e.g. 11th birthday or older) -> RM3300
        assertEquals(3300.0, PackagePricingCalculator.calculatePassengerPrice(
                detail, "high_peak", false, highPeakAdult, "2015-10-02", departureDate, null), 0.001);
    }

    @Test
    public void testUmrahChildPricingRulesAndBoundaries() {
        com.hafiztraveltours.app.models.PackageDetail detail = new com.hafiztraveltours.app.models.PackageDetail();
        detail.isUmrah = true;
        detail.price = "3500.0";
        detail.childUnder2Price = 2000.0;
        detail.child2To4Discount = 300.0; // Final: 3500 - 300 = 3200.0

        double adultUmrah = 3500.0;
        String departureDate = "2026-10-02";

        // 1. Umrah under 2 years (1 day before 2nd birthday: 2024-10-03) -> Infant (RM2000)
        assertEquals(2000.0, PackagePricingCalculator.calculatePassengerPrice(
                detail, null, true, adultUmrah, "2024-10-03", departureDate, null), 0.001);

        // 2. Umrah exactly 2 years on departure (dob: 2024-10-02) -> Requires staff confirmation
        try {
            PackagePricingCalculator.calculatePassengerPrice(
                    detail, null, true, adultUmrah, "2024-10-02", departureDate, null);
            fail("Expected PricingConfigurationException for exact 2nd birthday boundary");
        } catch (PackagePricingCalculator.PricingConfigurationException e) {
            assertTrue(e.getMessage().contains("pengesahan polisi staf"));
        }

        // 3. Umrah 2 years 1 day on departure (dob: 2024-10-01) -> Child (RM3200)
        assertEquals(3200.0, PackagePricingCalculator.calculatePassengerPrice(
                detail, null, true, adultUmrah, "2024-10-01", departureDate, null), 0.001);

        // 4. Umrah exactly 4 years on departure (dob: 2022-10-02) -> Child (RM3200)
        assertEquals(3200.0, PackagePricingCalculator.calculatePassengerPrice(
                detail, null, true, adultUmrah, "2022-10-02", departureDate, null), 0.001);

        // 5. Umrah 4 years 1 day on departure (dob: 2022-10-01) -> Adult room price (RM3500)
        assertEquals(3500.0, PackagePricingCalculator.calculatePassengerPrice(
                detail, null, true, adultUmrah, "2022-10-01", departureDate, null), 0.001);

        // 6. Umrah 5 years old (dob: 2021-10-02) -> Adult room price (RM3500)
        assertEquals(3500.0, PackagePricingCalculator.calculatePassengerPrice(
                detail, null, true, adultUmrah, "2021-10-02", departureDate, null), 0.001);
    }

    @Test
    public void testPassengerPriceDetailHelper() {
        com.hafiztraveltours.app.models.PackageDetail tourDetail = new com.hafiztraveltours.app.models.PackageDetail();
        tourDetail.isUmrah = false;
        tourDetail.price = "5000.0";
        tourDetail.childUnder2Price = 600.0;
        tourDetail.child211WithBedDiscount = 400.0;
        tourDetail.child211NoBedDiscount = 800.0;

        String dep = "2026-12-01";

        // Missing DOB
        PackagePricingCalculator.PassengerPriceDetail pdNoDob =
                PackagePricingCalculator.resolvePassengerPriceDetail(null, tourDetail, "standard", false, 5000.0, null, dep, null);
        assertEquals("unknown", pdNoDob.category);
        assertFalse(pdNoDob.isExactSecondBirthday);

        // Tour infant < 2
        PackagePricingCalculator.PassengerPriceDetail pdInfant =
                PackagePricingCalculator.resolvePassengerPriceDetail(null, tourDetail, "standard", false, 5000.0, "2025-06-01", dep, null);
        assertEquals("infant", pdInfant.category);
        assertEquals(600.0, pdInfant.amount, 0.001);

        // Tour 2-10 missing bed preference
        PackagePricingCalculator.PassengerPriceDetail pdNoBedPref =
                PackagePricingCalculator.resolvePassengerPriceDetail(null, tourDetail, "standard", false, 5000.0, "2020-01-01", dep, null);
        assertTrue(pdNoBedPref.isBedRequiredButMissing);

        // Tour 2-10 with bed
        PackagePricingCalculator.PassengerPriceDetail pdWithBed =
                PackagePricingCalculator.resolvePassengerPriceDetail(null, tourDetail, "standard", false, 5000.0, "2020-01-01", dep, true);
        assertEquals(4600.0, pdWithBed.amount, 0.001);
        assertEquals(400.0, pdWithBed.discount, 0.001);

        // Tour 11+
        PackagePricingCalculator.PassengerPriceDetail pd11Plus =
                PackagePricingCalculator.resolvePassengerPriceDetail(null, tourDetail, "standard", false, 5000.0, "2015-12-01", dep, true);
        assertEquals("adult", pd11Plus.category);
        assertEquals(5000.0, pd11Plus.amount, 0.001);
    }
}
