package com.hafiztraveltours.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class SignUpActivity extends AppCompatActivity {

    private TextInputLayout nameLayout, emailLayout, phoneLayout, passwordLayout, confirmPasswordLayout;
    private TextInputEditText nameInput, emailInput, phoneInput, passwordInput, confirmPasswordInput;
    private MaterialButton signUpButton;
    private FirebaseAuth mAuth;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
    }

    private String activeLanguage;

    @Override
    protected void onResume() {
        super.onResume();
        String currentSaved = LocaleHelper.getSavedLanguage(this);
        if (activeLanguage != null && !activeLanguage.equals(currentSaved)) {
            recreate();
        }
        activeLanguage = currentSaved;
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();

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

        // TODO: Google/Facebook/Phone real signup - buat lepas ni
        findViewById(R.id.googleSignUpButton).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.social_signup_google), Toast.LENGTH_SHORT).show());
        findViewById(R.id.facebookSignUpButton).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.social_signup_facebook), Toast.LENGTH_SHORT).show());
        findViewById(R.id.phoneSignUpButton).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.social_signup_phone), Toast.LENGTH_SHORT).show());

        findViewById(R.id.guestText).setOnClickListener(v ->
                startActivity(new Intent(SignUpActivity.this, MainActivity.class))
        );

        setupLanguageButton();
    }

    private void setupLanguageButton() {
        findViewById(R.id.languageButton).setOnClickListener(v -> {
            String[] options = {"English", "Bahasa Melayu", "العربية", "한국어", "日本語", "中文"};
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Choose Language / Pilih Bahasa")
                    .setItems(options, (dialog, which) -> {
                        String lang = (which == 0) ? LocaleHelper.LANGUAGE_ENGLISH
                                : (which == 1) ? LocaleHelper.LANGUAGE_MALAY
                                  : (which == 2) ? LocaleHelper.LANGUAGE_ARABIC
                                    : (which == 3) ? LocaleHelper.LANGUAGE_KOREAN
                                      : (which == 4) ? LocaleHelper.LANGUAGE_JAPANESE
                                        : LocaleHelper.LANGUAGE_CHINESE;
                        LocaleHelper.saveLanguage(this, lang);
                        recreate();
                    })
                    .show();
        });
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

        signUpButton.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest profileUpdate =
                                    new UserProfileChangeRequest.Builder()
                                            .setDisplayName(name)
                                            .build();
                            user.updateProfile(profileUpdate);
                        }
                        signUpButton.setEnabled(true);
                        Toast.makeText(this, "Pendaftaran berjaya!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        signUpButton.setEnabled(true);
                        String errorMsg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Pendaftaran gagal, sila cuba lagi";
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String textOf(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }
}