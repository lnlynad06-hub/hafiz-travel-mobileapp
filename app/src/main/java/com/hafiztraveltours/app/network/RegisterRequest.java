package com.hafiztraveltours.app.network;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {

    @SerializedName("name")
    public String name;

    @SerializedName("email")
    public String email;

    @SerializedName("phone")
    public String phone;

    @SerializedName("password")
    public String password;

    @SerializedName("password_confirmation")
    public String passwordConfirmation;

    public RegisterRequest(String name, String email, String phone, String password, String passwordConfirmation) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.passwordConfirmation = passwordConfirmation;
    }
}
