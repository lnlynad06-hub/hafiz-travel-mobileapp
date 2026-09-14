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
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.network.UserDto;

import java.util.HashMap;
import java.util.List;
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
    private TextView memberIdText;
    private TextView memberSinceText;
    private TextView activeLanguageText;
    private View guestLoginButton;
    private View statsCard;
    private View loyaltyCard;
    private View upcomingCard;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout profileSwipeRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        profilePrefs = getSharedPreferences("user_profile", Context.MODE_PRIVATE);

        findViewById(R.id.profileBackButton).setOnClickListener(v -> finish());

        profileSwipeRefresh = findViewById(R.id.profileSwipeRefresh);
        if (profileSwipeRefresh != null) {
            profileSwipeRefresh.setColorSchemeResources(
                    R.color.brand_magenta,
                    R.color.gold_accent,
                    R.color.brand_dark_pink
            );
            profileSwipeRefresh.setOnRefreshListener(() -> {
                refreshHeader();
                loadStats();
            });
        }

        nameText = findViewById(R.id.profileNameText);
        memberIdText = findViewById(R.id.profileMemberIdText);
        memberSinceText = findViewById(R.id.profileMemberSinceText);
        activeLanguageText = findViewById(R.id.tvActiveLanguage);
        guestLoginButton = findViewById(R.id.btnGuestLogin);
        statsCard = findViewById(R.id.statsCard);
        loyaltyCard = findViewById(R.id.loyaltyCard);
        upcomingCard = findViewById(R.id.upcomingCard);
        refreshHeader();

        if (guestLoginButton != null) {
            guestLoginButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, SignUpActivity.class);
                startActivity(intent);
            });
        }

        findViewById(R.id.editProfileRow).setOnClickListener(v -> showEditProfileDialog());

        View travelDocsRow = findViewById(R.id.travelDocsRow);
        if (travelDocsRow != null) {
            travelDocsRow.setOnClickListener(v -> showTravelDocsBottomSheet());
        }

        findViewById(R.id.myBookingsRow).setOnClickListener(v -> {
            if (!SessionManager.getInstance(this).isLoggedIn()) {
                Toast.makeText(this, getString(R.string.profile_login_to_update), Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(this, MyBookingsActivity.class));
        });

        View languageRow = findViewById(R.id.languageRow);
        if (languageRow != null) {
            languageRow.setOnClickListener(v -> showLanguageBottomSheet());
        }
        View upcomingSeeAll = findViewById(R.id.upcomingSeeAll);
        if (upcomingSeeAll != null) {
            upcomingSeeAll.setOnClickListener(v ->
                    startActivity(new Intent(this, MyBookingsActivity.class)));
        }

        Switch notificationSwitch = findViewById(R.id.notificationSwitch);
        notificationSwitch.setChecked(profilePrefs.getBoolean("notifications_enabled", true));
        notificationSwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) ->
                profilePrefs.edit().putBoolean("notifications_enabled", isChecked).apply());

        findViewById(R.id.logoutButton).setOnClickListener(v -> {
            if (!SessionManager.getInstance(this).isLoggedIn()) {
                startActivity(new Intent(this, SignUpActivity.class));
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.profile_logout))
                    .setMessage(getString(R.string.logout_confirm_message))
                    .setPositiveButton(getString(R.string.profile_logout), (d, which) -> performLogout())
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLanguageBadge();
        refreshHeader();
        renderStats(SessionManager.getInstance(this).getProfileStats());
        loadStats();
    }

    private void updateLanguageBadge() {
        if (activeLanguageText != null) {
            activeLanguageText.setText(LocaleHelper.getLanguageBadge(
                    LocaleHelper.getSavedLanguage(this)));
        }
    }

    private void performLogout() {
        try {
            ApiClient.getApiService().logout().enqueue(new retrofit2.Callback<ApiResponse<Object>>() {
                @Override
                public void onResponse(retrofit2.Call<ApiResponse<Object>> call,
                                       retrofit2.Response<ApiResponse<Object>> response) {
                }

                @Override
                public void onFailure(retrofit2.Call<ApiResponse<Object>> call, Throwable t) {
                }
            });
        } catch (Exception ignored) {}
        SessionManager.getInstance(this).clearSession();
        Toast.makeText(this, getString(R.string.profile_logout_success), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void refreshHeader() {
        boolean loggedIn = SessionManager.getInstance(this).isLoggedIn();
        if (loggedIn) {
            String name = SessionManager.getInstance(this).getUserName();
            String email = SessionManager.getInstance(this).getUserEmail();
            nameText.setText((name != null && !name.isEmpty()) ? name : email);
        } else {
            nameText.setText(getString(R.string.profile_guest_name));
        }
        if (guestLoginButton != null) {
            guestLoginButton.setVisibility(loggedIn ? View.GONE : View.VISIBLE);
        }
        updateLanguageBadge();
    }

    private void loadStats() {
        if (!SessionManager.getInstance(this).isLoggedIn()) {
            renderStats(null);
            if (profileSwipeRefresh != null) profileSwipeRefresh.setRefreshing(false);
            return;
        }
        ApiClient.getApiService().getProfileStats().enqueue(
                new retrofit2.Callback<ApiResponse<com.hafiztraveltours.app.models.ProfileStatsDto>>() {
                    @Override
                    public void onResponse(
                            retrofit2.Call<ApiResponse<com.hafiztraveltours.app.models.ProfileStatsDto>> call,
                            retrofit2.Response<ApiResponse<com.hafiztraveltours.app.models.ProfileStatsDto>> response) {
                        if (isFinishing() || isDestroyed()) return;
                        if (profileSwipeRefresh != null) profileSwipeRefresh.setRefreshing(false);
                        com.hafiztraveltours.app.models.ProfileStatsDto stats =
                                (response.isSuccessful() && response.body() != null
                                        && response.body().isSuccess()) ? response.body().data : null;
                        if (stats != null) {
                            SessionManager.getInstance(ProfileActivity.this).saveProfileStats(stats);
                            renderStats(stats);
                        }
                    }

                    @Override
                    public void onFailure(
                            retrofit2.Call<ApiResponse<com.hafiztraveltours.app.models.ProfileStatsDto>> call,
                            Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        if (profileSwipeRefresh != null) profileSwipeRefresh.setRefreshing(false);
                    }
                });
    }

    private void renderStats(com.hafiztraveltours.app.models.ProfileStatsDto stats) {
        boolean loggedIn = SessionManager.getInstance(this).isLoggedIn();
        if (statsCard != null) statsCard.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
        if (loyaltyCard != null) loyaltyCard.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
        if (!loggedIn) {
            if (memberIdText != null) memberIdText.setVisibility(View.GONE);
            if (memberSinceText != null) memberSinceText.setText(getString(R.string.guest_hero_subtitle));
            if (upcomingCard != null) upcomingCard.setVisibility(View.GONE);
            return;
        }
        if (memberIdText != null) memberIdText.setVisibility(View.VISIBLE);
        if (stats == null) return;

        if (stats.customer != null) {
            if (memberIdText != null && stats.customer.customerNo != null) {
                memberIdText.setText(getString(R.string.customer_no_format, stats.customer.customerNo));
            }
            if (memberSinceText != null && stats.customer.memberSince != null) {
                memberSinceText.setText(getString(R.string.member_since_format, stats.customer.memberSince));
            }
        }

        TextView bookingsValue = findViewById(R.id.statBookingsValue);
        TextView pointsValue = findViewById(R.id.statPointsValue);
        TextView spentValue = findViewById(R.id.statSpentValue);
        if (stats.stats != null) {
            if (bookingsValue != null) bookingsValue.setText(String.valueOf(stats.stats.bookingsCount));
            if (spentValue != null) {
                spentValue.setText(com.hafiztraveltours.app.models.BookingRequest.formatPrice(stats.stats.totalSpent));
            }
        }
        if (stats.loyalty != null) {
            if (pointsValue != null) {
                pointsValue.setText(formatPoints(stats.loyalty.pointsBalance));
            }
            TextView tierName = findViewById(R.id.loyaltyTierName);
            TextView pointsBadge = findViewById(R.id.loyaltyPointsText);
            android.widget.ProgressBar progressBar = findViewById(R.id.loyaltyProgressBar);
            TextView progressText = findViewById(R.id.loyaltyProgressText);
            TextView benefitsText = findViewById(R.id.loyaltyBenefitsText);
            if (tierName != null) {
                tierName.setText("Account & Travel Readiness");
            }
            
            // Kira % kelengkapan profil secara dinamik
            int compScore = 0;
            String passportNo = profilePrefs.getString("passport_no", "").trim();
            String icNo = profilePrefs.getString("ic_no", "").trim();
            String emergName = profilePrefs.getString("emergency_name", "").trim();
            if (!passportNo.isEmpty()) compScore += 30;
            if (!icNo.isEmpty()) compScore += 30;
            if (!emergName.isEmpty()) compScore += 20;

            if (pointsBadge != null) {
                pointsBadge.setText(compScore + "% Complete");
            }
            if (progressBar != null) {
                progressBar.setProgress(compScore);
            }
            if (progressText != null) {
                if (compScore >= 100) {
                    progressText.setText("Your travel profile & documents are 100% complete!");
                } else {
                    progressText.setText("Complete your profile & travel documents for faster booking clearance");
                }
            }
            if (benefitsText != null) {
                benefitsText.setText("✓ Auto-linked to all future Umrah & Tour bookings");
                benefitsText.setVisibility(View.VISIBLE);
            }
        }

        if (upcomingCard != null) {
            if (stats.stats != null && stats.stats.upcoming != null && !stats.stats.upcoming.isEmpty()) {
                com.hafiztraveltours.app.models.ProfileStatsDto.UpcomingBooking next =
                        stats.stats.upcoming.get(0);
                TextView pkg = findViewById(R.id.upcomingTripPackage);
                TextView date = findViewById(R.id.upcomingTripDate);
                if (pkg != null) pkg.setText(next.packageName != null ? next.packageName : "");
                if (date != null) {
                    String line = next.bookingNo != null ? next.bookingNo : "";
                    if (next.departureDate != null && !next.departureDate.isEmpty()) {
                        line += " • " + next.departureDate;
                    }
                    date.setText(line);
                }
                upcomingCard.setVisibility(View.VISIBLE);
            } else {
                upcomingCard.setVisibility(View.GONE);
            }
        }
    }

    private String formatPoints(int points) {
        return String.format(java.util.Locale.US, "%,d", points);
    }

    private String formatMoney(double amount) {
        return String.format(java.util.Locale.US, "%,.0f", amount);
    }

    private int computeTierProgress(com.hafiztraveltours.app.models.ProfileStatsDto.LoyaltyInfo loyalty) {
        if (loyalty.nextTier == null || loyalty.nextTier.minSpend <= 0) return 100;
        double progress = loyalty.lifetimeSpend / loyalty.nextTier.minSpend * 100.0;
        return Math.max(0, Math.min(100, (int) Math.round(progress)));
    }

    private void showLanguageBottomSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_language_picker, null);
        dialog.setContentView(sheetView);

        View btnClose = sheetView.findViewById(R.id.btnCloseSheet);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        String current = LocaleHelper.getSavedLanguage(this);
        if (current == null) current = LocaleHelper.LANGUAGE_ENGLISH;

        View[] items = {
                sheetView.findViewById(R.id.itemLangEnglish),
                sheetView.findViewById(R.id.itemLangMalay)
        };
        String[] codes = {
                LocaleHelper.LANGUAGE_ENGLISH,
                LocaleHelper.LANGUAGE_MALAY
        };
        int[] radioIds = {
                R.id.icRadioEnglish,
                R.id.icRadioMalay
        };
        for (int i = 0; i < items.length; i++) {
            setupLanguageItem(sheetView, items[i], radioIds[i], codes[i], current, dialog);
        }
        dialog.show();
    }

    private void setupLanguageItem(View sheet, View item, int radioId, String langCode,
                                   String currentLang,
                                   com.google.android.material.bottomsheet.BottomSheetDialog dialog) {
        if (item == null) return;
        android.widget.ImageView radio = sheet.findViewById(radioId);
        boolean isSelected = langCode.equalsIgnoreCase(currentLang);
        if (isSelected) {
            item.setBackgroundResource(R.drawable.bg_language_item_selected);
            if (radio != null) radio.setImageResource(R.drawable.ic_check_circle_magenta);
        }
        item.setOnClickListener(v -> {
            dialog.dismiss();
            if (!langCode.equalsIgnoreCase(LocaleHelper.getSavedLanguage(ProfileActivity.this))) {
                LocaleHelper.applyAndSaveLanguage(ProfileActivity.this, langCode);
            }
        });
    }

    private void showEditProfileDialog() {
        if (!SessionManager.getInstance(this).isLoggedIn()) {
            Toast.makeText(this, getString(R.string.profile_login_to_update), Toast.LENGTH_SHORT).show();
            return;
        }

        String currentName = SessionManager.getInstance(this).getUserName();
        String currentEmail = SessionManager.getInstance(this).getUserEmail();
        String currentPhone = SessionManager.getInstance(this).getUserPhone();
        String currentIc = profilePrefs.getString("ic_no", "");
        String currentPassport = profilePrefs.getString("passport_no", "");
        String currentExpiry = profilePrefs.getString("passport_expiry", "");
        String currentEmergName = profilePrefs.getString("emergency_name", "");
        String currentEmergPhone = profilePrefs.getString("emergency_phone", "");
        String currentMahram = profilePrefs.getString("mahram_name", "");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile_custom, null);

        TextInputEditText nameInput = dialogView.findViewById(R.id.nameInput);
        TextInputEditText icInput = dialogView.findViewById(R.id.icInput);
        TextInputEditText phoneInput = dialogView.findViewById(R.id.phoneInput);
        TextInputEditText emailInput = dialogView.findViewById(R.id.emailInput);
        TextInputEditText passportInput = dialogView.findViewById(R.id.passportInput);
        TextInputEditText expiryInput = dialogView.findViewById(R.id.expiryInput);
        TextInputEditText emergNameInput = dialogView.findViewById(R.id.emergNameInput);
        TextInputEditText emergPhoneInput = dialogView.findViewById(R.id.emergPhoneInput);
        TextInputEditText mahramInput = dialogView.findViewById(R.id.mahramInput);

        TextInputLayout nameLayout = dialogView.findViewById(R.id.nameLayout);
        TextInputLayout phoneLayout = dialogView.findViewById(R.id.phoneLayout);

        if (nameInput != null) nameInput.setText(currentName != null ? currentName : "");
        if (icInput != null) icInput.setText(currentIc);
        if (phoneInput != null) phoneInput.setText(currentPhone != null ? currentPhone : "");
        if (emailInput != null) emailInput.setText(currentEmail != null ? currentEmail : "");
        if (passportInput != null) passportInput.setText(currentPassport);
        if (expiryInput != null) expiryInput.setText(currentExpiry);
        if (emergNameInput != null) emergNameInput.setText(currentEmergName);
        if (emergPhoneInput != null) emergPhoneInput.setText(currentEmergPhone);
        if (mahramInput != null) mahramInput.setText(currentMahram);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View btnClose = dialogView.findViewById(R.id.btnCloseDialog);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        View btnCancel = dialogView.findViewById(R.id.btnCancelEdit);
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        TextView changePasswordLink = dialogView.findViewById(R.id.changePasswordLink);
        if (changePasswordLink != null) {
            changePasswordLink.setOnClickListener(v -> {
                dialog.dismiss();
                showChangePasswordDialog(currentEmail);
            });
        }

        TextView btnSave = dialogView.findViewById(R.id.btnSaveEdit);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String newName = nameInput != null ? nameInput.getText().toString().trim() : "";
                String newPhone = phoneInput != null ? phoneInput.getText().toString().trim() : "";
                if (nameLayout != null) nameLayout.setError(null);
                if (phoneLayout != null) phoneLayout.setError(null);

                if (newName.isEmpty()) {
                    if (nameLayout != null) nameLayout.setError(getString(R.string.err_name_required));
                    return;
                }
                if (!newPhone.isEmpty() && !android.util.Patterns.PHONE.matcher(newPhone).matches()) {
                    if (phoneLayout != null) phoneLayout.setError(getString(R.string.err_phone_invalid));
                    return;
                }

                // Save travel details locally to SharedPrefs
                profilePrefs.edit()
                        .putString("ic_no", icInput != null ? icInput.getText().toString().trim() : "")
                        .putString("passport_no", passportInput != null ? passportInput.getText().toString().trim() : "")
                        .putString("passport_expiry", expiryInput != null ? expiryInput.getText().toString().trim() : "")
                        .putString("emergency_name", emergNameInput != null ? emergNameInput.getText().toString().trim() : "")
                        .putString("emergency_phone", emergPhoneInput != null ? emergPhoneInput.getText().toString().trim() : "")
                        .putString("mahram_name", mahramInput != null ? mahramInput.getText().toString().trim() : "")
                        .apply();

                btnSave.setEnabled(false);
                btnSave.setText(getString(R.string.profile_saving));

                java.util.Map<String, String> body = new HashMap<>();
                body.put("name", newName);
                body.put("phone", newPhone);

                ApiClient.getApiService().updateProfile(body).enqueue(new retrofit2.Callback<ApiResponse<ProfileResponseDto>>() {
                    @Override
                    public void onResponse(retrofit2.Call<ApiResponse<ProfileResponseDto>> call,
                                           retrofit2.Response<ApiResponse<ProfileResponseDto>> response) {
                        ProfileResponseDto updated = (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) ? response.body().data : null;
                        if (updated == null || updated.user == null) {
                            btnSave.setEnabled(true);
                            btnSave.setText(getString(R.string.profile_save));
                            Toast.makeText(ProfileActivity.this,
                                    getString(R.string.profile_update_failed), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String token = SessionManager.getInstance(ProfileActivity.this).getAuthToken();
                        SessionManager.getInstance(ProfileActivity.this).saveAuthSession(token, updated.user);
                        refreshHeader();
                        dialog.dismiss();
                        Toast.makeText(ProfileActivity.this,
                                getString(R.string.profile_updated), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(retrofit2.Call<ApiResponse<ProfileResponseDto>> call, Throwable t) {
                        if (dialog.isShowing()) {
                            btnSave.setEnabled(true);
                            btnSave.setText(getString(R.string.profile_save));
                            Toast.makeText(ProfileActivity.this,
                                    getString(R.string.profile_update_failed), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            });
        }

        dialog.show();
    }

    private void showTravelDocsBottomSheet() {
        if (!SessionManager.getInstance(this).isLoggedIn()) {
            Toast.makeText(this, getString(R.string.profile_login_to_update), Toast.LENGTH_SHORT).show();
            return;
        }

        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_travel_docs, null);
        dialog.setContentView(sheetView);

        View btnClose = sheetView.findViewById(R.id.btnCloseSheet);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        String passportNo = profilePrefs.getString("passport_no", "").trim();
        String mahramName = profilePrefs.getString("mahram_name", "").trim();
        boolean hasVaccineCert = profilePrefs.getBoolean("has_vaccine_cert", false);

        TextView tvPassportStatus = sheetView.findViewById(R.id.tvPassportStatus);
        TextView btnPassportAction = sheetView.findViewById(R.id.btnPassportAction);

        if (!passportNo.isEmpty()) {
            if (tvPassportStatus != null) {
                tvPassportStatus.setText(getString(R.string.doc_status_pending));
                tvPassportStatus.setBackgroundResource(R.drawable.bg_status_pending);
                tvPassportStatus.setTextColor(getResources().getColor(R.color.gold_accent));
            }
            if (btnPassportAction != null) {
                btnPassportAction.setText(getString(R.string.doc_action_view));
                btnPassportAction.setBackgroundResource(R.drawable.bg_button_white_square);
                btnPassportAction.setTextColor(getResources().getColor(R.color.brand_magenta));
            }
        } else {
            if (tvPassportStatus != null) {
                tvPassportStatus.setText(getString(R.string.doc_status_not_uploaded));
                tvPassportStatus.setBackgroundResource(R.drawable.bg_status_not_uploaded);
                tvPassportStatus.setTextColor(getResources().getColor(R.color.text_gray));
            }
            if (btnPassportAction != null) {
                btnPassportAction.setText(getString(R.string.doc_action_upload));
                btnPassportAction.setBackgroundResource(R.drawable.bg_button_pink);
                btnPassportAction.setTextColor(getResources().getColor(R.color.white));
            }
        }

        TextView tvVaccineStatus = sheetView.findViewById(R.id.tvVaccineStatus);
        TextView btnVaccineAction = sheetView.findViewById(R.id.btnVaccineAction);

        if (hasVaccineCert) {
            if (tvVaccineStatus != null) {
                tvVaccineStatus.setText(getString(R.string.doc_status_pending));
                tvVaccineStatus.setBackgroundResource(R.drawable.bg_status_pending);
                tvVaccineStatus.setTextColor(getResources().getColor(R.color.gold_accent));
            }
            if (btnVaccineAction != null) {
                btnVaccineAction.setText(getString(R.string.doc_action_view));
                btnVaccineAction.setBackgroundResource(R.drawable.bg_button_white_square);
                btnVaccineAction.setTextColor(getResources().getColor(R.color.brand_magenta));
            }
        } else {
            if (tvVaccineStatus != null) {
                tvVaccineStatus.setText(getString(R.string.doc_status_not_uploaded));
                tvVaccineStatus.setBackgroundResource(R.drawable.bg_status_not_uploaded);
                tvVaccineStatus.setTextColor(getResources().getColor(R.color.text_gray));
            }
            if (btnVaccineAction != null) {
                btnVaccineAction.setText(getString(R.string.doc_action_upload));
                btnVaccineAction.setBackgroundResource(R.drawable.bg_button_pink);
                btnVaccineAction.setTextColor(getResources().getColor(R.color.white));
            }
        }

        TextView tvMarriageStatus = sheetView.findViewById(R.id.tvMarriageStatus);
        TextView btnMarriageAction = sheetView.findViewById(R.id.btnMarriageAction);

        if (!mahramName.isEmpty()) {
            if (tvMarriageStatus != null) {
                tvMarriageStatus.setText(getString(R.string.doc_status_pending));
                tvMarriageStatus.setBackgroundResource(R.drawable.bg_status_pending);
                tvMarriageStatus.setTextColor(getResources().getColor(R.color.gold_accent));
            }
            if (btnMarriageAction != null) {
                btnMarriageAction.setText(getString(R.string.doc_action_view));
                btnMarriageAction.setBackgroundResource(R.drawable.bg_button_white_square);
                btnMarriageAction.setTextColor(getResources().getColor(R.color.brand_magenta));
            }
        } else {
            if (tvMarriageStatus != null) {
                tvMarriageStatus.setText(getString(R.string.doc_status_not_uploaded));
                tvMarriageStatus.setBackgroundResource(R.drawable.bg_status_not_uploaded);
                tvMarriageStatus.setTextColor(getResources().getColor(R.color.text_gray));
            }
            if (btnMarriageAction != null) {
                btnMarriageAction.setText(getString(R.string.doc_action_upload));
                btnMarriageAction.setBackgroundResource(R.drawable.bg_button_pink);
                btnMarriageAction.setTextColor(getResources().getColor(R.color.white));
            }
        }

        if (btnPassportAction != null) {
            btnPassportAction.setOnClickListener(v ->
                    Toast.makeText(ProfileActivity.this, getString(R.string.doc_passport_copy) + ": " + (passportNo.isEmpty() ? getString(R.string.doc_status_not_uploaded) : passportNo), Toast.LENGTH_SHORT).show());
        }

        if (btnVaccineAction != null) {
            btnVaccineAction.setOnClickListener(v ->
                    Toast.makeText(ProfileActivity.this, getString(R.string.doc_vaccine_cert) + ": " + (hasVaccineCert ? getString(R.string.doc_status_pending) : getString(R.string.doc_status_not_uploaded)), Toast.LENGTH_SHORT).show());
        }

        if (btnMarriageAction != null) {
            btnMarriageAction.setOnClickListener(v ->
                    Toast.makeText(ProfileActivity.this, getString(R.string.doc_marriage_cert) + ": " + (mahramName.isEmpty() ? getString(R.string.doc_status_not_uploaded) : mahramName), Toast.LENGTH_SHORT).show());
        }

        ApiClient.getApiService().getUserDocuments().enqueue(new retrofit2.Callback<ApiResponse<List<com.hafiztraveltours.app.models.DocumentDto>>>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse<List<com.hafiztraveltours.app.models.DocumentDto>>> call,
                                   retrofit2.Response<ApiResponse<List<com.hafiztraveltours.app.models.DocumentDto>>> response) {
                if (isFinishing() || isDestroyed()) return;
                List<com.hafiztraveltours.app.models.DocumentDto> docs = (response.isSuccessful() && response.body() != null && response.body().isSuccess())
                        ? response.body().data : null;
                if (docs != null) {
                    for (com.hafiztraveltours.app.models.DocumentDto doc : docs) {
                        if ("passport".equalsIgnoreCase(doc.documentCode)) {
                            updateDocStatusUi(tvPassportStatus, btnPassportAction, doc.status);
                        } else if ("ic".equalsIgnoreCase(doc.documentCode)) {
                            updateDocStatusUi(tvVaccineStatus, btnVaccineAction, doc.status);
                        } else if ("passport_photo".equalsIgnoreCase(doc.documentCode)) {
                            updateDocStatusUi(tvMarriageStatus, btnMarriageAction, doc.status);
                        }
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse<List<com.hafiztraveltours.app.models.DocumentDto>>> call, Throwable t) {}
        });

        dialog.show();
    }

    private void updateDocStatusUi(TextView tvStatus, TextView btnAction, String status) {
        if (tvStatus == null) return;
        if ("submitted".equalsIgnoreCase(status) || "pending".equalsIgnoreCase(status)) {
            tvStatus.setText(getString(R.string.doc_status_pending));
            tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
            tvStatus.setTextColor(getResources().getColor(R.color.gold_accent));
            if (btnAction != null) {
                btnAction.setText(getString(R.string.doc_action_view));
                btnAction.setBackgroundResource(R.drawable.bg_button_white_square);
                btnAction.setTextColor(getResources().getColor(R.color.brand_magenta));
            }
        } else if ("verified".equalsIgnoreCase(status) || "approved".equalsIgnoreCase(status)) {
            tvStatus.setText(getString(R.string.doc_status_verified));
            tvStatus.setBackgroundResource(R.drawable.bg_status_verified);
            tvStatus.setTextColor(android.graphics.Color.parseColor("#047857"));
            if (btnAction != null) {
                btnAction.setText(getString(R.string.doc_action_view));
                btnAction.setBackgroundResource(R.drawable.bg_button_white_square);
                btnAction.setTextColor(getResources().getColor(R.color.brand_magenta));
            }
        } else if ("rejected".equalsIgnoreCase(status)) {
            tvStatus.setText("Rejected");
            tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
            tvStatus.setTextColor(android.graphics.Color.parseColor("#EF4444"));
            if (btnAction != null) {
                btnAction.setText(getString(R.string.doc_action_upload));
                btnAction.setBackgroundResource(R.drawable.bg_button_pink);
                btnAction.setTextColor(getResources().getColor(R.color.white));
            }
        } else {
            tvStatus.setText(getString(R.string.doc_status_not_uploaded));
            tvStatus.setBackgroundResource(R.drawable.bg_status_not_uploaded);
            tvStatus.setTextColor(getResources().getColor(R.color.text_gray));
            if (btnAction != null) {
                btnAction.setText(getString(R.string.doc_action_upload));
                btnAction.setBackgroundResource(R.drawable.bg_button_pink);
                btnAction.setTextColor(getResources().getColor(R.color.white));
            }
        }
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
                            boolean sent = response.isSuccessful() && response.body() != null
                                    && response.body().isSuccess();
                            Toast.makeText(ProfileActivity.this, getString(sent
                                    ? R.string.profile_reset_sent
                                    : R.string.reset_password_failed), Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                            Toast.makeText(ProfileActivity.this, getString(R.string.reset_password_failed), Toast.LENGTH_LONG).show();
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