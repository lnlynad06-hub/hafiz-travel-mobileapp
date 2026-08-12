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

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout emailLayout, passwordLayout;
    private TextInputEditText emailInput, passwordInput;
    private MaterialButton loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener(v -> attemptLogin());

        findViewById(R.id.goToSignUp).setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class))
        );

        // TODO: wire these up to real Firebase Authentication / Facebook SDK later
        findViewById(R.id.googleLoginButton).setOnClickListener(v ->
                Toast.makeText(this, "Log masuk Google - akan datang", Toast.LENGTH_SHORT).show());
        findViewById(R.id.facebookLoginButton).setOnClickListener(v ->
                Toast.makeText(this, "Log masuk Facebook - akan datang", Toast.LENGTH_SHORT).show());
        findViewById(R.id.phoneLoginButton).setOnClickListener(v ->
                Toast.makeText(this, "Log masuk nombor telefon - akan datang", Toast.LENGTH_SHORT).show());

        // Guest: skip login entirely. Replace MainActivity.class with your real
        // Homepage activity once it's built.
        findViewById(R.id.guestText).setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, MainActivity.class))
        );
    }

    private void attemptLogin() {
        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";

        boolean valid = true;

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Sila masukkan email yang sah");
            valid = false;
        } else {
            emailLayout.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Sila masukkan kata laluan");
            valid = false;
        } else {
            passwordLayout.setError(null);
        }

        if (!valid) return;

        // TODO: replace with real authentication (Firebase Auth / Laravel API call)
        Toast.makeText(this, "Login berjaya (dummy) - " + email, Toast.LENGTH_SHORT).show();

        // startActivity(new Intent(LoginActivity.this, MainActivity.class));
        // finish();
    }
}
