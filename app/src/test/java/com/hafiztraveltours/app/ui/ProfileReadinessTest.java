package com.hafiztraveltours.app.ui;

import com.hafiztraveltours.app.models.DocumentDto;
import com.hafiztraveltours.app.network.UserDto;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ProfileReadinessTest {

    @Test
    public void computeReadiness_empty_returnsZero() {
        ProfileViewModel.Readiness r = ProfileViewModel.computeReadiness(null, null, null);
        assertEquals(0, r.score);
    }

    @Test
    public void computeReadiness_whitespaceOnly_returnsZero() {
        Map<String, String> extras = new HashMap<>();
        extras.put("name", "   ");
        extras.put("ic_no", " \t ");
        extras.put("passport_no", "  ");
        extras.put("emergency_name", " ");
        extras.put("address", "  ");

        UserDto user = new UserDto();
        user.name = "   ";
        user.icNumber = " ";
        user.passportNumber = " ";
        user.emergencyName = " ";
        user.address = " ";

        ProfileViewModel.Readiness r = ProfileViewModel.computeReadiness(user, extras, null);
        assertEquals(0, r.score);
    }

    @Test
    public void computeReadiness_partialFields_calculatesCorrectSum() {
        Map<String, String> extras = new HashMap<>();
        extras.put("name", "Ahmad bin Ali"); // 15
        extras.put("ic_no", "900101-14-5555"); // 20

        ProfileViewModel.Readiness r = ProfileViewModel.computeReadiness(null, extras, null);
        assertEquals(35, r.score);
    }

    @Test
    public void computeReadiness_allFieldsAndDocs_returnsExactly100() {
        Map<String, String> extras = new HashMap<>();
        extras.put("name", "Ahmad bin Ali"); // 15
        extras.put("ic_no", "900101-14-5555"); // 20
        extras.put("passport_no", "A12345678"); // 20
        extras.put("address", "No 123, Jalan Ampang, Kuala Lumpur"); // 15
        extras.put("emergency_name", "Fatimah"); // 10

        Map<String, DocumentDto> docs = new HashMap<>();
        DocumentDto passportDoc = new DocumentDto();
        passportDoc.documentCode = "passport";
        passportDoc.status = "verified";
        docs.put("passport", passportDoc); // 7

        DocumentDto icDoc = new DocumentDto();
        icDoc.documentCode = "ic";
        icDoc.status = "verified";
        docs.put("ic", icDoc); // 7

        DocumentDto photoDoc = new DocumentDto();
        photoDoc.documentCode = "passport_photo";
        photoDoc.status = "verified";
        docs.put("passport_photo", photoDoc); // 6

        ProfileViewModel.Readiness r = ProfileViewModel.computeReadiness(null, extras, docs);
        assertEquals(100, r.score);
    }

    @Test
    public void computeReadiness_fallbackToUserDto_whenExtrasEmpty() {
        UserDto user = new UserDto();
        user.name = "Siti Nurhaliza";
        user.icNumber = "790111-06-5000";
        user.passportNumber = "A87654321";
        user.emergencyName = "Datuk K";
        user.address = "Taman Melawati, KL";

        ProfileViewModel.Readiness r = ProfileViewModel.computeReadiness(user, new HashMap<>(), null);
        assertEquals(80, r.score); // 15 + 20 + 20 + 15 + 10 = 80
    }
}
