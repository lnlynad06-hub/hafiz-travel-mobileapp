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

    @SerializedName("avatar")
    public String avatar;

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
