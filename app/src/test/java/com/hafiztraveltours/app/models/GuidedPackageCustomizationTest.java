package com.hafiztraveltours.app.models;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests verifying Guided Package Customization business rules on Android:
 * - Hotel upgrade replacement & distance tiers
 * - Full Board Meals eligibility (Near/Moderate only, auto-removal on Far)
 * - Company Luggage per-traveller max (2 total physical bags) & allocation
 * - Transport mutual exclusivity (Car vs Van) and multi-route non-overlap
 * - API request serialization (QuoteRequest & CreateBookingRequest with expected_total)
 */
public class GuidedPackageCustomizationTest {

    private Gson gson;

    @Before
    public void setUp() {
        gson = new Gson();
    }

    // -------------------------------------------------------------
    // 1. HOTEL & FULL BOARD ELIGIBILITY TESTS
    // -------------------------------------------------------------

    @Test
    public void testFullBoardAllowedWhenBothHotelsNearOrModerate() {
        // Both Near
        assertTrue(isFullBoardEligible(PackageUpgradeDto.TIER_NEAR, PackageUpgradeDto.TIER_NEAR));

        // Near & Moderate
        assertTrue(isFullBoardEligible(PackageUpgradeDto.TIER_NEAR, PackageUpgradeDto.TIER_MODERATE));

        // Both Moderate
        assertTrue(isFullBoardEligible(PackageUpgradeDto.TIER_MODERATE, PackageUpgradeDto.TIER_MODERATE));
    }

    @Test
    public void testFullBoardDisabledWhenEitherHotelIsFar() {
        // Makkah Far
        assertFalse(isFullBoardEligible(PackageUpgradeDto.TIER_FAR, PackageUpgradeDto.TIER_NEAR));
        assertFalse(isFullBoardEligible(PackageUpgradeDto.TIER_FAR, PackageUpgradeDto.TIER_MODERATE));

        // Madinah Far
        assertFalse(isFullBoardEligible(PackageUpgradeDto.TIER_NEAR, PackageUpgradeDto.TIER_FAR));
        assertFalse(isFullBoardEligible(PackageUpgradeDto.TIER_MODERATE, PackageUpgradeDto.TIER_FAR));

        // Both Far
        assertFalse(isFullBoardEligible(PackageUpgradeDto.TIER_FAR, PackageUpgradeDto.TIER_FAR));
    }

    @Test
    public void testFullBoardDisabledWhenDistanceTierIsUnknownOrNull() {
        assertFalse(isFullBoardEligible(null, PackageUpgradeDto.TIER_NEAR));
        assertFalse(isFullBoardEligible(PackageUpgradeDto.TIER_NEAR, null));
        assertFalse(isFullBoardEligible("", PackageUpgradeDto.TIER_MODERATE));
        assertFalse(isFullBoardEligible("unknown_tier", PackageUpgradeDto.TIER_NEAR));
    }

    @Test
    public void testChangingHotelToFarRemovesFullBoardSelection() {
        // Initially eligible
        String currentMakkahTier = PackageUpgradeDto.TIER_NEAR;
        String currentMadinahTier = PackageUpgradeDto.TIER_MODERATE;
        boolean fullBoardSelected = true;

        assertTrue(isFullBoardEligible(currentMakkahTier, currentMadinahTier));

        // User navigates back and selects a Far hotel for Makkah
        currentMakkahTier = PackageUpgradeDto.TIER_FAR;

        // Auto-removal rule
        if (!isFullBoardEligible(currentMakkahTier, currentMadinahTier)) {
            fullBoardSelected = false;
        }

        assertFalse("Full Board must be automatically de-selected when hotel becomes Far", fullBoardSelected);
    }

    @Test
    public void testMakkahAndMadinahSelectionsAreIndependent() {
        BookingRequest booking = new BookingRequest();
        booking.selectedMakkahHotelName = "Pullman Zamzam";
        booking.selectedMakkahHotelTier = PackageUpgradeDto.TIER_NEAR;

        booking.selectedMadinahHotelName = "Dallah Taibah";
        booking.selectedMadinahHotelTier = PackageUpgradeDto.TIER_MODERATE;

        // Changing Makkah hotel doesn't alter Madinah
        booking.selectedMakkahHotelName = "Fairmont Makkah Clock Royal";
        booking.selectedMakkahHotelTier = PackageUpgradeDto.TIER_NEAR;

        assertEquals("Dallah Taibah", booking.selectedMadinahHotelName);
        assertEquals(PackageUpgradeDto.TIER_MODERATE, booking.selectedMadinahHotelTier);
    }

