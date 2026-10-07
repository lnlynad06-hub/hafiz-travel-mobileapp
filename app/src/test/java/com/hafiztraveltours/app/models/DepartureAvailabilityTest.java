package com.hafiztraveltours.app.models;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for departure capacity and real-time seat availability calculation.
 */
public class DepartureAvailabilityTest {

    @Test
    public void testDepartureOptionSeatAvailability() {
        // Case 1: 40 total / 0 booked -> 40 available, not full
        PackageDetail.DepartureOption option1 = new PackageDetail.DepartureOption(
                "1", "2027-02-15", "2027-02-27", "15 Feb 2027", 40, 0, 40, false
        );
        assertEquals(40, option1.getTotalSeatsCount());
        assertEquals(40, option1.getAvailableSeatsCount());
        assertFalse(option1.isFullyBooked());

        // Case 2: 40 total / 1 booked -> 39 available, not full
        PackageDetail.DepartureOption option2 = new PackageDetail.DepartureOption(
                "2", "2027-02-15", "2027-02-27", "15 Feb 2027", 40, 1, 39, false
        );
        assertEquals(40, option2.getTotalSeatsCount());
        assertEquals(39, option2.getAvailableSeatsCount());
        assertFalse(option2.isFullyBooked());

        // Case 3: 40 total / 40 booked -> 0 available, fully booked
        PackageDetail.DepartureOption option3 = new PackageDetail.DepartureOption(
                "3", "2027-02-15", "2027-02-27", "15 Feb 2027", 40, 40, 0, true
        );
        assertEquals(40, option3.getTotalSeatsCount());
        assertEquals(0, option3.getAvailableSeatsCount());
        assertTrue(option3.isFullyBooked());
    }

    @Test
    public void testDepartureOptionFallbackDerivation() {
        // When seatsAvailable is not provided (null) but totalSeats is 40 and seatsBooked is 10
        PackageDetail.DepartureOption option = new PackageDetail.DepartureOption(
                "4", "2027-02-15", "2027-02-27", "15 Feb 2027", 40, 10, null, false
        );
        assertEquals(40, option.getTotalSeatsCount());
        assertEquals(30, option.getAvailableSeatsCount());
        assertFalse(option.isFullyBooked());
    }

    @Test
    public void testFromUmrahPackageMapping() {
        UmrahPackage pkg = new UmrahPackage();
        pkg.id = "pkg-1";
        pkg.name = "Umrah Premium";
        pkg.price = "7500";

        List<UmrahPackage.DepartureItem> items = new ArrayList<>();
        UmrahPackage.DepartureItem item = new UmrahPackage.DepartureItem();
        item.id = "101";
        item.departureDate = "2027-03-01";
        item.totalSeats = 40;
        item.seatsBooked = 5;
        item.seatsAvailable = 35;
        item.isFull = false;
        items.add(item);

        pkg.departures = items;

        PackageDetail detail = PackageDetail.fromUmrahPackage(pkg);
        assertEquals(1, detail.availableDepartures.size());

        PackageDetail.DepartureOption option = detail.availableDepartures.get(0);
        assertEquals("101", option.id);
        assertEquals(40, option.getTotalSeatsCount());
        assertEquals(35, option.getAvailableSeatsCount());
        assertEquals(Integer.valueOf(5), option.seatsBooked);
        assertFalse(option.isFullyBooked());
    }
}
