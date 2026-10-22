package com.hafiztraveltours.app.services;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying Prayer Time scheduling rules, stale alarm drop thresholds,
 * playback state tracking, and string resource localization consistency.
 */
public class PrayerSchedulingAndLocalizationUnitTest {

    @Test
    public void testMalaysiaTimezoneDateCalculation() {
        // Given an epoch timestamp at 23:30 UTC on 2026-03-15
        // In Asia/Kuala_Lumpur (UTC+8), it is 07:30 on 2026-03-16
        // UTC: 1773617400 = Sun Mar 15 2026 23:30:00 UTC
        long epochSeconds = 1773617400L;

        String dateStr = PrayerTimeScheduler.getMalaysiaDateString(epochSeconds);
        assertEquals("2026-03-16", dateStr);

        // Verify the timezone constant is Asia/Kuala_Lumpur
        assertEquals("Asia/Kuala_Lumpur", PrayerTimeScheduler.TIME_ZONE_MALAYSIA.getID());
    }

    @Test
    public void testMaxAllowedDriftSeconds() {
        // Alarms delayed by more than 15 minutes (e.g. Doze mode delay until midnight)
        // must be dropped to prevent startling midnight Azan playback.
        assertEquals(900L, PrayerAlarmReceiver.MAX_ALLOWED_DRIFT_SECONDS);
    }

    @Test
    public void testPlaybackTrackerStateTransitions() {
        SharedPreferences mockPrefs = createMockSharedPreferences();

        String prayer = "isha";
        String date = "2026-03-16";

        // Initial: Not played or stopped
        assertFalse(PrayerAlarmReceiver.isPrayerPlaybackCompletedOrStopped(mockPrefs, prayer, date));

        // When alarm triggers: mark dispatched
        PrayerAlarmReceiver.markPrayerAsDispatched(mockPrefs, prayer, date);
        assertFalse(PrayerAlarmReceiver.isPrayerPlaybackCompletedOrStopped(mockPrefs, prayer, date));

        // When prayer finishes naturally: mark played
        PrayerAlarmReceiver.markPrayerAsPlayed(mockPrefs, prayer, date);
        assertTrue(PrayerAlarmReceiver.isPrayerPlaybackCompletedOrStopped(mockPrefs, prayer, date));

        // Future trigger on same day must be recognized as duplicate
        assertTrue(PrayerAlarmReceiver.isPrayerPlaybackCompletedOrStopped(mockPrefs, prayer, date));

        // Different prayer or different date must not be blocked
        assertFalse(PrayerAlarmReceiver.isPrayerPlaybackCompletedOrStopped(mockPrefs, "fajr", date));
        assertFalse(PrayerAlarmReceiver.isPrayerPlaybackCompletedOrStopped(mockPrefs, prayer, "2026-03-17"));

        // If stopped by user:
        PrayerAlarmReceiver.markPrayerAsStopped(mockPrefs, "maghrib", date);
        assertTrue(PrayerAlarmReceiver.isPrayerPlaybackCompletedOrStopped(mockPrefs, "maghrib", date));

        // Clearing tracker resets state
        PrayerAlarmReceiver.clearTracker(mockPrefs);
        assertFalse(PrayerAlarmReceiver.isPrayerPlaybackCompletedOrStopped(mockPrefs, "maghrib", date));
    }