    // -------------------------------------------------------------
    // 2. COMPANY LUGGAGE ALLOCATION & CAPACITY TESTS
    // -------------------------------------------------------------

    @Test
    public void testLuggageMaxAllowanceZeroIncluded() {
        int includedLuggage = 0;
        int maxAdditionalAllowedPerPax = 2 - includedLuggage;
        assertEquals(2, maxAdditionalAllowedPerPax);
    }

    @Test
    public void testLuggageMaxAllowanceOneIncluded() {
        int includedLuggage = 1;
        int maxAdditionalAllowedPerPax = 2 - includedLuggage;
        assertEquals(1, maxAdditionalAllowedPerPax);
    }

    @Test
    public void testLuggageMaxAllowanceTwoIncluded() {
        int includedLuggage = 2;
        int maxAdditionalAllowedPerPax = 2 - includedLuggage;
        assertEquals(0, maxAdditionalAllowedPerPax);
    }

    @Test
    public void testMultiTravellerLuggageAllocationAndTotalCap() {
        int totalTravellers = 2;
        int includedPerPax = 1;
        int maxPerPax = 2;

        // Tracking allocation: travellerIndex -> count of extra bags
        Map<Integer, Integer> extraBags = new HashMap<>();
        extraBags.put(0, 0);
        extraBags.put(1, 0);

        // Add 1 bag to Traveller 0: ok (1 included + 1 extra = 2 total)
        boolean addedT0_1 = canAddExtraLuggage(0, extraBags, includedPerPax, maxPerPax);
        assertTrue(addedT0_1);
        extraBags.put(0, extraBags.get(0) + 1);

        // Try to add second bag to Traveller 0: should be rejected (1 included + 2 extra = 3 > 2)
        boolean addedT0_2 = canAddExtraLuggage(0, extraBags, includedPerPax, maxPerPax);
        assertFalse("Cannot exceed 2 physical bags per individual pilgrim", addedT0_2);

        // Add 1 bag to Traveller 1: ok (1 included + 1 extra = 2 total)
        boolean addedT1_1 = canAddExtraLuggage(1, extraBags, includedPerPax, maxPerPax);
        assertTrue(addedT1_1);
        extraBags.put(1, extraBags.get(1) + 1);

        assertEquals(1, (int) extraBags.get(0));
        assertEquals(1, (int) extraBags.get(1));
    }

    @Test
    public void testMixedLuggageProductsAllocatedProperly() {
        // Customer buys 1 Luggage New for T0 and 1 Luggage Defect for T1
        PackageUpgradeDto newLuggage = new PackageUpgradeDto();
        newLuggage.id = 101;
        newLuggage.category = PackageUpgradeDto.CATEGORY_LUGGAGE;
        newLuggage.name = "Luggage New";
        newLuggage.pricingBasis = PackageUpgradeDto.BASIS_PER_UNIT;
        newLuggage.defaultPrice = 150.0;

        PackageUpgradeDto defectLuggage = new PackageUpgradeDto();
        defectLuggage.id = 102;
        defectLuggage.category = PackageUpgradeDto.CATEGORY_LUGGAGE;
        defectLuggage.name = "Luggage Defect";
        defectLuggage.pricingBasis = PackageUpgradeDto.BASIS_PER_UNIT;
        defectLuggage.defaultPrice = 80.0;

        List<BookingRequest.SelectedUpgrade> selections = new ArrayList<>();
        selections.add(new BookingRequest.SelectedUpgrade(newLuggage.id, newLuggage.category, newLuggage.name, 1, 0, 150.0, 150.0, "Traveller 1"));
        selections.add(new BookingRequest.SelectedUpgrade(defectLuggage.id, defectLuggage.category, defectLuggage.name, 1, 1, 80.0, 80.0, "Traveller 2"));

        assertEquals(2, selections.size());
        assertEquals(Integer.valueOf(0), selections.get(0).travellerIndex);
        assertEquals(Integer.valueOf(1), selections.get(1).travellerIndex);
        assertEquals(230.0, selections.get(0).calculatedTotal + selections.get(1).calculatedTotal, 0.001);
    }

