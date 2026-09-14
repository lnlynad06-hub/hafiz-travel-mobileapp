package com.hafiztraveltours.app.network;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.R;


import com.google.gson.annotations.SerializedName;

public class LoginRequest {

    @SerializedName("email")
    public String email;

    @SerializedName("password")
    public String password;

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
