package com.hafiztraveltours.app.utils;

import com.hafiztraveltours.app.models.CreateCustomTravelRequest;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class CustomTravelHelperTest {

    @Test
    public void testChildAgeCalculatedBasedOnDepartureDate() {
        // Child born 2018-05-10
        // Departure date 2026-06-15 -> Age should be 8
        int age = CustomTravelHelper.calculateChildAge("2018-05-10", "2026-06-15");
        assertEquals(8, age);

        // Departure date 2026-04-10 (before birthday in 2026) -> Age should be 7
        int ageBeforeBirthday = CustomTravelHelper.calculateChildAge("2018-05-10", "2026-04-10");
        assertEquals(7, ageBeforeBirthday);

        // Invalid or empty returns 0
        assertEquals(0, CustomTravelHelper.calculateChildAge(null, "2026-06-15"));
        assertEquals(0, CustomTravelHelper.calculateChildAge("invalid", "2026-06-15"));
    }

    @Test
    public void testUmrahProhibitsSingleRoom() {
        assertFalse("Umrah must never offer Single room",
                CustomTravelHelper.isRoomCategoryAllowed("umrah", "single"));

        assertTrue(CustomTravelHelper.isRoomCategoryAllowed("umrah", "double"));
        assertTrue(CustomTravelHelper.isRoomCategoryAllowed("umrah", "triple"));
        assertTrue(CustomTravelHelper.isRoomCategoryAllowed("umrah", "quad"));
        assertTrue(CustomTravelHelper.isRoomCategoryAllowed("umrah", "quint"));
    }

    @Test
    public void testTourAllowedRoomCategories() {
        assertTrue(CustomTravelHelper.isRoomCategoryAllowed("tour", "single"));
        assertTrue(CustomTravelHelper.isRoomCategoryAllowed("tour", "double"));
        assertTrue(CustomTravelHelper.isRoomCategoryAllowed("tour", "triple"));
        assertFalse(CustomTravelHelper.isRoomCategoryAllowed("tour", "quad"));
        assertFalse(CustomTravelHelper.isRoomCategoryAllowed("tour", "quint"));
    }

    @Test
    public void testGenderSeparationPreservesSelectedRoomCategoryWhenFamilyOff() {
        // Customer selects Quint, with 2 male and 2 female, Family OFF.
        // Expected:
        // - Quint Male — 2/5
        // - Quint Female — 2/5
        // (Must NOT silently convert these into Double rooms)
        List<CreateCustomTravelRequest.RoomArrangement> rooms = CustomTravelHelper.generateRoomArrangements(
                "umrah",
                "quint",
                2,
                2,
                null,
                false // Family OFF
        );

        assertEquals(2, rooms.size());

        CreateCustomTravelRequest.RoomArrangement maleRoom = rooms.get(0);
        assertEquals("quint", maleRoom.roomType);
        assertEquals("male", maleRoom.gender);
        assertEquals(5, maleRoom.capacity);
        assertEquals(2, maleRoom.paxCount);
        assertTrue(maleRoom.description.contains("Quint Male"));
        assertTrue(maleRoom.description.contains("2/5"));

        CreateCustomTravelRequest.RoomArrangement femaleRoom = rooms.get(1);
        assertEquals("quint", femaleRoom.roomType);
        assertEquals("female", femaleRoom.gender);
        assertEquals(5, femaleRoom.capacity);
        assertEquals(2, femaleRoom.paxCount);
        assertTrue(femaleRoom.description.contains("Quint Female"));
        assertTrue(femaleRoom.description.contains("2/5"));
    }

    @Test
    public void testFamilyRoomCombinedWhenFamilyOn() {
        // Customer selects Quad, 2 adults + 2 children, Family ON.
        List<CreateCustomTravelRequest.ChildDetail> children = new ArrayList<>();
        children.add(new CreateCustomTravelRequest.ChildDetail("2018-01-01", 8, "male"));
        children.add(new CreateCustomTravelRequest.ChildDetail("2020-01-01", 6, "female"));

        List<CreateCustomTravelRequest.RoomArrangement> rooms = CustomTravelHelper.generateRoomArrangements(
                "umrah",
                "quad",
                1,
                1,
                children,
                true // Family ON
        );

        assertEquals(1, rooms.size());
        CreateCustomTravelRequest.RoomArrangement familyRoom = rooms.get(0);
        assertEquals("quad", familyRoom.roomType);
        assertEquals("family", familyRoom.gender);
        assertEquals(4, familyRoom.capacity);
        assertEquals(4, familyRoom.paxCount);
        assertTrue(familyRoom.description.contains("Quad Family"));
        assertTrue(familyRoom.description.contains("4/4"));
    }

    @Test
    public void testMultipleRoomsGeneratedForLargeGroup() {
        // 7 males in Quad category, Family OFF
        List<CreateCustomTravelRequest.RoomArrangement> rooms = CustomTravelHelper.generateRoomArrangements(
                "umrah",
                "quad",
                7,
                0,
                null,
                false
        );

        assertEquals(2, rooms.size());
        assertEquals(4, rooms.get(0).paxCount);
        assertEquals(4, rooms.get(0).capacity);
        assertEquals(3, rooms.get(1).paxCount);
        assertEquals(4, rooms.get(1).capacity);
    }
}