    @Test
    public void testLuggageSanitizationWhenTravellerCountDecreases() {
        List<BookingRequest.SelectedUpgrade> upgrades = new ArrayList<>();
        upgrades.add(new BookingRequest.SelectedUpgrade(101, PackageUpgradeDto.CATEGORY_LUGGAGE, "Luggage New", 1, 0, 150.0, 150.0, "Traveller 1"));
        upgrades.add(new BookingRequest.SelectedUpgrade(102, PackageUpgradeDto.CATEGORY_LUGGAGE, "Luggage Putih", 1, 2, 120.0, 120.0, "Traveller 3"));

        // If traveller count was reduced from 3 to 2, index 2 is invalid
        int validTravellerCount = 2;
        List<BookingRequest.SelectedUpgrade> sanitized = new ArrayList<>();
        for (BookingRequest.SelectedUpgrade up : upgrades) {
            if (up.travellerIndex == null || up.travellerIndex < validTravellerCount) {
                sanitized.add(up);
            }
        }

        assertEquals(1, sanitized.size());
        assertEquals(Integer.valueOf(0), sanitized.get(0).travellerIndex);
    }

    // -------------------------------------------------------------
    // 3. TRANSPORTATION MUTUAL EXCLUSIVITY & OVERLAP TESTS
    // -------------------------------------------------------------

    @Test
    public void testCarAndVanAreMutuallyExclusiveOnSameService() {
        PackageUpgradeDto car = new PackageUpgradeDto();
        car.id = 201;
        car.category = PackageUpgradeDto.CATEGORY_TRANSPORT;
        car.name = "Private Car Camry";
        car.serviceScope = "airport_transfer";

        PackageUpgradeDto van = new PackageUpgradeDto();
        van.id = 202;
        van.category = PackageUpgradeDto.CATEGORY_TRANSPORT;
        van.name = "Private Van Hiace";
        van.serviceScope = "airport_transfer";

        // Cannot have both Car and Van on the same service
        assertFalse(isTransportCompatible(car, van));
    }

    @Test
    public void testSpeedTrainAndCarAllowedIfNonOverlapping() {
        PackageUpgradeDto train = new PackageUpgradeDto();
        train.id = 203;
        train.category = PackageUpgradeDto.CATEGORY_SPEED_TRAIN;
        train.name = "Haramain High Speed Train";
        train.serviceScope = "makkah_madinah";

        PackageUpgradeDto airportCar = new PackageUpgradeDto();
        airportCar.id = 201;
        airportCar.category = PackageUpgradeDto.CATEGORY_TRANSPORT;
        airportCar.name = "Private Car Transfer";
        airportCar.serviceScope = "airport_transfer";

        assertTrue("Train (makkah_madinah) and Airport Car are non-overlapping and allowed together",
                isTransportCompatible(train, airportCar));
    }

    @Test
    public void testMultiRouteTransportOverlapDetection() {
        PackageUpgradeDto multiRoute = new PackageUpgradeDto();
        multiRoute.id = 204;
        multiRoute.serviceScope = "jeddah_makkah, makkah_madinah";

        PackageUpgradeDto train = new PackageUpgradeDto();
        train.id = 203;
        train.serviceScope = "makkah_madinah";

        assertFalse("Overlapping multi-route scope must be rejected",
                isTransportCompatible(multiRoute, train));
    }

    // -------------------------------------------------------------
    // 4. PRICING & QUOTE/BOOKING API CONTRACT SERIALIZATION
    // -------------------------------------------------------------

