package com.example.hafiztraveltours;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class SignUpActivity extends AppCompatActivity {

    private TextInputLayout nameLayout, emailLayout, phoneLayout, passwordLayout, confirmPasswordLayout;
    private TextInputEditText nameInput, emailInput, phoneInput, passwordInput, confirmPasswordInput;
    private MaterialButton signUpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        nameLayout = findViewById(R.id.nameLayout);
        emailLayout = findViewById(R.id.emailLayout);
        phoneLayout = findViewById(R.id.phoneLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);

        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        phoneInput = findViewById(R.id.phoneInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);

        signUpButton = findViewById(R.id.signUpButton);
        signUpButton.setOnClickListener(v -> attemptSignUp());

        findViewById(R.id.goToLogin).setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        });

        // TODO: wire these up to real Firebase Authentication / Facebook SDK later
        findViewById(R.id.googleSignUpButton).setOnClickListener(v ->
                Toast.makeText(this, "Daftar guna Google - akan datang", Toast.LENGTH_SHORT).show());
        findViewById(R.id.facebookSignUpButton).setOnClickListener(v ->
                Toast.makeText(this, "Daftar guna Facebook - akan datang", Toast.LENGTH_SHORT).show());
        findViewById(R.id.phoneSignUpButton).setOnClickListener(v ->
                Toast.makeText(this, "Daftar guna nombor telefon - akan datang", Toast.LENGTH_SHORT).show());

        // Guest: skip sign up entirely. Replace MainActivity.class with your real
        // Homepage activity once it's built.
        findViewById(R.id.guestText).setOnClickListener(v ->
                startActivity(new Intent(SignUpActivity.this, MainActivity.class))
        );
    }

    private void attemptSignUp() {
        String name = textOf(nameInput);
        String email = textOf(emailInput);
        String phone = textOf(phoneInput);
        String password = textOf(passwordInput);
        String confirmPassword = textOf(confirmPasswordInput);

        boolean valid = true;

        if (TextUtils.isEmpty(name)) {
            nameLayout.setError("Sila masukkan nama penuh");
            valid = false;
        } else {
            nameLayout.setError(null);
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Sila masukkan email yang sah");
            valid = false;
        } else {
            emailLayout.setError(null);
        }

        if (TextUtils.isEmpty(phone) || phone.length() < 9) {
            phoneLayout.setError("Sila masukkan nombor telefon yang sah");
            valid = false;
        } else {
            phoneLayout.setError(null);
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            passwordLayout.setError("Kata laluan sekurang-kurangnya 6 aksara");
            valid = false;
        } else {
            passwordLayout.setError(null);
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError("Kata laluan tidak sepadan");
            valid = false;
        } else {
            confirmPasswordLayout.setError(null);
        }

        if (!valid) return;

        // TODO: replace with real registration call (Firebase Auth / Laravel API)
        Toast.makeText(this, "Pendaftaran berjaya (dummy) - " + name, Toast.LENGTH_SHORT).show();

        startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
        finish();
    }

    private String textOf(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }
}
