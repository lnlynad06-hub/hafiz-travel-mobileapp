package com.hafiztraveltours.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.text.InputType;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
    }

    private SharedPreferences profilePrefs;
    private FirebaseAuth mAuth;

    private TextView nameText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        profilePrefs = getSharedPreferences("user_profile", Context.MODE_PRIVATE);

        findViewById(R.id.profileBackButton).setOnClickListener(v -> finish());

        nameText = findViewById(R.id.profileNameText);
        refreshHeader();

        findViewById(R.id.editProfileRow).setOnClickListener(v -> showEditProfileDialog());

        findViewById(R.id.myBookingsRow).setOnClickListener(v ->
                Toast.makeText(this, "Tiada tempahan lagi - tempahan anda akan dipaparkan di sini", Toast.LENGTH_LONG).show());

        findViewById(R.id.lightThemeRow).setOnClickListener(v -> {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            recreate();
        });

        findViewById(R.id.darkThemeRow).setOnClickListener(v -> {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            recreate();
        });

        Switch notificationSwitch = findViewById(R.id.notificationSwitch);
        notificationSwitch.setChecked(profilePrefs.getBoolean("notifications_enabled", true));
        notificationSwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) ->
                profilePrefs.edit().putBoolean("notifications_enabled", isChecked).apply());

        findViewById(R.id.logoutButton).setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(this, "Log keluar berjaya", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void refreshHeader() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String name = currentUser.getDisplayName();
            nameText.setText((name != null && !name.isEmpty()) ? name : currentUser.getEmail());
        } else {
            nameText.setText("Pengguna");
        }
    }

    /**
     * Styled edit-profile dialog using TextInputLayout (matches Sign Up page's
     * look) instead of plain EditText. Includes a "Tukar Kata Laluan" link at
     * the bottom that opens a separate password-change dialog.
     */
    private void showEditProfileDialog() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int padH = dp(24);
        form.setPadding(padH, dp(12), padH, dp(4));

        TextInputLayout nameLayout = createStyledInputLayout("Nama Penuh");
        TextInputEditText nameInput = (TextInputEditText) nameLayout.getEditText();
        nameInput.setText(currentUser != null && currentUser.getDisplayName() != null
                ? currentUser.getDisplayName() : "");
        form.addView(nameLayout);

        TextInputLayout phoneLayout = createStyledInputLayout("Nombor Telefon");
        TextInputEditText phoneInput = (TextInputEditText) phoneLayout.getEditText();
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
        phoneInput.setText(profilePrefs.getString("phone", ""));
        setTopMargin(phoneLayout, 14);
        form.addView(phoneLayout);

        TextInputLayout emailLayout = createStyledInputLayout("E-mel");
        TextInputEditText emailInput = (TextInputEditText) emailLayout.getEditText();
        emailInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setText(currentUser != null && currentUser.getEmail() != null
                ? currentUser.getEmail() : "");
        emailInput.setEnabled(false);
        setTopMargin(emailLayout, 14);
        form.addView(emailLayout);

        TextView changePasswordLink = new TextView(this);
        changePasswordLink.setText("Tukar Kata Laluan");
        changePasswordLink.setTextColor(getResources().getColor(R.color.pink_dark));
        changePasswordLink.setTypeface(null, Typeface.BOLD);
        changePasswordLink.setTextSize(14);
        changePasswordLink.setPadding(0, dp(16), 0, dp(4));
        form.addView(changePasswordLink);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Kemaskini Profil")
                .setView(form)
                .setPositiveButton("Simpan", (d, which) -> {
                    String newName = nameInput.getText().toString().trim();
                    if (!newName.isEmpty() && currentUser != null) {
                        UserProfileChangeRequest update = new UserProfileChangeRequest.Builder()
                                .setDisplayName(newName)
                                .build();
                        currentUser.updateProfile(update).addOnCompleteListener(task -> refreshHeader());
                    }
                    profilePrefs.edit()
                            .putString("phone", phoneInput.getText().toString().trim())
                            .apply();
                    Toast.makeText(this, "Profil dikemaskini", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Batal", null)
                .create();

        changePasswordLink.setOnClickListener(v -> {
            dialog.dismiss();
            showChangePasswordDialog();
        });

        dialog.show();
    }

    /**
     * Requires re-authentication with the current password (Firebase security
     * rule) before allowing a password change.
     */
    private void showChangePasswordDialog() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) {
            Toast.makeText(this, "Sila log masuk semula untuk menukar kata laluan", Toast.LENGTH_SHORT).show();
            return;
        }
        String email = currentUser.getEmail();

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int padH = dp(24);
        form.setPadding(padH, dp(12), padH, dp(4));

        TextInputLayout currentPassLayout = createStyledInputLayout("Kata Laluan Semasa");
        TextInputEditText currentPassInput = (TextInputEditText) currentPassLayout.getEditText();
        currentPassInput.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_CLASS_TEXT);
        currentPassLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        form.addView(currentPassLayout);

        TextInputLayout newPassLayout = createStyledInputLayout("Kata Laluan Baharu");
        TextInputEditText newPassInput = (TextInputEditText) newPassLayout.getEditText();
        newPassInput.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_CLASS_TEXT);
        newPassLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        setTopMargin(newPassLayout, 14);
        form.addView(newPassLayout);

        TextInputLayout confirmPassLayout = createStyledInputLayout("Sahkan Kata Laluan Baharu");
        TextInputEditText confirmPassInput = (TextInputEditText) confirmPassLayout.getEditText();
        confirmPassInput.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_CLASS_TEXT);
        confirmPassLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        setTopMargin(confirmPassLayout, 14);
        form.addView(confirmPassLayout);

        new AlertDialog.Builder(this)
                .setTitle("Tukar Kata Laluan")
                .setView(form)
                .setPositiveButton("Simpan", (d, which) -> {
                    String currentPass = currentPassInput.getText().toString().trim();
                    String newPass = newPassInput.getText().toString().trim();
                    String confirmPass = confirmPassInput.getText().toString().trim();

                    if (currentPass.isEmpty() || newPass.isEmpty()) {
                        Toast.makeText(this, "Sila isi semua ruangan", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newPass.length() < 6) {
                        Toast.makeText(this, "Kata laluan baharu sekurang-kurangnya 6 aksara", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!newPass.equals(confirmPass)) {
                        Toast.makeText(this, "Kata laluan baharu tidak sepadan", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AuthCredential credential = EmailAuthProvider.getCredential(email, currentPass);
                    currentUser.reauthenticate(credential).addOnCompleteListener(reauthTask -> {
                        if (reauthTask.isSuccessful()) {
                            currentUser.updatePassword(newPass).addOnCompleteListener(updateTask -> {
                                if (updateTask.isSuccessful()) {
                                    Toast.makeText(this, "Kata laluan berjaya ditukar", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Gagal menukar kata laluan. Sila cuba lagi.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Toast.makeText(this, "Kata laluan semasa salah", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    /**
     * Builds a TextInputLayout matching the com.hafiztraveltours.app's rounded/pink Material style
     * (same corner radius and stroke color used on Sign Up / Login).
     */
    private TextInputLayout createStyledInputLayout(String hint) {
        TextInputLayout layout = new TextInputLayout(this);
        layout.setHint(hint);
        layout.setBoxCornerRadiiResources(R.dimen.input_corner_radius, R.dimen.input_corner_radius,
                R.dimen.input_corner_radius, R.dimen.input_corner_radius);
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        layout.setBoxStrokeColor(getResources().getColor(R.color.pink_primary));
        layout.setHintTextColor(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.pink_primary)));
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextInputEditText editText = new TextInputEditText(this);
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.addView(editText);

        return layout;
    }

    private void setTopMargin(android.view.View view, int dpValue) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
        params.topMargin = dp(dpValue);
        view.setLayoutParams(params);
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}