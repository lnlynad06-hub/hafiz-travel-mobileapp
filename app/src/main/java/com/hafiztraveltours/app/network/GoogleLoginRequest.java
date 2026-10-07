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

    @SerializedName("id_token")
    public String idToken;

    public GoogleLoginRequest(String email, String name, String googleId, String avatar) {
        this(email, name, googleId, avatar, null);
    }

    public GoogleLoginRequest(String email, String name, String googleId, String avatar, String idToken) {
        this.email = email;
        this.name = name;
        this.googleId = googleId;
        this.avatar = avatar;
        this.idToken = idToken;
    }
}
