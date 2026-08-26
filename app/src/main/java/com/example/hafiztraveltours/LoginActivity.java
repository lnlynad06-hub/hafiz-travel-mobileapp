package com.example.hafiztraveltours;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;
import android.content.Context;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout emailLayout, passwordLayout;
    private TextInputEditText emailInput, passwordInput;
    private MaterialButton loginButton;
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
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener(v -> attemptLogin());

        findViewById(R.id.forgotPasswordText).setOnClickListener(v -> showForgotPasswordDialog());

        findViewById(R.id.goToSignUp).setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class))
        );

        // TODO: Google/Facebook/Phone real login - buat lepas ni
        findViewById(R.id.googleLoginButton).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.social_login_google), Toast.LENGTH_SHORT).show());
        findViewById(R.id.facebookLoginButton).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.social_login_facebook), Toast.LENGTH_SHORT).show());
        findViewById(R.id.phoneLoginButton).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.social_login_phone), Toast.LENGTH_SHORT).show());

        findViewById(R.id.guestText).setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, MainActivity.class))
        );

        setupLanguageButton();
    }

    private void setupLanguageButton() {
        TextView languageButton = findViewById(R.id.languageButton);
        languageButton.setText(getLanguageLabel(LocaleHelper.getSavedLanguage(this)));

        languageButton.setOnClickListener(v -> {
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

    private String getLanguageLabel(String code) {
        switch (code) {
            case LocaleHelper.LANGUAGE_MALAY: return "\uD83C\uDF10 BM";
            case LocaleHelper.LANGUAGE_ARABIC: return "\uD83C\uDF10 AR";
            case LocaleHelper.LANGUAGE_KOREAN: return "\uD83C\uDF10 KO";
            case LocaleHelper.LANGUAGE_JAPANESE: return "\uD83C\uDF10 JA";
            case LocaleHelper.LANGUAGE_CHINESE: return "\uD83C\uDF10 ZH";
            default: return "\uD83C\uDF10 EN";
        }
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

        loginButton.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    loginButton.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Login berjaya!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        String errorMsg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Login gagal, sila cuba lagi";
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }
    private void showForgotPasswordDialog() {
        final android.widget.EditText emailField = new android.widget.EditText(this);
        emailField.setHint("Masukkan email anda");
        emailField.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        emailField.setPadding(pad, pad, pad, pad);

        String existingEmail = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        if (!existingEmail.isEmpty()) {
            emailField.setText(existingEmail);
        }

        new android.app.AlertDialog.Builder(this)
                .setTitle("Reset Kata Laluan")
                .setMessage("Masukkan email anda, kami akan hantar pautan untuk reset kata laluan.")
                .setView(emailField)
                .setPositiveButton("Hantar", (dialog, which) -> {
                    String email = emailField.getText().toString().trim();
                    if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(this, "Sila masukkan email yang sah", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Email reset kata laluan telah dihantar. Sila semak inbox anda.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Gagal menghantar email. Sila cuba lagi.", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}