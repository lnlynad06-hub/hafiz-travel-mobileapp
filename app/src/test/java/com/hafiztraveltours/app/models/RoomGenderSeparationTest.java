package com.hafiztraveltours.app.models;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class RoomGenderSeparationTest {

    static class MockTraveller {
        int index;
        String name;
        String gender; // "Male" or "Female"

        MockTraveller(int index, String name, String gender) {
            this.index = index;
            this.name = name;
            this.gender = gender;
        }

        boolean isMale() {
            return "Male".equalsIgnoreCase(gender);
        }

        boolean isFemale() {
            return "Female".equalsIgnoreCase(gender);
        }
    }

    private boolean canAssignTravellerToRoom(BookingRequest.RoomConfig room, MockTraveller traveller, boolean isFamilyRoomEnabled) {
        if (room == null || traveller == null) return false;
        // Capacity check
        if (room.getOccupancy() >= room.capacity) return false;

        if (room.isFamilyRoom()) {
            return isFamilyRoomEnabled;
        }

        if (room.isMaleRoom()) {
            return traveller.isMale();
        }

        if (room.isFemaleRoom()) {
            return traveller.isFemale();
        }

        return false;
    }

    private boolean isRoomArrangementValid(List<BookingRequest.RoomConfig> rooms, List<MockTraveller> travellers, boolean isFamilyRoomEnabled) {
        if (rooms == null || rooms.isEmpty()) return false;

        for (BookingRequest.RoomConfig r : rooms) {
            if (r.assignedTravellerIndices == null || r.assignedTravellerIndices.isEmpty()) return false;
            if (r.isOverCapacity()) return false;

            if (r.isMaleRoom()) {
                for (int idx : r.assignedTravellerIndices) {
                    MockTraveller t = travellers.get(idx);
                    if (t.isFemale()) return false;
                }
            } else if (r.isFemaleRoom()) {
                for (int idx : r.assignedTravellerIndices) {
                    MockTraveller t = travellers.get(idx);
                    if (t.isMale()) return false;
                }
            } else if (r.isFamilyRoom()) {
                if (!isFamilyRoomEnabled) return false;
            }
        }
        return true;
    }

    @Test
    public void test1_maleRoomAcceptsMaleTravellers() {
        BookingRequest.RoomConfig maleRoom = new BookingRequest.RoomConfig(1, "triple", "male", 3);
        MockTraveller ahmad = new MockTraveller(0, "Ahmad", "Male");

        assertTrue("Male room must accept male traveller", canAssignTravellerToRoom(maleRoom, ahmad, false));
        maleRoom.assignedTravellerIndices.add(ahmad.index);
        assertEquals(1, maleRoom.getOccupancy());
        assertEquals("male", maleRoom.gender);
        assertTrue(maleRoom.isMaleRoom());
    }

    @Test
    public void test2_maleRoomRejectsFemaleTravellers() {
        BookingRequest.RoomConfig maleRoom = new BookingRequest.RoomConfig(1, "triple", "male", 3);
        MockTraveller siti = new MockTraveller(1, "Siti", "Female");

        assertFalse("Male room must strictly reject female traveller", canAssignTravellerToRoom(maleRoom, siti, false));
    }

    @Test
    public void test3_femaleRoomAcceptsFemaleTravellers() {
        BookingRequest.RoomConfig femaleRoom = new BookingRequest.RoomConfig(2, "triple", "female", 3);
        MockTraveller siti = new MockTraveller(1, "Siti", "Female");

        assertTrue("Female room must accept female traveller", canAssignTravellerToRoom(femaleRoom, siti, false));
        femaleRoom.assignedTravellerIndices.add(siti.index);
        assertEquals(1, femaleRoom.getOccupancy());
        assertEquals("female", femaleRoom.gender);
        assertTrue(femaleRoom.isFemaleRoom());
    }

    @Test
    public void test4_femaleRoomRejectsMaleTravellers() {
        BookingRequest.RoomConfig femaleRoom = new BookingRequest.RoomConfig(2, "triple", "female", 3);
        MockTraveller ali = new MockTraveller(2, "Ali", "Male");

        assertFalse("Female room must strictly reject male traveller", canAssignTravellerToRoom(femaleRoom, ali, false));
    }

    @Test
    public void test5_familyRoomOffPreventsAllMixedGenderStandardRooms() {
        List<MockTraveller> travellers = Arrays.asList(
                new MockTraveller(0, "Ahmad", "Male"),
                new MockTraveller(1, "Ali", "Male"),
                new MockTraveller(2, "Hakim", "Male"),
                new MockTraveller(3, "Siti", "Female"),
                new MockTraveller(4, "Aminah", "Female"),
                new MockTraveller(5, "Fatimah", "Female")
        );

        // Room 1: Male room containing 2 males and 1 female (Siti)
        BookingRequest.RoomConfig room1 = new BookingRequest.RoomConfig(1, "triple", "male", 3);
        room1.assignedTravellerIndices.addAll(Arrays.asList(0, 1, 3)); // 0: Ahmad, 1: Ali, 3: Siti

        // Room 2: Female room containing 2 females and 1 male (Hakim)
        BookingRequest.RoomConfig room2 = new BookingRequest.RoomConfig(2, "triple", "female", 3);
        room2.assignedTravellerIndices.addAll(Arrays.asList(4, 5, 2)); // 4: Aminah, 5: Fatimah, 2: Hakim

        List<BookingRequest.RoomConfig> rooms = Arrays.asList(room1, room2);

        assertFalse("Mixed gender in male room must fail validation when Family Room is OFF",
                isRoomArrangementValid(rooms, travellers, false));
    }

    @Test
    public void test6_eligibleFamilyRoomSupportsApprovedFamilyGrouping() {
        List<MockTraveller> travellers = Arrays.asList(
                new MockTraveller(0, "Ahmad", "Male"),
                new MockTraveller(1, "Siti", "Female"),
                new MockTraveller(2, "Anak 1", "Male"),
                new MockTraveller(3, "Anak 2", "Female")
        );

        BookingRequest.RoomConfig familyRoom = new BookingRequest.RoomConfig(1, "quad", "family", 4);
        familyRoom.assignedTravellerIndices.addAll(Arrays.asList(0, 1, 2, 3));

        List<BookingRequest.RoomConfig> rooms = Arrays.asList(familyRoom);

        assertTrue("Family room must be valid when Family Room is ON",
                isRoomArrangementValid(rooms, travellers, true));

        assertFalse("Family room must be rejected when Family Room is OFF",
                isRoomArrangementValid(rooms, travellers, false));
    }

    @Test
    public void test7_familyRoomOffAfterExistingMixedAssignmentsTriggersRevalidation() {
        List<MockTraveller> travellers = Arrays.asList(
                new MockTraveller(0, "Ahmad", "Male"),
                new MockTraveller(1, "Siti", "Female")
        );

        BookingRequest.RoomConfig familyRoom = new BookingRequest.RoomConfig(1, "double", "family", 2);
        familyRoom.assignedTravellerIndices.addAll(Arrays.asList(0, 1));
        List<BookingRequest.RoomConfig> rooms = new ArrayList<>(Arrays.asList(familyRoom));

        // When ON -> valid
        assertTrue(isRoomArrangementValid(rooms, travellers, true));

        // When toggled OFF -> invalidates family room and returns occupants to unassigned pool
        boolean isFamilyEnabled = false;
        List<Integer> unassignedPool = new ArrayList<>();

        List<BookingRequest.RoomConfig> toRemove = new ArrayList<>();
        for (BookingRequest.RoomConfig r : rooms) {
            if (r.isFamilyRoom()) {
                unassignedPool.addAll(r.assignedTravellerIndices);
                toRemove.add(r);
            }
        }
        rooms.removeAll(toRemove);

        // Occupants are now unassigned, not lost
        assertEquals(2, unassignedPool.size());
        assertTrue(unassignedPool.contains(0));
        assertTrue(unassignedPool.contains(1));
        assertTrue(rooms.isEmpty());

        // Arrangement cannot proceed until reassigned into separate Male and Female rooms
        assertFalse("Empty rooms or unassigned pool must fail arrangement validation",
                isRoomArrangementValid(rooms, travellers, isFamilyEnabled));

        // Reassign into proper separate single/double rooms:
        BookingRequest.RoomConfig maleRoom = new BookingRequest.RoomConfig(1, "double", "male", 2);
        maleRoom.assignedTravellerIndices.add(0);
        BookingRequest.RoomConfig femaleRoom = new BookingRequest.RoomConfig(2, "double", "female", 2);
        femaleRoom.assignedTravellerIndices.add(1);

        rooms.add(maleRoom);
        rooms.add(femaleRoom);

        assertTrue("Reassigned separated rooms must pass validation",
                isRoomArrangementValid(rooms, travellers, isFamilyEnabled));
    }

    @Test
    public void test8_childrenWithoutBedCountTowardCapacity() {
        BookingRequest.RoomConfig maleRoom = new BookingRequest.RoomConfig(1, "double", "male", 2);
        MockTraveller father = new MockTraveller(0, "Bapa", "Male");
        MockTraveller son = new MockTraveller(1, "Anak Lelaki", "Male");

        maleRoom.assignedTravellerIndices.add(father.index);
        maleRoom.assignedTravellerIndices.add(son.index);

        assertEquals("Child without bed still counts toward room capacity", 2, maleRoom.getOccupancy());
        assertFalse(maleRoom.isOverCapacity());

        // Adding another traveller causes over capacity
        MockTraveller extra = new MockTraveller(2, "Lelaki Lain", "Male");
        maleRoom.assignedTravellerIndices.add(extra.index);
        assertTrue(maleRoom.isOverCapacity());
    }

    @Test
    public void test9_multipleMaleAndFemaleRoomsUnderOneBookingWorkCorrectly() {
        // 8 travellers: 4 Male, 4 Female, Family Room: NO
        List<MockTraveller> travellers = Arrays.asList(
                new MockTraveller(0, "Male 1", "Male"),
                new MockTraveller(1, "Male 2", "Male"),
                new MockTraveller(2, "Male 3", "Male"),
                new MockTraveller(3, "Male 4", "Male"),
                new MockTraveller(4, "Female 1", "Female"),
                new MockTraveller(5, "Female 2", "Female"),
                new MockTraveller(6, "Female 3", "Female"),
                new MockTraveller(7, "Female 4", "Female")
        );

        BookingRequest req = new BookingRequest();
        req.adultPaxCount = 8;
        req.adultMaleCount = 4;
        req.adultFemaleCount = 4;
        req.isFamilyRoomEnabled = false;

        // Room 1: Quad Male - 4/4
        BookingRequest.RoomConfig r1 = new BookingRequest.RoomConfig(1, "quad", "male", 4);
        r1.assignedTravellerIndices.addAll(Arrays.asList(0, 1, 2, 3));

        // Room 2: Quad Female - 4/4
        BookingRequest.RoomConfig r2 = new BookingRequest.RoomConfig(2, "quad", "female", 4);
        r2.assignedTravellerIndices.addAll(Arrays.asList(4, 5, 6, 7));

        req.rooms.add(r1);
        req.rooms.add(r2);

        assertEquals(2, req.rooms.size());
        assertEquals(4, r1.getOccupancy());
        assertEquals(4, r2.getOccupancy());
        assertTrue(r1.isMaleRoom());
        assertTrue(r2.isFemaleRoom());
        assertTrue(isRoomArrangementValid(req.rooms, travellers, false));
    }

    public List<BookingRequest.RoomConfig> autoGenerateRooms(BookingRequest req, List<MockTraveller> travellers) {
        List<BookingRequest.RoomConfig> generatedRooms = new ArrayList<>();
        String baseCategory = req.getBaseRoomType();
        int capacity = req.getBaseRoomCapacity();

        if (req.isFamilyRoomEnabled) {
            int roomNum = 1;
            int paxIdx = 0;
            while (paxIdx < travellers.size()) {
                BookingRequest.RoomConfig famRoom = new BookingRequest.RoomConfig(roomNum++, baseCategory, "family", capacity);
                for (int i = 0; i < capacity && paxIdx < travellers.size(); i++) {
                    famRoom.assignedTravellerIndices.add(travellers.get(paxIdx++).index);
                }
                generatedRooms.add(famRoom);
            }
        } else {
            List<MockTraveller> maleList = new ArrayList<>();
            List<MockTraveller> femaleList = new ArrayList<>();
            for (MockTraveller s : travellers) {
                if (s.isFemale()) femaleList.add(s);
                else maleList.add(s);
            }

            int roomNum = 1;

            int maleIdx = 0;
            while (maleIdx < maleList.size()) {
                BookingRequest.RoomConfig mr = new BookingRequest.RoomConfig(roomNum++, baseCategory, "male", capacity);
                for (int i = 0; i < capacity && maleIdx < maleList.size(); i++) {
                    mr.assignedTravellerIndices.add(maleList.get(maleIdx++).index);
                }
                generatedRooms.add(mr);
            }

            int femaleIdx = 0;
            while (femaleIdx < femaleList.size()) {
                BookingRequest.RoomConfig fr = new BookingRequest.RoomConfig(roomNum++, baseCategory, "female", capacity);
                for (int i = 0; i < capacity && femaleIdx < femaleList.size(); i++) {
                    fr.assignedTravellerIndices.add(femaleList.get(femaleIdx++).index);
                }
                generatedRooms.add(fr);
            }
        }
        return generatedRooms;
    }

    @Test
    public void testScenarioA_quint_2Male_2Female_familyOff_generatesTwoQuintGroups_2_5_each() {
        BookingRequest req = new BookingRequest();
        req.roomType = "quint";
        req.isFamilyRoomEnabled = false;

        List<MockTraveller> travellers = Arrays.asList(
                new MockTraveller(0, "Ahmad (Male)", "Male"),
                new MockTraveller(1, "Ali (Male)", "Male"),
                new MockTraveller(2, "Siti (Female)", "Female"),
                new MockTraveller(3, "Aminah (Female)", "Female")
        );

        List<BookingRequest.RoomConfig> rooms = autoGenerateRooms(req, travellers);

        assertEquals("Must generate exactly 2 rooms", 2, rooms.size());

        BookingRequest.RoomConfig r1 = rooms.get(0);
        assertEquals("Room 1 must be Quint", "quint", r1.roomType);
        assertEquals("Room 1 must be Male", "male", r1.gender);
        assertEquals("Room 1 capacity must be 5", 5, r1.capacity);
        assertEquals("Room 1 occupancy must be 2", 2, r1.getOccupancy());
        assertEquals("Room 1 available slots must be 3", 3, r1.getAvailableSlots());

        BookingRequest.RoomConfig r2 = rooms.get(1);
        assertEquals("Room 2 must be Quint", "quint", r2.roomType);
        assertEquals("Room 2 must be Female", "female", r2.gender);
        assertEquals("Room 2 capacity must be 5", 5, r2.capacity);
        assertEquals("Room 2 occupancy must be 2", 2, r2.getOccupancy());
        assertEquals("Room 2 available slots must be 3", 3, r2.getAvailableSlots());

        assertTrue("Arrangement must be strictly valid", isRoomArrangementValid(rooms, travellers, false));
    }

    @Test
    public void testScenarioB_quint_2Male_2Female_familyOn_generatesOneQuintFamilyGroup_4_5() {
        BookingRequest req = new BookingRequest();
        req.roomType = "quint";
        req.isFamilyRoomEnabled = true;

        List<MockTraveller> travellers = Arrays.asList(
                new MockTraveller(0, "Ahmad (Male)", "Male"),
                new MockTraveller(1, "Ali (Male)", "Male"),
                new MockTraveller(2, "Siti (Female)", "Female"),
                new MockTraveller(3, "Aminah (Female)", "Female")
        );

        List<BookingRequest.RoomConfig> rooms = autoGenerateRooms(req, travellers);

        assertEquals("Must generate exactly 1 family room", 1, rooms.size());

        BookingRequest.RoomConfig r1 = rooms.get(0);
        assertEquals("Room 1 must be Quint", "quint", r1.roomType);
        assertEquals("Room 1 must be Family", "family", r1.gender);
        assertEquals("Room 1 capacity must be 5", 5, r1.capacity);
        assertEquals("Room 1 occupancy must be 4", 4, r1.getOccupancy());
        assertEquals("Room 1 available slots must be 1", 1, r1.getAvailableSlots());

        assertTrue("Family arrangement must be valid when Family Room is ON", isRoomArrangementValid(rooms, travellers, true));
    }

    @Test
    public void testExample_quint_7Male_3Female_familyOff_generatesThreeQuintGroups() {
        BookingRequest req = new BookingRequest();
        req.roomType = "quint";
        req.isFamilyRoomEnabled = false;

        List<MockTraveller> travellers = new ArrayList<>();
        for (int m = 0; m < 7; m++) travellers.add(new MockTraveller(m, "Male " + (m + 1), "Male"));
        for (int f = 0; f < 3; f++) travellers.add(new MockTraveller(7 + f, "Female " + (f + 1), "Female"));

        List<BookingRequest.RoomConfig> rooms = autoGenerateRooms(req, travellers);

        assertEquals("7 Male + 3 Female in Quint must generate exactly 3 Quint rooms", 3, rooms.size());

        // Quint Male 1: 5/5
        BookingRequest.RoomConfig r1 = rooms.get(0);
        assertEquals("quint", r1.roomType);
        assertEquals("male", r1.gender);
        assertEquals(5, r1.capacity);
        assertEquals(5, r1.getOccupancy());

        // Quint Male 2: 2/5
        BookingRequest.RoomConfig r2 = rooms.get(1);
        assertEquals("quint", r2.roomType);
        assertEquals("male", r2.gender);
        assertEquals(5, r2.capacity);
        assertEquals(2, r2.getOccupancy());

        // Quint Female 1: 3/5
        BookingRequest.RoomConfig r3 = rooms.get(2);
        assertEquals("quint", r3.roomType);
        assertEquals("female", r3.gender);
        assertEquals(5, r3.capacity);
        assertEquals(3, r3.getOccupancy());

        assertTrue("Arrangement must be strictly valid", isRoomArrangementValid(rooms, travellers, false));
    }

    @Test
    public void testNeverAutomaticallyConvertSelectedQuintCategoryIntoDouble() {
        BookingRequest req = new BookingRequest();
        req.roomType = "quint";
        req.isFamilyRoomEnabled = false;

        // Even with only 2 males, it must NOT convert Quint into Double
        List<MockTraveller> travellers = Arrays.asList(
                new MockTraveller(0, "Ahmad (Male)", "Male"),
                new MockTraveller(1, "Ali (Male)", "Male")
        );

        List<BookingRequest.RoomConfig> rooms = autoGenerateRooms(req, travellers);

        assertEquals(1, rooms.size());
        BookingRequest.RoomConfig r = rooms.get(0);
        assertEquals("Category must remain Quint", "quint", r.roomType);
        assertNotEquals("Must NEVER convert Quint into Double", "double", r.roomType);
        assertEquals("Capacity must be 5", 5, r.capacity);
        assertEquals("Occupancy is 2", 2, r.getOccupancy());
    }
}
