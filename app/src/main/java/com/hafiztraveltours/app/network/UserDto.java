package com.hafiztraveltours.app.network;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.R;


import com.google.gson.annotations.SerializedName;

public class UserDto {

    @SerializedName("id")
    public String id;

    @SerializedName("name")
    public String name;

    @SerializedName("nickname")
    public String nickname;

    @SerializedName("email")
    public String email;

    @SerializedName("phone")
    public String phone;

    @SerializedName("gender")
    public String gender;

    @SerializedName("address")
    public String address;

    @SerializedName("address_line_1")
    public String addressLine1;

    @SerializedName("address_line_2")
    public String addressLine2;

    @SerializedName("postcode")
    public String postcode;

    @SerializedName("city")
    public String city;

    @SerializedName("state")
    public String state;

    @SerializedName("country")
    public String country;

    @SerializedName("ic_number")
    public String icNumber;

    @SerializedName("passport_number")
    public String passportNumber;

    @SerializedName("passport_expiry_date")
    public String passportExpiryDate;

    @SerializedName("issuing_country")
    public String issuingCountry;

    @SerializedName("date_of_birth")
    public String dateOfBirth;

    @SerializedName("nationality")
    public String nationality;

    @SerializedName("clothes_size")
    public String clothesSize;

    @SerializedName("avatar")
    public String avatar;

    @SerializedName("emergency_name")
    public String emergencyName;

    @SerializedName("emergency_phone")
    public String emergencyPhone;

    @SerializedName("created_at")
    public String createdAt;

    public UserDto() {}

    public UserDto(String id, String name, String email, String phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public UserDto(String id, String name, String nickname, String email, String phone) {
        this.id = id;
        this.name = name;
        this.nickname = nickname;
        this.email = email;
        this.phone = phone;
    }
}
