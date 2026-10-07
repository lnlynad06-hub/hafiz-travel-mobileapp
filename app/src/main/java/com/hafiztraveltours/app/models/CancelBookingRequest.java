package com.hafiztraveltours.app.models;

import com.google.gson.annotations.SerializedName;

public class CancelBookingRequest {
    @SerializedName("reason")
    public String reason;

    public CancelBookingRequest(String reason) {
        this.reason = reason;
    }
}
