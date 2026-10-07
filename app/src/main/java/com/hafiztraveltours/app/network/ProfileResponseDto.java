package com.hafiztraveltours.app.network;

import com.google.gson.annotations.SerializedName;

public class ProfileResponseDto {
    @SerializedName("user")
    public UserDto user;

    @SerializedName("customer_no")
    public String customerNo;

    @SerializedName("identification_no")
    public String identificationNo;

    @SerializedName("nationality")
    public String nationality;
}
