package com.hafiztraveltours.app.ui;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.adapters.*;
import com.hafiztraveltours.app.network.*;
import com.hafiztraveltours.app.services.*;
import com.hafiztraveltours.app.utils.*;
import com.hafiztraveltours.app.views.*;
import com.hafiztraveltours.app.ui.*;


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
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.network.UserDto;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
    }

    private SharedPreferences profilePrefs;
    private TextView nameText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        profilePrefs = getSharedPreferences("user_profile", Context.MODE_PRIVATE);

        findViewById(R.id.profileBackButton).setOnClickListener(v -> finish());

        nameText = findViewById(R.id.profileNameText);
        refreshHeader();

        findViewById(R.id.editProfileRow).setOnClickListener(v -> showEditProfileDialog());

        findViewById(R.id.myBookingsRow).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.profile_no_booking), Toast.LENGTH_LONG).show());

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
            SessionManager.getInstance(this).clearSession();
            Toast.makeText(this, getString(R.string.profile_logout_success), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void refreshHeader() {
        if (SessionManager.getInstance(this).isLoggedIn()) {
            String name = SessionManager.getInstance(this).getUserName();
            String email = SessionManager.getInstance(this).getUserEmail();
            nameText.setText((name != null && !name.isEmpty()) ? name : email);
        } else {
            nameText.setText(getString(R.string.profile_guest_name));
        }
    }

    private void showEditProfileDialog() {
        if (!SessionManager.getInstance(this).isLoggedIn()) {
            Toast.makeText(this, getString(R.string.profile_login_to_update), Toast.LENGTH_SHORT).show();
            return;
        }

        String currentName = SessionManager.getInstance(this).getUserName();
        String currentEmail = SessionManager.getInstance(this).getUserEmail();
        String currentPhone = SessionManager.getInstance(this).getUserPhone();

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int padH = dp(24);
        form.setPadding(padH, dp(12), padH, dp(4));

        TextInputLayout nameLayout = createStyledInputLayout(getString(R.string.profile_field_full_name));
        TextInputEditText nameInput = (TextInputEditText) nameLayout.getEditText();
        nameInput.setText(currentName != null ? currentName : "");
        form.addView(nameLayout);

        TextInputLayout phoneLayout = createStyledInputLayout(getString(R.string.profile_field_phone));
        TextInputEditText phoneInput = (TextInputEditText) phoneLayout.getEditText();
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
        phoneInput.setText(currentPhone != null ? currentPhone : "");
        setTopMargin(phoneLayout, 14);
        form.addView(phoneLayout);

        TextInputLayout emailLayout = createStyledInputLayout(getString(R.string.profile_field_email));
        TextInputEditText emailInput = (TextInputEditText) emailLayout.getEditText();
        emailInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setText(currentEmail != null ? currentEmail : "");
        emailInput.setEnabled(false);
        setTopMargin(emailLayout, 14);
        form.addView(emailLayout);

        TextView changePasswordLink = new TextView(this);
        changePasswordLink.setText(getString(R.string.profile_change_password));
        changePasswordLink.setTextColor(getResources().getColor(R.color.pink_dark));
        changePasswordLink.setTypeface(null, Typeface.BOLD);
        changePasswordLink.setTextSize(14);
        changePasswordLink.setPadding(0, dp(16), 0, dp(4));
        form.addView(changePasswordLink);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.profile_edit))
                .setView(form)
                .setPositiveButton(getString(R.string.profile_save), (d, which) -> {
                    String newName = nameInput.getText().toString().trim();
                    String newPhone = phoneInput.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        String userId = SessionManager.getInstance(this).getUserId();
                        String token = SessionManager.getInstance(this).getAuthToken();
                        UserDto updated = new UserDto(userId, newName, currentEmail, newPhone);
                        SessionManager.getInstance(this).saveAuthSession(token, updated);
                        refreshHeader();
                        Toast.makeText(this, getString(R.string.profile_updated), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .create();

        changePasswordLink.setOnClickListener(v -> {
            dialog.dismiss();
            showChangePasswordDialog(currentEmail);
        });

        dialog.show();
    }

    private void showChangePasswordDialog(String email) {
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, getString(R.string.profile_email_invalid), Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.profile_reset_title))
                .setMessage(getString(R.string.profile_reset_message, email))
                .setPositiveButton(getString(R.string.profile_reset_send), (d, which) -> {
                    Map<String, String> body = new HashMap<>();
                    body.put("email", email);
                    ApiClient.getApiService().forgotPassword(body).enqueue(new Callback<ApiResponse<Object>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                            Toast.makeText(ProfileActivity.this, getString(R.string.profile_reset_sent), Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                            Toast.makeText(ProfileActivity.this, getString(R.string.profile_reset_sent), Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton(getString(R.string.cancel), null)
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