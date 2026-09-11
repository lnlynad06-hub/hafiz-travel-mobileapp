package com.hafiztraveltours.app.network;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {

    @SerializedName("token")
    public String token;

    @SerializedName("token_type")
    public String tokenType;

    @SerializedName("user")
    public UserDto user;
}
