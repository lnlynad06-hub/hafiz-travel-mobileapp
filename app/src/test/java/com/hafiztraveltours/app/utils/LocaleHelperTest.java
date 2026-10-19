package com.hafiztraveltours.app.utils;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LocaleHelperTest {

    @Test
    public void testLocaleResolution() {
        assertEquals(Locale.ENGLISH, LocaleHelper.getLocaleForCode(LocaleHelper.LANGUAGE_ENGLISH));
        assertEquals("ms", LocaleHelper.getLocaleForCode(LocaleHelper.LANGUAGE_MALAY).getLanguage());
        assertEquals(Locale.ENGLISH, LocaleHelper.getLocaleForCode(null));
        assertEquals(Locale.ENGLISH, LocaleHelper.getLocaleForCode("unknown"));
    }

    @Test
    public void testLanguageBadge() {
        assertEquals("EN", LocaleHelper.getLanguageBadge(LocaleHelper.LANGUAGE_ENGLISH));
        assertEquals("BM", LocaleHelper.getLanguageBadge(LocaleHelper.LANGUAGE_MALAY));
        assertEquals("EN", LocaleHelper.getLanguageBadge(null));
    }

    @Test
    public void testNullContextFallback() {
        assertEquals(LocaleHelper.LANGUAGE_ENGLISH, LocaleHelper.getSavedLanguage(null));
    }
}
