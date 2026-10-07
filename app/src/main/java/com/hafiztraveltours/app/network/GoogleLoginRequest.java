package com.hafiztraveltours.app.network;
import com.google.gson.annotations.SerializedName;

public class GoogleLoginRequest {

    @SerializedName("id_token")
    public String idToken;

    public GoogleLoginRequest(String idToken) {
        this.idToken = idToken;
    }
}
