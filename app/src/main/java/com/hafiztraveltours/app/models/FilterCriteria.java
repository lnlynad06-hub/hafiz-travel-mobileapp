package com.hafiztraveltours.app.models;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.adapters.*;
import com.hafiztraveltours.app.network.*;
import com.hafiztraveltours.app.services.*;
import com.hafiztraveltours.app.utils.*;
import com.hafiztraveltours.app.views.*;
import com.hafiztraveltours.app.ui.*;


import android.content.Context;
import java.io.Serializable;

public class FilterCriteria implements Serializable {
    public static final float DEFAULT_MIN_PRICE = 0f;
    public static final float DEFAULT_MAX_PRICE = 20000f;

    public String category = "ALL";        // "ALL", "UMRAH", "TOUR"
    public String destination = "ALL";     // "ALL", "TURKEY", "KOREA", "JAPAN", "SAUDI", "EUROPE", "VIETNAM", "CHINA"
    public float minPrice = DEFAULT_MIN_PRICE;
    public float maxPrice = DEFAULT_MAX_PRICE;

    public FilterCriteria() {}

    public FilterCriteria(FilterCriteria other) {
        if (other != null) {
            this.category = other.category;
            this.destination = other.destination;
            this.minPrice = other.minPrice;
            this.maxPrice = other.maxPrice;
        }
    }

    public boolean isDefault() {
        return (category == null || "ALL".equalsIgnoreCase(category)) &&
               (destination == null || "ALL".equalsIgnoreCase(destination)) &&
               minPrice <= DEFAULT_MIN_PRICE &&
               maxPrice >= DEFAULT_MAX_PRICE;
    }

    public void reset() {
        this.category = "ALL";
        this.destination = "ALL";
        this.minPrice = DEFAULT_MIN_PRICE;
        this.maxPrice = DEFAULT_MAX_PRICE;
    }

    public int getActiveFilterCount() {
        int count = 0;
        if (category != null && !"ALL".equalsIgnoreCase(category)) count++;
        if (destination != null && !"ALL".equalsIgnoreCase(destination)) count++;
        if (minPrice > DEFAULT_MIN_PRICE || maxPrice < DEFAULT_MAX_PRICE) count++;
        return count;
    }
}