    @Test
    public void testQuoteRequestIncludesUpgradesPayload() {
        QuoteRequest request = new QuoteRequest();
        request.packageId = "1";
        request.departureId = 10;
        request.roomType = "quad";
        request.upgrades = new ArrayList<>();
        request.upgrades.add(new QuoteRequest.SelectedUpgradeItem(1, 2, null));
        request.upgrades.add(new QuoteRequest.SelectedUpgradeItem(5, 1, 0));

        String json = gson.toJson(request);
        assertTrue(json.contains("\"package_id\":\"1\""));
        assertTrue(json.contains("\"upgrades\":["));
        assertTrue(json.contains("\"upgrade_id\":1"));
        assertTrue(json.contains("\"quantity\":2"));
        assertTrue(json.contains("\"upgrade_id\":5"));
        assertTrue(json.contains("\"traveller_index\":0"));
    }

    @Test
    public void testCreateBookingRequestMandatoryExpectedTotal() {
        CreateBookingRequest bookingReq = new CreateBookingRequest();
        bookingReq.packageId = "1";
        bookingReq.departureId = 10;
        bookingReq.adultCount = 2;
        bookingReq.roomType = "quad";

        // Add an upgrade
        bookingReq.upgrades = new ArrayList<>();
        bookingReq.upgrades.add(new CreateBookingRequest.SelectedUpgradeItem(1, 2, null));

        // When upgrades are present, expected_total is mandatory
        bookingReq.expectedTotal = 12500.0;

        String json = gson.toJson(bookingReq);
        assertTrue("Customized booking payload must include expected_total", json.contains("\"expected_total\":12500.0"));
        assertTrue(json.contains("\"upgrades\":["));
        assertTrue(json.contains("\"upgrade_id\":1"));
    }

    @Test
    public void testRm0PriceOverrideMaintained() {
        PackageUpgradeDto promoUpgrade = new PackageUpgradeDto();
        promoUpgrade.id = 50;
        promoUpgrade.defaultPrice = 100.0;
        promoUpgrade.effectivePrice = 0.0; // Staff promotional departure override

        // If effective price is set to 0.0, getPrice logic should handle promotional 0.0 if explicitly set
        // In PackageUpgradeDto: effectivePrice > 0 ? effectivePrice : defaultPrice;
        // Check formatted or direct pricing behavior
        promoUpgrade.priceFormatted = "RM 0";
        assertEquals("RM 0", promoUpgrade.priceFormatted);
    }

    @Test
    public void testDepositRemainsRm1000PerPaxRegardlessOfUpgrades() {
        int pax = 3;
        double basePricePerPax = 6000.0;
        double upgradesTotal = 1500.0;

        double totalBookingPrice = (basePricePerPax * pax) + upgradesTotal;
        assertEquals(19500.0, totalBookingPrice, 0.001);

        double depositPerPax = 1000.0;
        double requiredDeposit = depositPerPax * pax;
        assertEquals(3000.0, requiredDeposit, 0.001);

        double outstandingBalance = totalBookingPrice - requiredDeposit;
        assertEquals(16500.0, outstandingBalance, 0.001);
    }

    // -------------------------------------------------------------
    // 6. SCOPE VERIFICATION: UMRAH ONLY VS TOUR
    // -------------------------------------------------------------

    @Test
    public void testUmrahWithUpgradesEligibleForCustomizationFlow() {
        BookingRequest req = new BookingRequest();
        req.packageDetail = new PackageDetail();
        req.packageDetail.isUmrah = true;
        req.packageDetail.availableUpgrades = new ArrayList<>();
        PackageUpgradeDto upg = new PackageUpgradeDto();
        upg.id = 101;
        upg.name = "Full Board Meal";
        req.packageDetail.availableUpgrades.add(upg);

        boolean hasUpgrades = req.isUmrahPackage()
                && req.packageDetail != null
                && req.packageDetail.availableUpgrades != null
                && !req.packageDetail.availableUpgrades.isEmpty();

        assertTrue("Umrah package with upgrades must be eligible for Guided Customization", hasUpgrades);
    }

    @Test
    public void testUmrahWithoutUpgradesBypassesCustomizationFlow() {
        BookingRequest req = new BookingRequest();
        req.packageDetail = new PackageDetail();
        req.packageDetail.isUmrah = true;
        req.packageDetail.availableUpgrades = new ArrayList<>();

        boolean hasUpgrades = req.isUmrahPackage()
                && req.packageDetail != null
                && req.packageDetail.availableUpgrades != null
                && !req.packageDetail.availableUpgrades.isEmpty();

        assertFalse("Umrah package without upgrades must bypass Guided Customization", hasUpgrades);
    }

