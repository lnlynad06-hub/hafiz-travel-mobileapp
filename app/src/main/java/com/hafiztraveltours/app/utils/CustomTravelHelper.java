package com.hafiztraveltours.app.utils;

import com.hafiztraveltours.app.models.CreateCustomTravelRequest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CustomTravelHelper {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    /**
     * Calculates child age on the preferred departure date.
     * If departureDateStr is null/empty, uses current date.
     */
    public static int calculateChildAge(String dobStr, String departureDateStr) {
        if (dobStr == null || dobStr.trim().isEmpty()) {
            return 0;
        }
        try {
            Date dob = DATE_FORMAT.parse(dobStr.trim());
            Date refDate = new Date();
            if (departureDateStr != null && !departureDateStr.trim().isEmpty()) {
                refDate = DATE_FORMAT.parse(departureDateStr.trim());
            }

            Calendar dobCal = Calendar.getInstance();
            dobCal.setTime(dob);

            Calendar refCal = Calendar.getInstance();
            refCal.setTime(refDate);

            int age = refCal.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR);
            if (refCal.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            return Math.max(0, age);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Capacity of room categories:
     * Quint: 5, Quad: 4, Triple: 3, Double: 2, Single: 1
     */
    public static int getRoomCapacity(String roomCategory) {
        if (roomCategory == null) return 2;
        switch (roomCategory.toLowerCase()) {
            case "quint":
                return 5;
            case "quad":
                return 4;
            case "triple":
                return 3;
            case "double":
                return 2;
            case "single":
                return 1;
            default:
                return 2;
        }
    }

    /**
     * Checks if a room category is allowed for the travel type.
     * Umrah allows: quint, quad, triple, double (Single prohibited).
     * Tour allows: triple, double, single.
     */
    public static boolean isRoomCategoryAllowed(String travelType, String roomCategory) {
        if (roomCategory == null) return false;
        String cat = roomCategory.toLowerCase();
        boolean isUmrah = "umrah".equalsIgnoreCase(travelType);

        if (isUmrah) {
            return cat.equals("quint") || cat.equals("quad") || cat.equals("triple") || cat.equals("double");
        } else {
            return cat.equals("triple") || cat.equals("double") || cat.equals("single");
        }
    }

    /**
     * Generates requested room groups while strictly preserving the chosen room category.
     *
     * Example: Customer selects Quint with 2 male and 2 female, Family OFF.
     * Generates:
     * - Quint Male — 2/5
     * - Quint Female — 2/5
     * (Does NOT silently convert them to Double rooms!)
     */
    public static List<CreateCustomTravelRequest.RoomArrangement> generateRoomArrangements(
            String travelType,
            String roomCategory,
            int adultMale,
            int adultFemale,
            List<CreateCustomTravelRequest.ChildDetail> children,
            boolean isFamilyRoom) {

        List<CreateCustomTravelRequest.RoomArrangement> arrangements = new ArrayList<>();
        int capacity = getRoomCapacity(roomCategory);
        String categoryName = capitalize(roomCategory);

        int maleChildren = 0;
        int femaleChildren = 0;
        if (children != null) {
            for (CreateCustomTravelRequest.ChildDetail child : children) {
                if ("female".equalsIgnoreCase(child.gender)) {
                    femaleChildren++;
                } else {
                    maleChildren++;
                }
            }
        }

        int totalMales = adultMale + maleChildren;
        int totalFemales = adultFemale + femaleChildren;
        int totalPax = totalMales + totalFemales;

        if (totalPax <= 0) {
            return arrangements;
        }

        if (isFamilyRoom) {
            // Combined Family rooms of the chosen category
            int remaining = totalPax;
            int roomIndex = 1;
            while (remaining > 0) {
                int paxInThisRoom = Math.min(remaining, capacity);
                String desc = String.format(Locale.getDefault(), "%s Family %d (%d/%d)", categoryName, roomIndex, paxInThisRoom, capacity);
                arrangements.add(new CreateCustomTravelRequest.RoomArrangement(
                        roomCategory.toLowerCase(),
                        "family",
                        capacity,
                        paxInThisRoom,
                        desc
                ));
                remaining -= paxInThisRoom;
                roomIndex++;
            }
        } else {
            // Strictly separate male and female rooms, preserving the selected category
            int remainingMales = totalMales;
            int maleRoomIndex = 1;
            while (remainingMales > 0) {
                int paxInThisRoom = Math.min(remainingMales, capacity);
                String desc = (maleRoomIndex == 1 && remainingMales <= capacity)
                        ? String.format(Locale.getDefault(), "%s Male (%d/%d)", categoryName, paxInThisRoom, capacity)
                        : String.format(Locale.getDefault(), "%s Male %d (%d/%d)", categoryName, maleRoomIndex, paxInThisRoom, capacity);
                arrangements.add(new CreateCustomTravelRequest.RoomArrangement(
                        roomCategory.toLowerCase(),
                        "male",
                        capacity,
                        paxInThisRoom,
                        desc
                ));
                remainingMales -= paxInThisRoom;
                maleRoomIndex++;
            }

            int remainingFemales = totalFemales;
            int femaleRoomIndex = 1;
            while (remainingFemales > 0) {
                int paxInThisRoom = Math.min(remainingFemales, capacity);
                String desc = (femaleRoomIndex == 1 && remainingFemales <= capacity)
                        ? String.format(Locale.getDefault(), "%s Female (%d/%d)", categoryName, paxInThisRoom, capacity)
                        : String.format(Locale.getDefault(), "%s Female %d (%d/%d)", categoryName, femaleRoomIndex, paxInThisRoom, capacity);
                arrangements.add(new CreateCustomTravelRequest.RoomArrangement(
                        roomCategory.toLowerCase(),
                        "female",
                        capacity,
                        paxInThisRoom,
                        desc
                ));
                remainingFemales -= paxInThisRoom;
                femaleRoomIndex++;
            }
        }

        return arrangements;
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return Character.toUpperCase(str.charAt(0)) + str.substring(1).toLowerCase();
    }
}
