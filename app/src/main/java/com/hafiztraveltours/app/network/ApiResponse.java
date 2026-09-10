package com.hafiztraveltours.app.network;

import com.google.gson.annotations.SerializedName;

public class ApiResponse<T> {
    @SerializedName("status")
    public String status;

    @SerializedName("message")
    public String message;

    @SerializedName("data")
    public T data;

    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }
}