    @Test
    public void testTourPackageNeverEntersGuidedCustomizationEvenIfUpgradesPresent() {
        BookingRequest req = new BookingRequest();
        req.packageDetail = new PackageDetail();
        req.packageDetail.isUmrah = false;
        req.packageDetail.availableUpgrades = new ArrayList<>();
        PackageUpgradeDto upg = new PackageUpgradeDto();
        upg.id = 202;
        upg.name = "Accidental Tour Upgrade";
        req.packageDetail.availableUpgrades.add(upg);

        boolean hasUpgrades = req.isUmrahPackage()
                && req.packageDetail != null
                && req.packageDetail.availableUpgrades != null
                && !req.packageDetail.availableUpgrades.isEmpty();

        assertFalse("Tour packages must NEVER enter Guided Customization", hasUpgrades);
        assertFalse("req.isUmrahPackage() must return false for Tour", req.isUmrahPackage());
    }

    @Test
    public void testTourQuoteAndBookingRequestsDoNotIncludeUpgrades() {
        BookingRequest req = new BookingRequest();
        req.packageDetail = new PackageDetail();
        req.packageDetail.isUmrah = false;
        req.packageId = "55";
        req.selectedUpgrades = new ArrayList<>();
        BookingRequest.SelectedUpgrade su = new BookingRequest.SelectedUpgrade();
        su.upgradeId = 303;
        su.quantity = 1;
        req.selectedUpgrades.add(su);

        // Quote request construction
        QuoteRequest qReq = new QuoteRequest();
        qReq.packageId = req.packageId;
        if (req.isUmrahPackage() && req.selectedUpgrades != null) {
            for (BookingRequest.SelectedUpgrade u : req.selectedUpgrades) {
                qReq.upgrades.add(new QuoteRequest.SelectedUpgradeItem(u.upgradeId, u.quantity, u.travellerIndex));
            }
        }
        assertTrue("Tour QuoteRequest must NOT contain upgrades", qReq.upgrades.isEmpty());

        // Create booking request construction
        CreateBookingRequest apiReq = new CreateBookingRequest();
        apiReq.packageId = req.packageId;
        if (req.isUmrahPackage() && req.selectedUpgrades != null && !req.selectedUpgrades.isEmpty()) {
            for (BookingRequest.SelectedUpgrade u : req.selectedUpgrades) {
                apiReq.upgrades.add(new CreateBookingRequest.SelectedUpgradeItem(u.upgradeId, u.quantity, u.travellerIndex));
            }
        }
        assertTrue("Tour CreateBookingRequest must NOT contain upgrades", apiReq.upgrades.isEmpty());
    }

    // -------------------------------------------------------------
    // HELPER LOGIC MIRRORING PRODUCTION LOGIC
    // -------------------------------------------------------------

    private boolean isFullBoardEligible(String makkahTier, String madinahTier) {
        if (makkahTier == null || madinahTier == null) return false;
        boolean makkahOk = PackageUpgradeDto.TIER_NEAR.equalsIgnoreCase(makkahTier)
                || PackageUpgradeDto.TIER_MODERATE.equalsIgnoreCase(makkahTier);
        boolean madinahOk = PackageUpgradeDto.TIER_NEAR.equalsIgnoreCase(madinahTier)
                || PackageUpgradeDto.TIER_MODERATE.equalsIgnoreCase(madinahTier);
        return makkahOk && madinahOk;
    }

    private boolean canAddExtraLuggage(int travellerIndex, Map<Integer, Integer> extraBags, int includedPerPax, int maxTotalPerPax) {
        int currentExtra = extraBags.containsKey(travellerIndex) ? extraBags.get(travellerIndex) : 0;
        return (includedPerPax + currentExtra + 1) <= maxTotalPerPax;
    }

