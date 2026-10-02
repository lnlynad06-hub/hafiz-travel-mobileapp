package com.hafiztraveltours.app.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PackagePricingCalculatorTest {

    @Test
    public void testTourChildPricingRules() {
        double adultPrice = 2800.0;
        String departureDate = "2026-10-02";

        // 1. Infant < 2 years (e.g. born 2025-05-01 -> ~1.4 years old) -> RM500 fixed
        double infantPrice = PackagePricingCalculator.calculatePassengerPrice(
                false, adultPrice, "2025-05-01", departureDate, null);
        assertEquals(500.0, infantPrice, 0.001);

        // 2. Child 2-11 with bed (e.g. born 2020-01-01 -> 6 years old) -> Adult - RM100 = RM2700
        double childWithBed = PackagePricingCalculator.calculatePassengerPrice(
                false, adultPrice, "2020-01-01", departureDate, true);
        assertEquals(2700.0, childWithBed, 0.001);

        // 3. Child 2-11 without bed -> Adult - RM200 = RM2600
        double childWithoutBed = PackagePricingCalculator.calculatePassengerPrice(
                false, adultPrice, "2020-01-01", departureDate, false);
        assertEquals(2600.0, childWithoutBed, 0.001);

        // 4. Passenger > 11 years (e.g. born 2010-01-01 -> 16 years old) -> Adult price = RM2800
        double olderPassenger = PackagePricingCalculator.calculatePassengerPrice(
                false, adultPrice, "2010-01-01", departureDate, null);
        assertEquals(2800.0, olderPassenger, 0.001);
    }

    @Test
    public void testTourHighPeakPricingRules() {
        double highPeakAdult = 3300.0;
        String departureDate = "2026-10-02";

        // Below 2 -> RM500
        assertEquals(500.0, PackagePricingCalculator.calculatePassengerPrice(
                false, highPeakAdult, "2025-01-01", departureDate, null), 0.001);

        // Age 2-11 + bed -> RM3200
        assertEquals(3200.0, PackagePricingCalculator.calculatePassengerPrice(
                false, highPeakAdult, "2018-05-01", departureDate, true), 0.001);

        // Age 2-11 without bed -> RM3100
        assertEquals(3100.0, PackagePricingCalculator.calculatePassengerPrice(
                false, highPeakAdult, "2018-05-01", departureDate, false), 0.001);

        // Age > 11 -> RM3300
        assertEquals(3300.0, PackagePricingCalculator.calculatePassengerPrice(
                false, highPeakAdult, "2005-01-01", departureDate, null), 0.001);
    }

    @Test
    public void testUmrahChildPricingRules() {
        double adultUmrah = 3500.0;
        String departureDate = "2026-10-02";

        // 1. Infant < 2 years (born 2025-05-01) -> Fixed RM2000
        assertEquals(2000.0, PackagePricingCalculator.calculatePassengerPrice(
                true, adultUmrah, "2025-05-01", departureDate, null), 0.001);

        // 2. Child 2y 1d to 4y (born 2023-01-01 -> 3.75 years old) -> Adult - RM300 = RM3200
        assertEquals(3200.0, PackagePricingCalculator.calculatePassengerPrice(
                true, adultUmrah, "2023-01-01", departureDate, null), 0.001);

        // 3. Child > 4y 1d (born 2020-01-01 -> 6.75 years old) -> Adult = RM3500
        assertEquals(3500.0, PackagePricingCalculator.calculatePassengerPrice(
                true, adultUmrah, "2020-01-01", departureDate, null), 0.001);
    }

    @Test
    public void testUmrahExactDobBoundaries() {
        double adultUmrah = 4000.0;
        String departureDate = "2026-10-02";

        // 1 day before 2nd birthday: 2024-10-03 -> < 2 years -> Infant (RM2000)
        assertEquals(2000.0, PackagePricingCalculator.calculatePassengerPrice(
                true, adultUmrah, "2024-10-03", departureDate, null), 0.001);

        // Exactly 2 years on departure: 2024-10-02 -> 2 years 0 days -> Child (Adult - RM300 = RM3700)
        assertEquals(3700.0, PackagePricingCalculator.calculatePassengerPrice(
                true, adultUmrah, "2024-10-02", departureDate, null), 0.001);

        // Exactly 4 years on departure: 2022-10-02 -> <= 4.0 years -> Child (Adult - RM300 = RM3700)
        assertEquals(3700.0, PackagePricingCalculator.calculatePassengerPrice(
                true, adultUmrah, "2022-10-02", departureDate, null), 0.001);

        // 4 years + 1 day before departure: 2022-10-01 -> > 4.0 years -> Adult (RM4000)
        assertEquals(4000.0, PackagePricingCalculator.calculatePassengerPrice(
                true, adultUmrah, "2022-10-01", departureDate, null), 0.001);
    }
}
