package com.hafiztraveltours.app.network;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.R;


import com.google.gson.annotations.SerializedName;

public class GoogleLoginRequest {

    @SerializedName("email")
    public String email;

    @SerializedName("name")
    public String name;

    @SerializedName("google_id")
    public String googleId;

    @SerializedName("avatar")
    public String avatar;

    public GoogleLoginRequest(String email, String name, String googleId, String avatar) {
        this.email = email;
        this.name = name;
        this.googleId = googleId;
        this.avatar = avatar;
    }
}
