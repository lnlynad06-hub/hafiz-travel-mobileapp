package com.hafiztraveltours.app.utils;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.adapters.*;
import com.hafiztraveltours.app.network.*;
import com.hafiztraveltours.app.services.*;
import com.hafiztraveltours.app.utils.*;
import com.hafiztraveltours.app.views.*;
import com.hafiztraveltours.app.ui.*;


/**
 * Works out which prayer is "current" (most recently passed) and which is
 * "next" (coming up), plus how far along we are between them as a 0f..1f
 * fraction - used to position the dot on the arc widget.
 *
 * Boundary handling: before Subuh or after Isyak, there is no "next prayer
 * today" data on hand, so this approximates using +/-24h from the nearest
 * known Isyak/Subuh time (prayer times only shift by roughly 1-2 minutes
 * day to day, so this is accurate enough purely for a visual progress
 * indicator - it is NOT used for actual alarm scheduling, which always
 * uses the real API/Adhan values from PrayerTimeScheduler).
 */
public class PrayerProgressCalculator {

    public static class Result {
        public final String currentName;
        public final long currentEpochSeconds;
        public final String nextName;
        public final long nextEpochSeconds;
        public final float progress;

        Result(String currentName, long currentEpochSeconds, String nextName, long nextEpochSeconds, float progress) {
            this.currentName = currentName;
            this.currentEpochSeconds = currentEpochSeconds;
            this.nextName = nextName;
            this.nextEpochSeconds = nextEpochSeconds;
            this.progress = progress;
        }
    }

    /**
     * @param names  prayer names in order, e.g. {"Subuh","Zohor","Asar","Maghrib","Isyak"}
     * @param epochs matching epoch-seconds for each name, same order, same day
     * @param nowEpochSeconds current time (epoch seconds)
     */
    public static Result calculate(String[] names, long[] epochs, long nowEpochSeconds) {
        int n = names.length;

        // Normal case: now falls between two of today's prayers.
        for (int i = 0; i < n - 1; i++) {
            if (nowEpochSeconds >= epochs[i] && nowEpochSeconds < epochs[i + 1]) {
                float fraction = (float) (nowEpochSeconds - epochs[i]) / (epochs[i + 1] - epochs[i]);
                return new Result(names[i], epochs[i], names[i + 1], epochs[i + 1], fraction);
            }
        }

        // Before Subuh: "current" is yesterday's Isyak (approximated as
        // today's Isyak minus 24h), "next" is today's Subuh.
        if (nowEpochSeconds < epochs[0]) {
            long approxYesterdayIsyak = epochs[n - 1] - 24L * 3600L;
            float fraction = (float) (nowEpochSeconds - approxYesterdayIsyak) / (epochs[0] - approxYesterdayIsyak);
            return new Result(names[n - 1], approxYesterdayIsyak, names[0], epochs[0], fraction);
        }

        // After Isyak: "current" is today's Isyak, "next" is tomorrow's
        // Subuh (approximated as today's Subuh plus 24h).
        long approxTomorrowSubuh = epochs[0] + 24L * 3600L;
        float fraction = (float) (nowEpochSeconds - epochs[n - 1]) / (approxTomorrowSubuh - epochs[n - 1]);
        return new Result(names[n - 1], epochs[n - 1], names[0], approxTomorrowSubuh, fraction);
    }
}