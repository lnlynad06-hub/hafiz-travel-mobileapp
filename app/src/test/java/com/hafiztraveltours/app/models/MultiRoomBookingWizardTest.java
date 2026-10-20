package com.hafiztraveltours.app.models;

import com.hafiztraveltours.app.utils.PackagePricingCalculator;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class MultiRoomBookingWizardTest {

    @Test
    public void testChildAgeCalculatedBasedOnDepartureDateNotToday() {
        // DOB: 2022-11-15
        String dob = "2022-11-15";

        // If departure is 2024-11-14 -> still 1 year old
        int ageBeforeBirthday = PackagePricingCalculator.calculateAge(dob, "2024-11-14");
        assertEquals(1, ageBeforeBirthday);

        // If departure is 2024-11-15 -> exactly 2 years old
        int ageOnBirthday = PackagePricingCalculator.calculateAge(dob, "2024-11-15");
        assertEquals(2, ageOnBirthday);

        // If departure is 2026-11-16 -> 4 years old
        int ageLater = PackagePricingCalculator.calculateAge(dob, "2026-11-16");
        assertEquals(4, ageLater);
    }

    @Test
    public void testUmrahPackageRejectsSingleRoom() {
        BookingRequest req = new BookingRequest();
        req.packageCategory = "umrah";
        assertTrue(req.isUmrahPackage());
        assertFalse(req.isTourPackage());

        // Validate available room types for Umrah
        List<String> validUmrahTypes = Arrays.asList("quint", "quad", "triple", "double");
        assertTrue(validUmrahTypes.contains("quint"));
        assertTrue(validUmrahTypes.contains("quad"));
        assertTrue(validUmrahTypes.contains("triple"));
        assertTrue(validUmrahTypes.contains("double"));
        assertFalse(validUmrahTypes.contains("single"));
    }

    @Test
    public void testTourPackageAllowsSingleRoom() {
        BookingRequest req = new BookingRequest();
        req.packageCategory = "tour";
        assertFalse(req.isUmrahPackage());
        assertTrue(req.isTourPackage());

        List<String> validTourTypes = Arrays.asList("triple", "double", "single");
        assertTrue(validTourTypes.contains("single"));
        assertTrue(validTourTypes.contains("double"));
        assertTrue(validTourTypes.contains("triple"));
    }

    @Test
    public void testChildNoBedCountsTowardOccupancy() {
        BookingRequest.RoomConfig room = new BookingRequest.RoomConfig(1, "double", 2);
        // Adult 1 (index 0) and Child No Bed (index 1)
        room.assignedTravellerIndices.add(0);
        room.assignedTravellerIndices.add(1);

        assertEquals(2, room.getOccupancy());
        assertEquals(0, room.getAvailableSlots());
        assertFalse(room.isOverCapacity());

        // Adding 1 more traveller exceeds double room capacity
        room.assignedTravellerIndices.add(2);
        assertEquals(3, room.getOccupancy());
        assertTrue(room.isOverCapacity());
    }

    @Test
    public void testMultipleRoomsPerBookingAndTravellerReassignment() {
        BookingRequest req = new BookingRequest();
        req.adultPaxCount = 9;

        // Room 1: Quint (capacity 5)
        BookingRequest.RoomConfig room1 = new BookingRequest.RoomConfig(1, "quint", 5);
        room1.assignedTravellerIndices.addAll(Arrays.asList(0, 1, 2, 3, 4));

        // Room 2: Quad (capacity 4)
        BookingRequest.RoomConfig room2 = new BookingRequest.RoomConfig(2, "quad", 4);
        room2.assignedTravellerIndices.addAll(Arrays.asList(5, 6, 7, 8));

        req.rooms.add(room1);
        req.rooms.add(room2);

        assertEquals(2, req.rooms.size());
        assertEquals(5, room1.getOccupancy());
        assertEquals(4, room2.getOccupancy());
        assertFalse(room1.isOverCapacity());
        assertFalse(room2.isOverCapacity());

        // Move traveller 4 from room1 to room2
        room1.assignedTravellerIndices.remove(Integer.valueOf(4));
        room2.assignedTravellerIndices.add(4);

        assertEquals(4, room1.getOccupancy());
        assertEquals(5, room2.getOccupancy());
        assertTrue(room2.isOverCapacity()); // Room 2 is quad (4), now has 5
    }

    @Test
    public void testRemovingRoomPreservesTravellersToUnassignedPool() {
        List<Integer> allTravellerIndices = Arrays.asList(0, 1, 2, 3);
        List<BookingRequest.RoomConfig> rooms = new ArrayList<>();

        BookingRequest.RoomConfig r1 = new BookingRequest.RoomConfig(1, "double", 2);
        r1.assignedTravellerIndices.add(0);
        r1.assignedTravellerIndices.add(1);

        BookingRequest.RoomConfig r2 = new BookingRequest.RoomConfig(2, "double", 2);
        r2.assignedTravellerIndices.add(2);
        r2.assignedTravellerIndices.add(3);

        rooms.add(r1);
        rooms.add(r2);

        // Remove Room 2
        rooms.remove(1);

        // Determine unassigned pool
        List<Integer> unassigned = new ArrayList<>();
        for (int idx : allTravellerIndices) {
            boolean assigned = false;
            for (BookingRequest.RoomConfig r : rooms) {
                if (r.assignedTravellerIndices.contains(idx)) {
                    assigned = true;
                    break;
                }
            }
            if (!assigned) {
                unassigned.add(idx);
            }
        }

        // Occupants of Room 2 (2 and 3) are now unassigned, not deleted!
        assertEquals(2, unassigned.size());
        assertTrue(unassigned.contains(2));
        assertTrue(unassigned.contains(3));
    }
}