    @Test
    public void testLocalizationXmlRequiredPairs() throws Exception {
        Map<String, String> defaultStrings = loadStringsXml(new File("src/main/res/values/strings.xml"));
        Map<String, String> msStrings = loadStringsXml(new File("src/main/res/values-ms/strings.xml"));

        // 1. Azan stop button
        assertEquals("Stop Azan", defaultStrings.get("azan_stop"));
        assertEquals("Hentikan Azan", msStrings.get("azan_stop"));

        // 2. Umrah Package vs Pakej Umrah
        assertEquals("Umrah Package", defaultStrings.get("category_umrah"));
        assertEquals("Pakej Umrah", msStrings.get("category_umrah"));

        // 3. Tour vs Pelancongan
        assertEquals("Tour", defaultStrings.get("category_tourism"));
        assertEquals("Pelancongan", msStrings.get("category_tourism"));
        assertEquals("Pelancongan", msStrings.get("filter_cat_tour"));

        // 4. Male Room vs Bilik Lelaki
        assertEquals("Male Room", defaultStrings.get("room_category_male_plain"));
        assertEquals("Bilik Lelaki", msStrings.get("room_category_male_plain"));

        // 5. Female Room vs Bilik Perempuan
        assertEquals("Female Room", defaultStrings.get("room_category_female_plain"));
        assertEquals("Bilik Perempuan", msStrings.get("room_category_female_plain"));

        // 6. Family Room vs Bilik Keluarga
        assertEquals("Family Room", defaultStrings.get("room_category_family_plain"));
        assertEquals("Bilik Keluarga", msStrings.get("room_category_family_plain"));

        // 7. Unassigned Travellers vs Jemaah Belum Ditetapkan
        assertEquals("Unassigned Travellers", defaultStrings.get("unassigned_travellers"));
        assertEquals("Jemaah Belum Ditetapkan", msStrings.get("unassigned_travellers"));

        // 8. Continue vs Teruskan
        assertEquals("Continue", defaultStrings.get("action_continue"));
        assertEquals("Teruskan", msStrings.get("action_continue"));

        // 9. Outstanding Balance vs Baki Bayaran
        assertEquals("Outstanding Balance", defaultStrings.get("outstanding_balance"));
        assertEquals("Baki Bayaran", msStrings.get("outstanding_balance"));
        assertEquals("Baki Bayaran", msStrings.get("receipt_balance_due"));
        assertEquals("Baki Bayaran", msStrings.get("booking_label_balance_due"));
    }

    private Map<String, String> loadStringsXml(File file) throws Exception {
        Map<String, String> map = new HashMap<>();
        if (!file.exists()) {
            // fallback if running from root directory
            file = new File("app/" + file.getPath());
        }
        assertTrue("File should exist: " + file.getAbsolutePath(), file.exists());

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(file);
        doc.getDocumentElement().normalize();

        NodeList nList = doc.getElementsByTagName("string");
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Element element = (Element) nList.item(temp);
            String name = element.getAttribute("name");
            String value = element.getTextContent();
            map.put(name, value);
        }
        return map;
    }

    private SharedPreferences createMockSharedPreferences() {
        final Map<String, Object> storage = new HashMap<>();

        final SharedPreferences.Editor editorProxy = (SharedPreferences.Editor) Proxy.newProxyInstance(
                SharedPreferences.Editor.class.getClassLoader(),
                new Class<?>[]{SharedPreferences.Editor.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        String name = method.getName();
                        if ("putInt".equals(name) || "putLong".equals(name) || "putString".equals(name)) {
                            storage.put((String) args[0], args[1]);
                            return proxy;
                        } else if ("clear".equals(name)) {
                            storage.clear();
                            return proxy;
                        } else if ("apply".equals(name) || "commit".equals(name)) {
                            return Boolean.TRUE;
                        }
                        return null;
                    }
                }
        );

        return (SharedPreferences) Proxy.newProxyInstance(
                SharedPreferences.class.getClassLoader(),
                new Class<?>[]{SharedPreferences.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        String name = method.getName();
                        if ("edit".equals(name)) {
                            return editorProxy;
                        } else if ("getInt".equals(name)) {
                            Object val = storage.get((String) args[0]);
                            return val instanceof Integer ? val : (Integer) args[1];
                        } else if ("getLong".equals(name)) {
                            Object val = storage.get((String) args[0]);
                            return val instanceof Long ? val : (Long) args[1];
                        } else if ("getString".equals(name)) {
                            Object val = storage.get((String) args[0]);
                            return val instanceof String ? val : (String) args[1];
                        }
                        return null;
                    }
                }
        );
    }
}