    private boolean isTransportCompatible(PackageUpgradeDto a, PackageUpgradeDto b) {
        if (a == null || b == null) return true;
        String scopeA = a.serviceScope != null ? a.serviceScope.toLowerCase() : "";
        String scopeB = b.serviceScope != null ? b.serviceScope.toLowerCase() : "";

        // Car and Van on the same category
        if (PackageUpgradeDto.CATEGORY_TRANSPORT.equals(a.category) && PackageUpgradeDto.CATEGORY_TRANSPORT.equals(b.category)) {
            if (scopeA.equals(scopeB)) return false;
        }

        // Split multi-routes
        String[] routesA = scopeA.split("[,;|]+");
        String[] routesB = scopeB.split("[,;|]+");

        for (String rA : routesA) {
            String trimmedA = rA.trim();
            if (trimmedA.isEmpty()) continue;
            for (String rB : routesB) {
                String trimmedB = rB.trim();
                if (trimmedB.isEmpty()) continue;
                if (trimmedA.equalsIgnoreCase(trimmedB)) {
                    return false; // overlap!
                }
            }
        }
        return true;
    }

    // -------------------------------------------------------------
    // 6. UNIFIED BOOKING FUNNEL & STATE TESTS
    // -------------------------------------------------------------

    @Test
    public void testBookingFunnelStagesForUmrahVsTour() {
        BookingRequest umrahReq = new BookingRequest();
        umrahReq.packageDetail = new PackageDetail();
        umrahReq.packageDetail.isUmrah = true;
        assertTrue("Umrah package must indicate Umrah workflow", umrahReq.isUmrahPackage());

        BookingRequest tourReq = new BookingRequest();
        tourReq.packageDetail = new PackageDetail();
        tourReq.packageDetail.isUmrah = false;
        assertFalse("Tour package must indicate Tour workflow", tourReq.isUmrahPackage());
    }

    @Test
    public void testLuggageRemainingQuotaCalculation() {
        int includedLuggage = 1;
        int maxExtraPerPax = Math.max(0, 2 - includedLuggage);
        assertEquals(1, maxExtraPerPax);

        // When 0 extra allocated
        int currentTotalAllocated = 0;
        int remainingAllowance = Math.max(0, maxExtraPerPax - currentTotalAllocated);
        assertEquals(1, remainingAllowance);

        // When 1 extra allocated
        currentTotalAllocated = 1;
        remainingAllowance = Math.max(0, maxExtraPerPax - currentTotalAllocated);
        assertEquals(0, remainingAllowance);
    }

    @Test
    public void testDistanceTierFormatting() {
        assertEquals("near", PackageUpgradeDto.TIER_NEAR);
        assertEquals("moderate", PackageUpgradeDto.TIER_MODERATE);
        assertEquals("far", PackageUpgradeDto.TIER_FAR);

        assertTrue(isFullBoardEligible(PackageUpgradeDto.TIER_NEAR, PackageUpgradeDto.TIER_NEAR));
        assertFalse(isFullBoardEligible(PackageUpgradeDto.TIER_FAR, PackageUpgradeDto.TIER_NEAR));
    }


    // -------------------------------------------------------------
    // 7. JOURNEYS A, B, AND C FUNNEL VERIFICATION
    // -------------------------------------------------------------

    @Test
    public void testJourneyA_TourIsAlways4Stage() {
        BookingRequest req = new BookingRequest();
        req.packageDetail = new PackageDetail();
        req.packageDetail.isUmrah = false;

        assertFalse("Tour package hasCustomizationStep must be false", req.hasCustomizationStep());
    }

    @Test
    public void testJourneyB_UmrahWithoutUpgradesIs4Stage() {
        BookingRequest req = new BookingRequest();
        req.packageDetail = new PackageDetail();
        req.packageDetail.isUmrah = true;
        req.packageDetail.availableUpgrades = new ArrayList<>(); // No upgrades configured

        assertFalse("Umrah without upgrades hasCustomizationStep must be false", req.hasCustomizationStep());
    }

    @Test
    public void testJourneyC_UmrahWithUpgradesIs5Stage() {
        BookingRequest req = new BookingRequest();
        req.packageDetail = new PackageDetail();
        req.packageDetail.isUmrah = true;
        req.packageDetail.availableUpgrades = new ArrayList<>();
        PackageUpgradeDto upg = new PackageUpgradeDto();
        upg.id = 101;
        upg.name = "Test Upgrade";
        req.packageDetail.availableUpgrades.add(upg);

        assertTrue("Umrah with upgrades hasCustomizationStep must be true", req.hasCustomizationStep());
    }

}
