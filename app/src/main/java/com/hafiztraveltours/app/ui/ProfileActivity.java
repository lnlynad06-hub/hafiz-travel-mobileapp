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
import android.content.Intent;
import android.graphics.Typeface;
import android.text.InputType;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.UserDto;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileActivity extends BaseActivity {

    public static final String EXTRA_ACTION = "extra_action";
    public static final String ACTION_EDIT_PROFILE = "action_edit_profile";
    public static final String ACTION_TRAVEL_DOCS = "action_travel_docs";

    private TextView nameText;
    private TextView memberIdText;
    private TextView memberSinceText;
    private TextView activeLanguageText;
    private View guestLoginButton;
    private View statsCard;
    private View loyaltyCard;
    private View upcomingCard;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout profileSwipeRefresh;

    private final Map<String, DocumentDto> userDocumentsMap = new HashMap<>();
    private ProfileViewModel profileViewModel;
    private com.google.android.material.bottomsheet.BottomSheetDialog travelDocsDialog;

    /** Pending dialog UI for one-shot save/password operation results. */
    private static class PendingOpUi {
        AlertDialog dialog;
        TextView btnSave;
        com.google.android.material.textfield.TextInputLayout errorLayout;
        int saveTextRes;
    }

    private PendingOpUi pendingSaveUi;
    private PendingOpUi pendingPasswordUi;
    private androidx.activity.result.ActivityResultLauncher<String> docPickerLauncher;
    private String pendingUploadDocCode;
    private String pendingUploadDocName;
    private android.net.Uri selectedFileUri;
    private TextView pendingFileNameView;
    private View pendingConfirmButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        BottomNavHelper.setup(this, BottomNavHelper.Tab.PROFILE);

        docPickerLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        handleSelectedFileUri(uri);
                    }
                }
        );

        profileViewModel = new androidx.lifecycle.ViewModelProvider(this).get(ProfileViewModel.class);
        observeProfileState();

        handleIntentAction(getIntent());

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
                if (!profileViewModel.isLoggedIn()) {
                    renderStats(null);
                    if (profileSwipeRefresh != null) profileSwipeRefresh.setRefreshing(false);
                    return;
                }
                profileViewModel.loadStats();
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
        findViewById(R.id.languageRow).setOnClickListener(v -> showLanguageBottomSheet());
        findViewById(R.id.travelDocsRow).setOnClickListener(v -> showTravelDocsBottomSheet());
        findViewById(R.id.changePasswordRow).setOnClickListener(v -> showChangePasswordDialog());
        View logoutButton = findViewById(R.id.logoutButton);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> showLogoutConfirmationDialog());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshHeader();
        if (profileViewModel.isLoggedIn()) {
            profileViewModel.loadStats();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntentAction(intent);
    }

    private void handleIntentAction(Intent intent) {
        if (intent == null) return;
        String action = intent.getStringExtra(EXTRA_ACTION);
        if (ACTION_EDIT_PROFILE.equals(action)) {
            showEditProfileDialog();
        } else if (ACTION_TRAVEL_DOCS.equals(action)) {
            showTravelDocsBottomSheet();
        }
    }

    private void refreshHeader() {
        boolean loggedIn = profileViewModel.isLoggedIn();
        if (guestLoginButton != null) {
            guestLoginButton.setVisibility(loggedIn ? View.GONE : View.VISIBLE);
        }
        if (nameText != null) {
            nameText.setText(loggedIn ? profileViewModel.getUserName() : getString(R.string.profile_guest_name));
        }
        if (activeLanguageText != null) {
            String currentLang = LocaleHelper.getSavedLanguage(this);
            if (LocaleHelper.LANGUAGE_MALAY.equalsIgnoreCase(currentLang)) {
                activeLanguageText.setText(getString(R.string.lang_malay));
            } else {
                activeLanguageText.setText(getString(R.string.lang_english));
            }
        }
    }

    private void observeProfileState() {
        profileViewModel.getStatsData().observe(this, stats -> {
            if (stats != null) renderStats(stats);
        });
        profileViewModel.getStatsLoading().observe(this, loading -> {
            if (profileSwipeRefresh != null
                    && (loading == null || !loading)) {
                profileSwipeRefresh.setRefreshing(false);
            }
        });
        profileViewModel.getUserData().observe(this, user -> refreshHeader());
        profileViewModel.getDocsData().observe(this, docs -> {
            userDocumentsMap.clear();
            if (docs != null) {
                for (DocumentDto d : docs) {
                    if (d != null && d.documentCode != null) {
                        userDocumentsMap.put(d.documentCode.toLowerCase(), d);
                    }
                }
            }
            if (travelDocsDialog != null && travelDocsDialog.isShowing()) {
                populateTravelDocsSheet();
            }
        });
        profileViewModel.getReadiness().observe(this, readiness -> {
            if (readiness != null) renderReadiness(readiness);
        });
        profileViewModel.getSaveOp().observe(this, event -> {
            com.hafiztraveltours.app.utils.ApiOpResult result = event != null ? event.consume() : null;
            if (result != null) handleSaveResult(result);
        });
        profileViewModel.getPasswordOp().observe(this, event -> {
            com.hafiztraveltours.app.utils.ApiOpResult result = event != null ? event.consume() : null;
            if (result != null) handlePasswordResult(result);
        });
        profileViewModel.getUploadOp().observe(this, event -> {
            com.hafiztraveltours.app.utils.ApiOpResult result = event != null ? event.consume() : null;
            if (result != null) handleUploadResult(result);
        });
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.profile_logout)
                .setMessage(R.string.logout_confirm_message)
                .setPositiveButton(R.string.profile_logout, (dialog, which) -> {
                    profileViewModel.logout();
                    refreshHeader();
                    renderStats(null);
                    Toast.makeText(this, getString(R.string.profile_logout_success), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void renderStats(com.hafiztraveltours.app.models.ProfileStatsDto stats) {
        boolean loggedIn = profileViewModel.isLoggedIn();
        if (statsCard != null) statsCard.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
        if (!loggedIn) {
            if (loyaltyCard != null) loyaltyCard.setVisibility(View.GONE);
            if (memberIdText != null) memberIdText.setVisibility(View.GONE);
            if (memberSinceText != null) memberSinceText.setText(getString(R.string.guest_hero_subtitle));
            if (upcomingCard != null) upcomingCard.setVisibility(View.GONE);
            return;
        }
        if (memberIdText != null) memberIdText.setVisibility(View.VISIBLE);
        ProfileViewModel.Readiness currentReadiness = profileViewModel.getReadiness().getValue();
        if (currentReadiness != null) {
            renderReadiness(currentReadiness);
        } else if (loyaltyCard != null) {
            loyaltyCard.setVisibility(View.GONE);
        }
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
            if (tierName != null) {
                tierName.setText(getString(R.string.readiness_title));
            }
            ProfileViewModel.Readiness latest = profileViewModel.getReadiness().getValue();
            if (latest != null) renderReadiness(latest);
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

    /** Renders the readiness score block from ViewModel state (pure view code). */
    private void renderReadiness(ProfileViewModel.Readiness readiness) {
        int compScore = readiness != null ? readiness.score : 0;
        TextView pointsBadge = findViewById(R.id.loyaltyPointsText);
        android.widget.ProgressBar progressBar = findViewById(R.id.loyaltyProgressBar);
        TextView progressText = findViewById(R.id.loyaltyProgressText);
        TextView benefitsText = findViewById(R.id.loyaltyBenefitsText);

        // Hide completely if not logged in or readiness reaches 100%
        if (!profileViewModel.isLoggedIn() || compScore >= 100) {
            if (loyaltyCard != null) {
                loyaltyCard.setVisibility(View.GONE);
            }
            return;
        }

        if (loyaltyCard != null) {
            loyaltyCard.setVisibility(View.VISIBLE);
        }

        if (pointsBadge != null) {
            pointsBadge.setText(getString(R.string.readiness_percent_format, compScore));
        }
        if (progressBar != null) {
            progressBar.setProgress(compScore);
        }
        if (progressText != null) {
            progressText.setText(getString(R.string.readiness_incomplete_msg));
        }
        if (benefitsText != null) {
            benefitsText.setText(getString(R.string.readiness_autolink_msg));
            benefitsText.setVisibility(View.VISIBLE);
        }
    }

    private String formatPoints(int points) {
        return String.format(java.util.Locale.US, "%,d", points);
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

    /** Null-safe extras lookup with default (view plumbing for dialog prefill). */
    private static String exVal(java.util.Map<String, String> ex, String key, String def) {
        String v = ex != null ? ex.get(key) : null;
        return v != null ? v : def;
    }

    private static String matchCountry(String[] countries, String current) {
        if (current == null || current.trim().isEmpty()) return "";
        String c = current.trim();
        for (String country : countries) {
            if (country.equalsIgnoreCase(c)) return country;
        }
        String lower = c.toLowerCase();
        for (String country : countries) {
            String cl = country.toLowerCase();
            if (cl.startsWith(lower) || lower.startsWith(cl)) return country;
            if (lower.contains("singap") && cl.contains("singap")) return country;
            if (lower.contains("filipin") && cl.contains("philippin")) return country;
            if (lower.contains("philippin") && cl.contains("filipin")) return country;
            if (lower.contains("kemboj") && cl.contains("cambodi")) return country;
            if (lower.contains("cambodi") && cl.contains("kemboj")) return country;
        }
        return countries.length > 0 ? countries[0] : c;
    }

    private static final String[] ASEAN_PHONE_CODES = new String[]{
            "+673", "+856", "+855", "+670", "+60", "+65", "+62", "+66", "+84", "+63", "+95"
    };

    private void showCountryPickerBottomSheet(TextView tvCountryCode, String[] codeHolder) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_country_picker, null);
        dialog.setContentView(sheetView);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.55f);
        }
        View btnClose = sheetView.findViewById(R.id.btnCloseSheet);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        int[] itemIds = new int[]{
                R.id.itemMalaysia, R.id.itemSingapore, R.id.itemIndonesia, R.id.itemBrunei,
                R.id.itemThailand, R.id.itemVietnam, R.id.itemPhilippines, R.id.itemLaos,
                R.id.itemMyanmar, R.id.itemCambodia, R.id.itemTimorLeste
        };
        String[] codes = new String[]{
                "+60", "+65", "+62", "+673",
                "+66", "+84", "+63", "+856",
                "+95", "+855", "+670"
        };
        for (int i = 0; i < itemIds.length; i++) {
            View itm = sheetView.findViewById(itemIds[i]);
            final String code = codes[i];
            if (itm != null) {
                itm.setOnClickListener(v -> {
                    com.hafiztraveltours.app.utils.HapticUtil.click(v);
                    codeHolder[0] = code;
                    if (tvCountryCode != null) tvCountryCode.setText(code);
                    dialog.dismiss();
                });
            }
        }
        dialog.show();
    }

    private void showEditProfileDialog() {
        if (!profileViewModel.isLoggedIn()) {
            Toast.makeText(this, getString(R.string.profile_login_to_update), Toast.LENGTH_SHORT).show();
            return;
        }

        UserDto currentUser = profileViewModel.getSessionUser();
        java.util.Map<String, String> ex = profileViewModel.readProfileExtras();

        String currentNickname = profileViewModel.getUserNickname();
        String currentName = profileViewModel.getUserName();
        String currentEmail = profileViewModel.getUserEmail();
        String currentPhone = profileViewModel.getUserPhone();
        String currentIc = (currentUser != null && currentUser.icNumber != null && !currentUser.icNumber.isEmpty())
                ? currentUser.icNumber : exVal(ex, "ic_no", "");
        String currentPassport = (currentUser != null && currentUser.passportNumber != null && !currentUser.passportNumber.isEmpty())
                ? currentUser.passportNumber : exVal(ex, "passport_no", "");
        String currentExpiry = (currentUser != null && currentUser.passportExpiryDate != null && !currentUser.passportExpiryDate.isEmpty())
                ? currentUser.passportExpiryDate : exVal(ex, "passport_expiry", "");
        String currentIssuingCountry = (currentUser != null && currentUser.issuingCountry != null && !currentUser.issuingCountry.isEmpty())
                ? currentUser.issuingCountry : exVal(ex, "issuing_country", "Malaysia");
        String currentDob = (currentUser != null && currentUser.dateOfBirth != null && !currentUser.dateOfBirth.isEmpty())
                ? currentUser.dateOfBirth : exVal(ex, "date_of_birth", "");
        String currentNationality = (currentUser != null && currentUser.nationality != null && !currentUser.nationality.isEmpty())
                ? currentUser.nationality : exVal(ex, "nationality", "Malaysian");
        String currentClothesSize = (currentUser != null && currentUser.clothesSize != null && !currentUser.clothesSize.isEmpty())
                ? currentUser.clothesSize : exVal(ex, "clothes_size", "");

        String currentEmergName = exVal(ex, "emergency_name", "");
        String currentEmergPhone = exVal(ex, "emergency_phone", "");
        String currentMahram = exVal(ex, "mahram_name", "");
        String currentMahramRel = exVal(ex, "mahram_relationship", "");
        boolean isMahramApplicable = profileViewModel.readProfileFlag("mahram_applicable", !currentMahram.isEmpty());

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile_custom, null);

        // 1. Personal Information Inputs
        TextInputEditText nameInput = dialogView.findViewById(R.id.nameInput);
        TextInputEditText nicknameInput = dialogView.findViewById(R.id.nicknameInput);
        TextInputEditText icInput = dialogView.findViewById(R.id.icInput);
        TextInputEditText phoneInput = dialogView.findViewById(R.id.phoneInput);
        View btnCountryCode = dialogView.findViewById(R.id.btnCountryCode);
        TextView tvCountryCode = dialogView.findViewById(R.id.tvCountryCode);
        TextView phoneErrorText = dialogView.findViewById(R.id.phoneErrorText);
        TextInputEditText emailInput = dialogView.findViewById(R.id.emailInput);
        com.google.android.material.textfield.MaterialAutoCompleteTextView genderInput = dialogView.findViewById(R.id.genderInput);
        TextInputEditText dobInput = dialogView.findViewById(R.id.dobInput);
        TextView tvDobAge = dialogView.findViewById(R.id.tvDobAge);
        com.google.android.material.textfield.MaterialAutoCompleteTextView nationalityInput = dialogView.findViewById(R.id.nationalityInput);

        // 2. Residential Address Inputs
        TextInputEditText addressLine1Input = dialogView.findViewById(R.id.addressLine1Input);
        TextInputEditText addressLine2Input = dialogView.findViewById(R.id.addressLine2Input);
        TextInputEditText postcodeInput = dialogView.findViewById(R.id.postcodeInput);
        TextInputEditText cityInput = dialogView.findViewById(R.id.cityInput);
        TextInputEditText stateInput = dialogView.findViewById(R.id.stateInput);
        com.google.android.material.textfield.MaterialAutoCompleteTextView countryInput = dialogView.findViewById(R.id.countryInput);

        // 3. Passport Information Inputs
        TextInputEditText passportInput = dialogView.findViewById(R.id.passportInput);
        com.google.android.material.textfield.MaterialAutoCompleteTextView issuingCountryInput = dialogView.findViewById(R.id.issuingCountryInput);
        TextInputEditText expiryInput = dialogView.findViewById(R.id.expiryInput);
        LinearLayout warningContainer = dialogView.findViewById(R.id.passportWarningContainer);
        TextView warningText = dialogView.findViewById(R.id.passportWarningText);

        // 4. Emergency Contact Inputs
        TextInputEditText emergNameInput = dialogView.findViewById(R.id.emergNameInput);
        TextInputEditText emergPhoneInput = dialogView.findViewById(R.id.emergPhoneInput);

        // 5. Mahram Information Inputs
        com.google.android.material.button.MaterialButtonToggleGroup mahramToggleGroup = dialogView.findViewById(R.id.mahramToggleGroup);
        com.google.android.material.button.MaterialButton btnMahramNo = dialogView.findViewById(R.id.btnMahramNo);
        com.google.android.material.button.MaterialButton btnMahramYes = dialogView.findViewById(R.id.btnMahramYes);
        View mahramFieldsContainer = dialogView.findViewById(R.id.mahramFieldsContainer);
        TextInputEditText mahramInput = dialogView.findViewById(R.id.mahramInput);
        com.google.android.material.textfield.MaterialAutoCompleteTextView mahramRelInput = dialogView.findViewById(R.id.mahramRelInput);

        TextInputLayout nameLayout = dialogView.findViewById(R.id.nameLayout);
        TextInputLayout nicknameLayout = dialogView.findViewById(R.id.nicknameLayout);

        // Setup Gender Options (Localized)
        boolean isMalay = "ms".equalsIgnoreCase(LocaleHelper.getSavedLanguage(this));
        String genderMaleStr = isMalay ? "Lelaki" : "Male";
        String genderFemaleStr = isMalay ? "Perempuan" : "Female";
        String[] genderOptions = new String[]{genderMaleStr, genderFemaleStr};
        android.widget.ArrayAdapter<String> genderAdapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, genderOptions);
        if (genderInput != null) {
            genderInput.setAdapter(genderAdapter);
        }

        // Setup Southeast Asian Countries Dropdowns (Personal, Issuing, Residential)
        String[] seaCountries = getResources().getStringArray(R.array.sea_countries);

        CountryDropdownAdapter nationalityAdapter = new CountryDropdownAdapter(this, seaCountries);
        if (nationalityInput != null) {
            nationalityInput.setAdapter(nationalityAdapter);
            nationalityInput.setDropDownBackgroundResource(R.drawable.bg_dropdown_popup);
            nationalityInput.setOnItemClickListener((parent, view, position, id) -> {
                String selected = (String) parent.getItemAtPosition(position);
                nationalityAdapter.setSelectedCountry(selected);
            });
            nationalityInput.setOnClickListener(v -> {
                nationalityAdapter.setSelectedCountry(nationalityInput.getText().toString());
                nationalityInput.showDropDown();
            });
        }

        CountryDropdownAdapter issuingCountryAdapter = new CountryDropdownAdapter(this, seaCountries);
        if (issuingCountryInput != null) {
            issuingCountryInput.setAdapter(issuingCountryAdapter);
            issuingCountryInput.setDropDownBackgroundResource(R.drawable.bg_dropdown_popup);
            issuingCountryInput.setOnItemClickListener((parent, view, position, id) -> {
                String selected = (String) parent.getItemAtPosition(position);
                issuingCountryAdapter.setSelectedCountry(selected);
            });
            issuingCountryInput.setOnClickListener(v -> {
                issuingCountryAdapter.setSelectedCountry(issuingCountryInput.getText().toString());
                issuingCountryInput.showDropDown();
            });
        }

        CountryDropdownAdapter countryAdapter = new CountryDropdownAdapter(this, seaCountries);
        if (countryInput != null) {
            countryInput.setAdapter(countryAdapter);
            countryInput.setDropDownBackgroundResource(R.drawable.bg_dropdown_popup);
            countryInput.setOnItemClickListener((parent, view, position, id) -> {
                String selected = (String) parent.getItemAtPosition(position);
                countryAdapter.setSelectedCountry(selected);
            });
            countryInput.setOnClickListener(v -> {
                countryAdapter.setSelectedCountry(countryInput.getText().toString());
                countryInput.showDropDown();
            });
        }

        // Setup Phone Number & Country Code (Shared with Create Account)
        final String[] selectedCountryCode = new String[]{"+60"};
        String localPhone = "";
        if (currentPhone != null && !currentPhone.trim().isEmpty()) {
            String cleaned = currentPhone.trim();
            boolean matched = false;
            for (String code : ASEAN_PHONE_CODES) {
                if (cleaned.startsWith(code)) {
                    selectedCountryCode[0] = code;
                    localPhone = cleaned.substring(code.length()).replaceAll("^0+", "");
                    matched = true;
                    break;
                } else if (cleaned.startsWith(code.substring(1))) {
                    selectedCountryCode[0] = code;
                    localPhone = cleaned.substring(code.length() - 1).replaceAll("^0+", "");
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                if (cleaned.startsWith("0")) {
                    selectedCountryCode[0] = "+60";
                    localPhone = cleaned.substring(1);
                } else {
                    localPhone = cleaned;
                }
            }
        }
        if (tvCountryCode != null) tvCountryCode.setText(selectedCountryCode[0]);
        if (phoneInput != null) phoneInput.setText(localPhone);
        if (btnCountryCode != null) {
            btnCountryCode.setOnClickListener(v -> {
                com.hafiztraveltours.app.utils.HapticUtil.click(v);
                showCountryPickerBottomSheet(tvCountryCode, selectedCountryCode);
            });
        }

        // Setup Mahram Relationship Dropdown Options
        String[] mahramOptions = new String[]{"Father", "Husband", "Brother", "Son", "Other"};
        android.widget.ArrayAdapter<String> mahramAdapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, mahramOptions);
        if (mahramRelInput != null) {
            mahramRelInput.setAdapter(mahramAdapter);
        }

        String currentGender = (currentUser != null && currentUser.gender != null && !currentUser.gender.isEmpty())
                ? currentUser.gender : exVal(ex, "gender", "");
        if (!currentGender.isEmpty()) {
            if ("male".equalsIgnoreCase(currentGender) || "lelaki".equalsIgnoreCase(currentGender)) {
                currentGender = genderMaleStr;
            } else if ("female".equalsIgnoreCase(currentGender) || "perempuan".equalsIgnoreCase(currentGender)) {
                currentGender = genderFemaleStr;
            }
        }

        String currentAddress1 = (currentUser != null && currentUser.addressLine1 != null && !currentUser.addressLine1.isEmpty())
                ? currentUser.addressLine1 : exVal(ex, "address_line_1", "");
        String currentAddress2 = (currentUser != null && currentUser.addressLine2 != null && !currentUser.addressLine2.isEmpty())
                ? currentUser.addressLine2 : exVal(ex, "address_line_2", "");
        String currentPostcode = (currentUser != null && currentUser.postcode != null && !currentUser.postcode.isEmpty())
                ? currentUser.postcode : exVal(ex, "postcode", "");
        String currentCity = (currentUser != null && currentUser.city != null && !currentUser.city.isEmpty())
                ? currentUser.city : exVal(ex, "city", "");
        String currentState = (currentUser != null && currentUser.state != null && !currentUser.state.isEmpty())
                ? currentUser.state : exVal(ex, "state", "");
        String currentCountry = (currentUser != null && currentUser.country != null && !currentUser.country.isEmpty())
                ? currentUser.country : exVal(ex, "country", "Malaysia");

        // Fill current values
        if (nameInput != null) nameInput.setText(currentName != null ? currentName : "");
        if (nicknameInput != null) nicknameInput.setText(currentNickname != null ? currentNickname : "");
        if (icInput != null) icInput.setText(currentIc);
        if (emailInput != null) emailInput.setText(currentEmail != null ? currentEmail : "");
        if (genderInput != null && !currentGender.isEmpty()) {
            genderInput.setText(currentGender, false);
        }
        if (dobInput != null) dobInput.setText(currentDob);
        String matchedNationality = matchCountry(seaCountries, currentNationality);
        if (nationalityInput != null && !matchedNationality.isEmpty()) {
            nationalityInput.setText(matchedNationality, false);
            nationalityAdapter.setSelectedCountry(matchedNationality);
        }

        if (addressLine1Input != null) addressLine1Input.setText(currentAddress1);
        if (addressLine2Input != null) addressLine2Input.setText(currentAddress2);
        if (postcodeInput != null) postcodeInput.setText(currentPostcode);
        if (cityInput != null) cityInput.setText(currentCity);
        if (stateInput != null) stateInput.setText(currentState);
        String matchedCountry = matchCountry(seaCountries, currentCountry);
        if (countryInput != null && !matchedCountry.isEmpty()) {
            countryInput.setText(matchedCountry, false);
            countryAdapter.setSelectedCountry(matchedCountry);
        }

        if (passportInput != null) passportInput.setText(currentPassport);
        String matchedIssuingCountry = matchCountry(seaCountries, currentIssuingCountry);
        if (issuingCountryInput != null && !matchedIssuingCountry.isEmpty()) {
            issuingCountryInput.setText(matchedIssuingCountry, false);
            issuingCountryAdapter.setSelectedCountry(matchedIssuingCountry);
        }
        if (expiryInput != null) expiryInput.setText(currentExpiry);
        if (emergNameInput != null) emergNameInput.setText(currentEmergName);
        if (emergPhoneInput != null) emergPhoneInput.setText(currentEmergPhone);
        if (mahramInput != null) mahramInput.setText(currentMahram);
        if (mahramRelInput != null && !currentMahramRel.isEmpty()) {
            mahramRelInput.setText(currentMahramRel, false);
        }

        // Dynamic Age Calculation from DOB
        Runnable updateAgeBadge = () -> {
            String dobStr = dobInput != null ? dobInput.getText().toString().trim() : "";
            if (tvDobAge == null) return;
            if (dobStr.isEmpty()) {
                tvDobAge.setVisibility(View.GONE);
                return;
            }
            try {
                String[] parts = dobStr.split("-");
                if (parts.length == 3) {
                    int y = Integer.parseInt(parts[0]);
                    int m = Integer.parseInt(parts[1]) - 1;
                    int d = Integer.parseInt(parts[2]);
                    java.util.Calendar today = java.util.Calendar.getInstance();
                    int age = today.get(java.util.Calendar.YEAR) - y;
                    if (today.get(java.util.Calendar.MONTH) < m ||
                            (today.get(java.util.Calendar.MONTH) == m && today.get(java.util.Calendar.DAY_OF_MONTH) < d)) {
                        age--;
                    }
                    if (age >= 0) {
                        tvDobAge.setText(getString(R.string.profile_age_format, age));
                        tvDobAge.setVisibility(View.VISIBLE);
                    } else {
                        tvDobAge.setVisibility(View.GONE);
                    }
                } else {
                    tvDobAge.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                tvDobAge.setVisibility(View.GONE);
            }
        };
        updateAgeBadge.run();

        if (dobInput != null) {
            dobInput.setOnClickListener(v -> {
                java.util.Calendar calDob = java.util.Calendar.getInstance();
                int year = calDob.get(java.util.Calendar.YEAR) - 30;
                int month = calDob.get(java.util.Calendar.MONTH);
                int day = calDob.get(java.util.Calendar.DAY_OF_MONTH);
                String cDob = dobInput.getText().toString().trim();
                if (!cDob.isEmpty()) {
                    try {
                        String[] parts = cDob.split("-");
                        if (parts.length == 3) {
                            year = Integer.parseInt(parts[0]);
                            month = Integer.parseInt(parts[1]) - 1;
                            day = Integer.parseInt(parts[2]);
                        }
                    } catch (Exception ignored) {}
                }
                new android.app.DatePickerDialog(
                        this,
                        (view, selectedYear, selectedMonth, selectedDay) -> {
                            String formattedDate = String.format(java.util.Locale.US, "%04d-%02d-%02d",
                                    selectedYear, selectedMonth + 1, selectedDay);
                            dobInput.setText(formattedDate);
                            updateAgeBadge.run();
                        },
                        year, month, day
                ).show();
            });
        }

        // Passport Expiry Logic & Warning Check (Inline Banner Below Date Input)
        java.util.Calendar cal = java.util.Calendar.getInstance();
        Runnable checkPassportWarning = () -> {
            String expStr = expiryInput != null ? expiryInput.getText().toString().trim() : "";
            if (expStr.isEmpty() || warningContainer == null || warningText == null) {
                if (warningContainer != null) warningContainer.setVisibility(View.GONE);
                return;
            }
            try {
                java.util.Date expDate = com.hafiztraveltours.app.utils.DateFormats.parseApiDate(expStr);
                if (expDate != null) {
                    java.util.Calendar sixMonths = java.util.Calendar.getInstance();
                    sixMonths.add(java.util.Calendar.MONTH, 6);
                    java.util.Date now = new java.util.Date();
                    if (expDate.before(now)) {
                        warningText.setText(getString(R.string.passport_expired_warning, expStr));
                        warningContainer.setVisibility(View.VISIBLE);
                    } else if (expDate.before(sixMonths.getTime())) {
                        warningText.setText(getString(R.string.passport_expiry_warning, expStr));
                        warningContainer.setVisibility(View.VISIBLE);
                    } else {
                        warningContainer.setVisibility(View.GONE);
                    }
                } else {
                    warningContainer.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                if (warningContainer != null) warningContainer.setVisibility(View.GONE);
            }
        };
        checkPassportWarning.run();

        if (expiryInput != null) {
            expiryInput.setOnClickListener(v -> {
                int year = cal.get(java.util.Calendar.YEAR);
                int month = cal.get(java.util.Calendar.MONTH);
                int day = cal.get(java.util.Calendar.DAY_OF_MONTH);
                String currentExp = expiryInput.getText().toString().trim();
                if (!currentExp.isEmpty()) {
                    try {
                        String[] parts = currentExp.split("-");
                        if (parts.length == 3) {
                            year = Integer.parseInt(parts[0]);
                            month = Integer.parseInt(parts[1]) - 1;
                            day = Integer.parseInt(parts[2]);
                        }
                    } catch (Exception ignored) {}
                }
                android.app.DatePickerDialog datePicker = new android.app.DatePickerDialog(
                        this,
                        (view, selectedYear, selectedMonth, selectedDay) -> {
                            String formattedDate = String.format(java.util.Locale.US, "%04d-%02d-%02d",
                                    selectedYear, selectedMonth + 1, selectedDay);
                            expiryInput.setText(formattedDate);
                            checkPassportWarning.run();
                        },
                        year, month, day
                );
                datePicker.show();
            });
        }

        // Mahram Toggle Handling (Hides Name and Relationship when "No" is selected)
        if (mahramToggleGroup != null && mahramFieldsContainer != null) {
            if (isMahramApplicable) {
                mahramToggleGroup.check(R.id.btnMahramYes);
                mahramFieldsContainer.setVisibility(View.VISIBLE);
            } else {
                mahramToggleGroup.check(R.id.btnMahramNo);
                mahramFieldsContainer.setVisibility(View.GONE);
            }

            mahramToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {
                    if (checkedId == R.id.btnMahramYes) {
                        mahramFieldsContainer.setVisibility(View.VISIBLE);
                    } else {
                        mahramFieldsContainer.setVisibility(View.GONE);
                    }
                }
            });
        }

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

        TextView btnSave = dialogView.findViewById(R.id.btnSaveEdit);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String newName = nameInput != null ? nameInput.getText().toString().trim() : "";
                String newNickname = nicknameInput != null ? nicknameInput.getText().toString().trim() : "";
                String rawPhone = phoneInput != null ? phoneInput.getText().toString().trim() : "";
                String newPhone = "";
                if (!rawPhone.isEmpty()) {
                    newPhone = SignUpViewModel.normalizePhone(rawPhone, selectedCountryCode[0]);
                }
                String newGender = genderInput != null ? genderInput.getText().toString().trim() : "";
                String newDob = dobInput != null ? dobInput.getText().toString().trim() : "";
                String newNationality = nationalityInput != null ? nationalityInput.getText().toString().trim() : "";

                String newAddress1 = addressLine1Input != null ? addressLine1Input.getText().toString().trim() : "";
                String newAddress2 = addressLine2Input != null ? addressLine2Input.getText().toString().trim() : "";
                String newPostcode = postcodeInput != null ? postcodeInput.getText().toString().trim() : "";
                String newCity = cityInput != null ? cityInput.getText().toString().trim() : "";
                String newState = stateInput != null ? stateInput.getText().toString().trim() : "";
                String newCountry = countryInput != null ? countryInput.getText().toString().trim() : "";

                // Format standard gender value for backend API ("male"/"female")
                String apiGender = ProfileViewModel.normalizeGender(newGender);

                // Construct full address summary string
                String combinedAddress = ProfileViewModel.combineAddress(
                        newAddress1, newAddress2, newPostcode, newCity, newState, newCountry);

                String newIc = icInput != null ? icInput.getText().toString().trim() : "";
                String newPassport = passportInput != null ? passportInput.getText().toString().trim() : "";
                String newIssuingCountry = issuingCountryInput != null ? issuingCountryInput.getText().toString().trim() : "";
                String newExpiry = expiryInput != null ? expiryInput.getText().toString().trim() : "";

                if (nameLayout != null) {
                    nameLayout.setError(null);
                    nameLayout.setErrorEnabled(false);
                }
                if (nicknameLayout != null) {
                    nicknameLayout.setError(null);
                    nicknameLayout.setErrorEnabled(false);
                }
                if (phoneErrorText != null) phoneErrorText.setVisibility(View.GONE);

                ProfileViewModel.FormErrors formErrors =
                        profileViewModel.validateProfileForm(newName, newNickname);
                if (formErrors.nameErr != 0) {
                    if (nameLayout != null) {
                        nameLayout.setErrorEnabled(true);
                        nameLayout.setError(getString(formErrors.nameErr));
                    }
                    return;
                }
                if (formErrors.nickErr != 0) {
                    if (nicknameLayout != null) {
                        nicknameLayout.setErrorEnabled(true);
                        nicknameLayout.setError(getString(formErrors.nickErr));
                    }
                    return;
                }
                if (!rawPhone.isEmpty()) {
                    int phoneErr = com.hafiztraveltours.app.utils.Validator.phone(rawPhone, false, R.string.err_phone_invalid);
                    if (phoneErr != 0) {
                        if (phoneErrorText != null) {
                            phoneErrorText.setText(getString(phoneErr));
                            phoneErrorText.setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(this, getString(phoneErr), Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }
                }

                // Jangan benarkan pengguna memadam (empty) maklumat yang sudah diisi sebelum ini
                if (!currentIc.isEmpty() && newIc.isEmpty()) {
                    Toast.makeText(this, getString(R.string.profile_ic_locked), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!currentPassport.isEmpty() && newPassport.isEmpty()) {
                    Toast.makeText(this, getString(R.string.profile_passport_locked), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!currentAddress1.isEmpty() && newAddress1.isEmpty()) {
                    Toast.makeText(this, getString(R.string.profile_address_locked), Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean mahramYes = mahramToggleGroup != null && mahramToggleGroup.getCheckedButtonId() == R.id.btnMahramYes;
                String finalMahramName = mahramYes ? (mahramInput != null ? mahramInput.getText().toString().trim() : "") : "";
                String finalMahramRel = mahramYes ? (mahramRelInput != null ? mahramRelInput.getText().toString().trim() : "") : "";

                ProfileViewModel.ProfileForm form = new ProfileViewModel.ProfileForm();
                form.name = newName;
                form.nickname = newNickname;
                form.email = (currentUser != null && currentUser.email != null && !currentUser.email.trim().isEmpty())
                        ? currentUser.email.trim()
                        : (emailInput != null ? emailInput.getText().toString().trim() : "");
                form.phone = newPhone;
                form.gender = apiGender;
                form.ic = newIc;
                form.passport = newPassport;
                form.expiry = newExpiry;
                form.issuingCountry = newIssuingCountry;
                form.dob = newDob;
                form.nationality = newNationality;
                form.addressLine1 = newAddress1;
                form.addressLine2 = newAddress2;
                form.postcode = newPostcode;
                form.city = newCity;
                form.state = newState;
                form.country = newCountry;
                form.combinedAddress = combinedAddress;
                form.emergencyName = emergNameInput != null ? emergNameInput.getText().toString().trim() : "";
                form.emergencyPhone = emergPhoneInput != null ? emergPhoneInput.getText().toString().trim() : "";
                form.mahramYes = mahramYes;
                form.mahramName = finalMahramName;
                form.mahramRelationship = finalMahramRel;

                // Local-first persistence (same order as before), then API.
                profileViewModel.saveProfileExtras(form);

                btnSave.setEnabled(false);
                btnSave.setText(getString(R.string.profile_saving));

                PendingOpUi pending = new PendingOpUi();
                pending.dialog = dialog;
                pending.btnSave = btnSave;
                pending.saveTextRes = R.string.profile_save;
                pendingSaveUi = pending;

                profileViewModel.updateProfile(form);
            });
        }

        dialog.show();
    }

    /** Renders the save-profile one-shot result (button + toast + dismiss). */
    private void handleSaveResult(com.hafiztraveltours.app.utils.ApiOpResult result) {
        PendingOpUi pending = pendingSaveUi;
        pendingSaveUi = null;
        if (pending == null || pending.dialog == null || !pending.dialog.isShowing()) return;
        if (pending.btnSave != null) {
            pending.btnSave.setEnabled(true);
            pending.btnSave.setText(getString(pending.saveTextRes));
        }
        if (result.success) {
            pending.dialog.dismiss();
            Toast.makeText(this, getString(R.string.profile_updated), Toast.LENGTH_SHORT).show();
        } else {
            String message;
            if (result.errorResponse != null) {
                message = com.hafiztraveltours.app.network.ApiErrors.userMessage(
                        this, result.errorResponse, result.fallbackResId);
            } else {
                message = com.hafiztraveltours.app.network.ApiErrors.userMessage(
                        this, result.error, result.fallbackResId);
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showTravelDocsBottomSheet() {
        if (!profileViewModel.isLoggedIn()) {
            Toast.makeText(this, getString(R.string.profile_login_to_update), Toast.LENGTH_SHORT).show();
            return;
        }

        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_travel_docs, null);
        dialog.setContentView(sheetView);

        View btnClose = sheetView.findViewById(R.id.btnCloseSheet);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        java.util.Map<String, String> vaultEx = profileViewModel.readProfileExtras();
        String passportNo = exVal(vaultEx, "passport_no", "").trim();
        String mahramName = exVal(vaultEx, "mahram_name", "").trim();

        View cardDocProgressContainer = sheetView.findViewById(R.id.cardDocProgressContainer);
        TextView tvDocProgressPercent = sheetView.findViewById(R.id.tvDocProgressPercent);
        TextView tvDocProgressCount = sheetView.findViewById(R.id.tvDocProgressCount);
        ProgressBar pbDocVerification = sheetView.findViewById(R.id.pbDocVerification);
        TextView tvDocProgressMessage = sheetView.findViewById(R.id.tvDocProgressMessage);

        TextView tvPassportStatus = sheetView.findViewById(R.id.tvPassportStatus);
        TextView btnPassportAction = sheetView.findViewById(R.id.btnPassportAction);
        TextView tvPassportDates = sheetView.findViewById(R.id.tvPassportDates);
        TextView tvPassportGuidance = sheetView.findViewById(R.id.tvPassportGuidance);
        LinearLayout passportRejectionContainer = sheetView.findViewById(R.id.passportRejectionContainer);
        TextView tvPassportRejectionReason = sheetView.findViewById(R.id.tvPassportRejectionReason);

        TextView tvMarriageStatus = sheetView.findViewById(R.id.tvMarriageStatus);
        TextView btnMarriageAction = sheetView.findViewById(R.id.btnMarriageAction);
        TextView tvMarriageDates = sheetView.findViewById(R.id.tvMarriageDates);
        TextView tvMarriageGuidance = sheetView.findViewById(R.id.tvMarriageGuidance);
        LinearLayout marriageRejectionContainer = sheetView.findViewById(R.id.marriageRejectionContainer);
        TextView tvMarriageRejectionReason = sheetView.findViewById(R.id.tvMarriageRejectionReason);

        TextView tvVisaStatus = sheetView.findViewById(R.id.tvVisaStatus);
        TextView btnVisaAction = sheetView.findViewById(R.id.btnVisaAction);
        TextView tvVisaDates = sheetView.findViewById(R.id.tvVisaDates);
        TextView tvVisaGuidance = sheetView.findViewById(R.id.tvVisaGuidance);
        LinearLayout visaRejectionContainer = sheetView.findViewById(R.id.visaRejectionContainer);
        TextView tvVisaRejectionReason = sheetView.findViewById(R.id.tvVisaRejectionReason);

        if (!passportNo.isEmpty()) {
            if (tvPassportStatus != null) {
                tvPassportStatus.setText(getString(R.string.doc_status_under_review));
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

        if (!mahramName.isEmpty()) {
            if (tvMarriageStatus != null) {
                tvMarriageStatus.setText(getString(R.string.doc_status_under_review));
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

        // Travel Visa: Provided & managed by company, view-only for customers
        if (tvVisaStatus != null) {
            tvVisaStatus.setText(getString(R.string.doc_visa_managed_by_company));
            tvVisaStatus.setBackgroundResource(R.drawable.bg_status_pending);
            tvVisaStatus.setTextColor(getResources().getColor(R.color.gold_accent));
        }
        if (btnVisaAction != null) {
            btnVisaAction.setVisibility(View.GONE);
            btnVisaAction.setText(getString(R.string.doc_action_view));
            btnVisaAction.setBackgroundResource(R.drawable.bg_button_white_square);
            btnVisaAction.setTextColor(getResources().getColor(R.color.brand_magenta));
            btnVisaAction.setOnClickListener(v -> openUploadedDocument("visa"));
        }
        if (tvVisaGuidance != null) {
            tvVisaGuidance.setText(getString(R.string.doc_visa_company_note));
            tvVisaGuidance.setVisibility(View.VISIBLE);
        }

        if (btnPassportAction != null) {
            btnPassportAction.setOnClickListener(v -> {
                String action = btnPassportAction.getText().toString();
                if (getString(R.string.doc_action_view).equals(action)) {
                    openUploadedDocument("passport");
                } else {
                    showUploadDocumentDialog("passport", getString(R.string.doc_passport_copy), R.drawable.ic_doc_passport);
                }
            });
        }

        if (btnMarriageAction != null) {
            btnMarriageAction.setOnClickListener(v -> {
                String action = btnMarriageAction.getText().toString();
                if (getString(R.string.doc_action_view).equals(action)) {
                    openUploadedDocument("passport_photo");
                } else {
                    showUploadDocumentDialog("passport_photo", getString(R.string.doc_passport_photo_title), R.drawable.ic_profile);
                }
            });
        }

        View btnPassportReq = sheetView.findViewById(R.id.btnPassportReq);
        View btnMarriageReq = sheetView.findViewById(R.id.btnMarriageReq);
        View btnVisaReq = sheetView.findViewById(R.id.btnVisaReq);

        if (btnPassportReq != null) {
            btnPassportReq.setOnClickListener(v -> showDocRequirementsDialog("passport", getString(R.string.doc_passport_copy), R.drawable.ic_doc_passport));
        }
        if (btnMarriageReq != null) {
            btnMarriageReq.setOnClickListener(v -> showDocRequirementsDialog("passport_photo", getString(R.string.doc_passport_photo_title), R.drawable.ic_profile));
        }
        if (btnVisaReq != null) {
            btnVisaReq.setOnClickListener(v -> showDocRequirementsDialog("visa", getString(R.string.doc_travel_visa_title), R.drawable.ic_visa));
        }

        // M8 single-flight with the stats-chained docs fetch above.
        travelDocsDialog = dialog;
        populateTravelDocsSheet();
        profileViewModel.refreshDocuments();

        dialog.show();
    }

    /**
     * Populates the open vault sheet: placeholder inference first, then real document
     * data when observed. Called on open and on every docs update (H1/Step 5).
     */
    private void populateTravelDocsSheet() {
        if (travelDocsDialog == null || isFinishing() || isDestroyed()) return;

        TextView tvPassportStatus = travelDocsDialog.findViewById(R.id.tvPassportStatus);
        TextView btnPassportAction = travelDocsDialog.findViewById(R.id.btnPassportAction);
        TextView tvPassportDates = travelDocsDialog.findViewById(R.id.tvPassportDates);
        TextView tvPassportGuidance = travelDocsDialog.findViewById(R.id.tvPassportGuidance);
        LinearLayout passportRejectionContainer = travelDocsDialog.findViewById(R.id.passportRejectionContainer);
        TextView tvPassportRejectionReason = travelDocsDialog.findViewById(R.id.tvPassportRejectionReason);

        TextView tvMarriageStatus = travelDocsDialog.findViewById(R.id.tvMarriageStatus);
        TextView btnMarriageAction = travelDocsDialog.findViewById(R.id.btnMarriageAction);
        TextView tvMarriageDates = travelDocsDialog.findViewById(R.id.tvMarriageDates);
        TextView tvMarriageGuidance = travelDocsDialog.findViewById(R.id.tvMarriageGuidance);
        LinearLayout marriageRejectionContainer = travelDocsDialog.findViewById(R.id.marriageRejectionContainer);
        TextView tvMarriageRejectionReason = travelDocsDialog.findViewById(R.id.tvMarriageRejectionReason);

        TextView tvVisaStatus = travelDocsDialog.findViewById(R.id.tvVisaStatus);
        TextView btnVisaAction = travelDocsDialog.findViewById(R.id.btnVisaAction);
        TextView tvVisaDates = travelDocsDialog.findViewById(R.id.tvVisaDates);
        TextView tvVisaGuidance = travelDocsDialog.findViewById(R.id.tvVisaGuidance);
        LinearLayout visaRejectionContainer = travelDocsDialog.findViewById(R.id.visaRejectionContainer);
        TextView tvVisaRejectionReason = travelDocsDialog.findViewById(R.id.tvVisaRejectionReason);

        View cardDocProgressContainer = travelDocsDialog.findViewById(R.id.cardDocProgressContainer);
        TextView tvDocProgressPercent = travelDocsDialog.findViewById(R.id.tvDocProgressPercent);
        TextView tvDocProgressCount = travelDocsDialog.findViewById(R.id.tvDocProgressCount);
        ProgressBar pbDocVerification = travelDocsDialog.findViewById(R.id.pbDocVerification);
        TextView tvDocProgressMessage = travelDocsDialog.findViewById(R.id.tvDocProgressMessage);

        java.util.List<com.hafiztraveltours.app.models.DocumentDto> docs =
                profileViewModel.getDocsData().getValue();
        if (docs != null) {
            int verifiedCount = 0;
            int underReviewCount = 0;
            int rejectedCount = 0;
            final int requiredTotal = 2; // passport, passport_photo (visa managed by company)

            for (com.hafiztraveltours.app.models.DocumentDto doc : docs) {
                if (doc.documentCode != null) {
                    userDocumentsMap.put(doc.documentCode.toLowerCase(), doc);
                }
                boolean isRequiredDoc = "passport".equalsIgnoreCase(doc.documentCode)
                        || "passport_photo".equalsIgnoreCase(doc.documentCode);

                String st = doc.status != null ? doc.status : "";

                if (isRequiredDoc) {
                    switch (com.hafiztraveltours.app.utils.DocumentStatus.from(st)) {
                        case VERIFIED:
                            verifiedCount++;
                            break;
                        case PENDING:
                            underReviewCount++;
                            break;
                        case REJECTED:
                            rejectedCount++;
                            break;
                        default:
                            break;
                    }
                }

                if ("passport".equalsIgnoreCase(doc.documentCode)) {
                    updateDocStatusUi(tvPassportStatus, btnPassportAction, tvPassportDates, tvPassportGuidance, passportRejectionContainer, tvPassportRejectionReason, doc);
                } else if ("passport_photo".equalsIgnoreCase(doc.documentCode)) {
                    updateDocStatusUi(tvMarriageStatus, btnMarriageAction, tvMarriageDates, tvMarriageGuidance, marriageRejectionContainer, tvMarriageRejectionReason, doc);
                } else if ("visa".equalsIgnoreCase(doc.documentCode) || "travel_visa".equalsIgnoreCase(doc.documentCode)) {
                    updateDocStatusUi(tvVisaStatus, btnVisaAction, tvVisaDates, tvVisaGuidance, visaRejectionContainer, tvVisaRejectionReason, doc);
                    if (btnVisaAction != null) {
                        if (doc.filePath != null && !doc.filePath.isEmpty()) {
                            btnVisaAction.setVisibility(View.VISIBLE);
                            btnVisaAction.setText(getString(R.string.doc_action_view));
                        } else {
                            btnVisaAction.setVisibility(View.GONE);
                        }
                    }
                    if (tvVisaGuidance != null) {
                        tvVisaGuidance.setText(getString(R.string.doc_visa_company_note));
                        tvVisaGuidance.setVisibility(View.VISIBLE);
                    }
                }
            }

                    int progressPercent = (int) Math.round((verifiedCount / (double) requiredTotal) * 100);

                    if (cardDocProgressContainer != null) cardDocProgressContainer.setVisibility(View.VISIBLE);
                    if (tvDocProgressPercent != null) tvDocProgressPercent.setText(progressPercent + "%");
                    if (tvDocProgressCount != null) {
                        tvDocProgressCount.setText(getString(R.string.doc_progress_count_format, verifiedCount, requiredTotal));
                    }
                    if (pbDocVerification != null) pbDocVerification.setProgress(progressPercent);

                    if (tvDocProgressMessage != null) {
                        if (rejectedCount > 0) {
                            tvDocProgressMessage.setText(getString(R.string.msg_doc_progress_rejected));
                            tvDocProgressMessage.setTextColor(android.graphics.Color.parseColor("#DC2626"));
                        } else if (verifiedCount == requiredTotal) {
                            tvDocProgressMessage.setText(getString(R.string.msg_doc_progress_complete));
                            tvDocProgressMessage.setTextColor(android.graphics.Color.parseColor("#047857"));
                        } else if (verifiedCount + underReviewCount == requiredTotal) {
                            tvDocProgressMessage.setText(getString(R.string.msg_doc_progress_under_review));
                            tvDocProgressMessage.setTextColor(getResources().getColor(R.color.gold_accent));
                        } else if (verifiedCount > 0 || underReviewCount > 0) {
                            tvDocProgressMessage.setText(getString(R.string.msg_doc_progress_partial));
                            tvDocProgressMessage.setTextColor(getResources().getColor(R.color.brand_magenta));
                        } else {
                            tvDocProgressMessage.setText(getString(R.string.msg_doc_progress_start));
                            tvDocProgressMessage.setTextColor(getResources().getColor(R.color.brand_magenta));
                        }
                    }
                }
    }

    private void updateDocStatusUi(TextView tvStatus, TextView btnAction, TextView tvDates, TextView tvGuidance, View rejectionContainer, TextView tvRejectionReason, com.hafiztraveltours.app.models.DocumentDto doc) {
        if (tvStatus == null || doc == null) return;
        com.hafiztraveltours.app.utils.DocumentStatus status =
                com.hafiztraveltours.app.utils.DocumentStatus.from(doc.status);

        // Reset rejection container visibility by default
        if (rejectionContainer != null) rejectionContainer.setVisibility(View.GONE);

        if (status == com.hafiztraveltours.app.utils.DocumentStatus.PENDING) {
            tvStatus.setText(getString(R.string.doc_status_under_review));
            tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
            tvStatus.setTextColor(getResources().getColor(R.color.gold_accent));
            if (btnAction != null) {
                btnAction.setText(getString(R.string.doc_action_view));
                btnAction.setBackgroundResource(R.drawable.bg_button_white_square);
                btnAction.setTextColor(getResources().getColor(R.color.brand_magenta));
            }
            if (tvDates != null) {
                if (doc.submittedAt != null && !doc.submittedAt.isEmpty()) {
                    tvDates.setText(getString(R.string.doc_updated_at_format, doc.submittedAt));
                    tvDates.setVisibility(View.VISIBLE);
                } else {
                    tvDates.setVisibility(View.GONE);
                }
            }
            if (tvGuidance != null) {
                tvGuidance.setText(getString(R.string.doc_guidance_under_review));
                tvGuidance.setVisibility(View.VISIBLE);
            }
        } else if (status == com.hafiztraveltours.app.utils.DocumentStatus.VERIFIED) {
            tvStatus.setText(getString(R.string.doc_status_verified));
            tvStatus.setBackgroundResource(R.drawable.bg_status_verified);
            tvStatus.setTextColor(android.graphics.Color.parseColor("#047857"));
            if (btnAction != null) {
                btnAction.setText(getString(R.string.doc_action_view));
                btnAction.setBackgroundResource(R.drawable.bg_button_white_square);
                btnAction.setTextColor(getResources().getColor(R.color.brand_magenta));
            }
            if (tvDates != null) {
                String vDate = (doc.verifiedAt != null && !doc.verifiedAt.isEmpty()) ? doc.verifiedAt : doc.submittedAt;
                if (vDate != null && !vDate.isEmpty()) {
                    tvDates.setText(getString(R.string.doc_verified_at_format, vDate));
                    tvDates.setVisibility(View.VISIBLE);
                } else {
                    tvDates.setVisibility(View.GONE);
                }
            }
            if (tvGuidance != null) {
                tvGuidance.setText(getString(R.string.doc_guidance_verified));
                tvGuidance.setVisibility(View.VISIBLE);
            }
        } else if (status == com.hafiztraveltours.app.utils.DocumentStatus.REJECTED) {
            tvStatus.setText(getString(R.string.doc_status_rejected));
            tvStatus.setBackgroundResource(R.drawable.bg_status_rejected);
            tvStatus.setTextColor(android.graphics.Color.parseColor("#DC2626"));

            if (rejectionContainer != null && tvRejectionReason != null) {
                String reason = doc.rejectionReason != null && !doc.rejectionReason.isEmpty()
                        ? doc.rejectionReason : getString(R.string.doc_rejection_default);
                tvRejectionReason.setText(getString(R.string.doc_rejection_reason_prefix, reason));
                rejectionContainer.setVisibility(View.VISIBLE);
            }

            if (btnAction != null) {
                btnAction.setText(getString(R.string.doc_action_replace));
                btnAction.setBackgroundResource(R.drawable.bg_button_pink);
                btnAction.setTextColor(getResources().getColor(R.color.white));
            }
            if (tvDates != null) {
                if (doc.submittedAt != null && !doc.submittedAt.isEmpty()) {
                    tvDates.setText(getString(R.string.doc_updated_at_format, doc.submittedAt));
                    tvDates.setVisibility(View.VISIBLE);
                } else {
                    tvDates.setVisibility(View.GONE);
                }
            }
            if (tvGuidance != null) tvGuidance.setVisibility(View.GONE);
        } else if (status == com.hafiztraveltours.app.utils.DocumentStatus.EXPIRED) {
            tvStatus.setText(getString(R.string.doc_status_expired));
            tvStatus.setBackgroundResource(R.drawable.bg_status_rejected);
            tvStatus.setTextColor(android.graphics.Color.parseColor("#DC2626"));
            if (btnAction != null) {
                btnAction.setText(getString(R.string.doc_action_replace));
                btnAction.setBackgroundResource(R.drawable.bg_button_pink);
                btnAction.setTextColor(getResources().getColor(R.color.white));
            }
            if (tvDates != null) tvDates.setVisibility(View.GONE);
            if (tvGuidance != null) tvGuidance.setVisibility(View.GONE);
        } else if (status == com.hafiztraveltours.app.utils.DocumentStatus.NOT_REQUIRED) {
            tvStatus.setText(getString(R.string.doc_status_not_required));
            tvStatus.setBackgroundResource(R.drawable.bg_status_not_uploaded);
            tvStatus.setTextColor(getResources().getColor(R.color.text_gray));
            if (btnAction != null) {
                btnAction.setText(getString(R.string.doc_action_upload));
                btnAction.setBackgroundResource(R.drawable.bg_button_pink);
                btnAction.setTextColor(getResources().getColor(R.color.white));
            }
            if (tvDates != null) tvDates.setVisibility(View.GONE);
            if (tvGuidance != null) tvGuidance.setVisibility(View.GONE);
        } else {
            tvStatus.setText(getString(R.string.doc_status_not_uploaded));
            tvStatus.setBackgroundResource(R.drawable.bg_status_not_uploaded);
            tvStatus.setTextColor(getResources().getColor(R.color.text_gray));
            if (btnAction != null) {
                btnAction.setText(getString(R.string.doc_action_upload));
                btnAction.setBackgroundResource(R.drawable.bg_button_pink);
                btnAction.setTextColor(getResources().getColor(R.color.white));
            }
            if (tvDates != null) tvDates.setVisibility(View.GONE);
            if (tvGuidance != null) tvGuidance.setVisibility(View.GONE);
        }
    }

    private void showChangePasswordDialog() {
        if (!profileViewModel.isLoggedIn()) {
            Toast.makeText(this, getString(R.string.profile_login_to_update), Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);

        TextInputEditText currentPasswordInput = dialogView.findViewById(R.id.currentPasswordInput);
        TextInputEditText newPasswordInput = dialogView.findViewById(R.id.newPasswordInput);
        TextInputEditText confirmNewPasswordInput = dialogView.findViewById(R.id.confirmNewPasswordInput);

        TextInputLayout currentPasswordLayout = dialogView.findViewById(R.id.currentPasswordLayout);
        TextInputLayout newPasswordLayout = dialogView.findViewById(R.id.newPasswordLayout);
        TextInputLayout confirmNewPasswordLayout = dialogView.findViewById(R.id.confirmNewPasswordLayout);

        View passwordReqLayout = dialogView.findViewById(R.id.passwordRequirementsLayout);
        if (passwordReqLayout != null && newPasswordInput != null) {
            com.hafiztraveltours.app.utils.PasswordChecklistHelper checklistHelper =
                    new com.hafiztraveltours.app.utils.PasswordChecklistHelper(passwordReqLayout);
            checklistHelper.attachToInput(newPasswordInput);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View btnClose = dialogView.findViewById(R.id.btnClosePasswordDialog);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        View btnCancel = dialogView.findViewById(R.id.btnCancelPasswordChange);
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        TextView btnSave = dialogView.findViewById(R.id.btnSavePasswordChange);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String curPass = currentPasswordInput != null ? currentPasswordInput.getText().toString() : "";
                String newPass = newPasswordInput != null ? newPasswordInput.getText().toString() : "";
                String confirmPass = confirmNewPasswordInput != null ? confirmNewPasswordInput.getText().toString() : "";

                if (currentPasswordLayout != null) currentPasswordLayout.setError(null);
                if (newPasswordLayout != null) newPasswordLayout.setError(null);
                if (confirmNewPasswordLayout != null) confirmNewPasswordLayout.setError(null);

                ProfileViewModel.PasswordErrors pwdErrors =
                        profileViewModel.validatePasswordForm(curPass, newPass, confirmPass);
                if (pwdErrors.currentErr != 0) {
                    if (currentPasswordLayout != null) currentPasswordLayout.setError(getString(pwdErrors.currentErr));
                    return;
                }
                if (pwdErrors.newErr != 0) {
                    if (newPasswordLayout != null) newPasswordLayout.setError(getString(pwdErrors.newErr));
                    return;
                }
                if (pwdErrors.confirmErr != 0) {
                    if (confirmNewPasswordLayout != null) confirmNewPasswordLayout.setError(getString(pwdErrors.confirmErr));
                    return;
                }

                btnSave.setEnabled(false);
                btnSave.setText(getString(R.string.password_updating));

                PendingOpUi pending = new PendingOpUi();
                pending.dialog = dialog;
                pending.btnSave = btnSave;
                pending.errorLayout = currentPasswordLayout;
                pending.saveTextRes = R.string.btn_update_password;
                pendingPasswordUi = pending;

                profileViewModel.changePassword(curPass, newPass, confirmPass);
            });
        }

        dialog.show();
    }

    /** Renders the change-password one-shot result (button + error/toast + dismiss). */
    private void handlePasswordResult(com.hafiztraveltours.app.utils.ApiOpResult result) {
        PendingOpUi pending = pendingPasswordUi;
        pendingPasswordUi = null;
        if (pending == null || pending.dialog == null || !pending.dialog.isShowing()) return;
        if (pending.btnSave != null) {
            pending.btnSave.setEnabled(true);
            pending.btnSave.setText(getString(pending.saveTextRes));
        }
        if (result.success) {
            pending.dialog.dismiss();
            Toast.makeText(this, getString(R.string.password_updated_success), Toast.LENGTH_SHORT).show();
            return;
        }
        String errorMsg;
        if (result.errorResponse != null) {
            errorMsg = com.hafiztraveltours.app.network.ApiErrors.userMessage(
                    this, result.errorResponse, result.fallbackResId);
        } else {
            errorMsg = com.hafiztraveltours.app.network.ApiErrors.userMessage(
                    this, result.error, result.fallbackResId);
        }
        if (pending.errorLayout != null) {
            pending.errorLayout.setError(errorMsg);
        } else {
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSelectedFileUri(android.net.Uri uri) {
        if (uri == null) return;

        long fileSize = 0;
        String fileName = "selected_file";
        String mimeType = getContentResolver().getType(uri);

        try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex);
                if (nameIndex != -1) fileName = cursor.getString(nameIndex);
            }
        } catch (Exception ignored) {}

        if (fileSize == 0) {
            try {
                android.os.ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
                if (pfd != null) {
                    fileSize = pfd.getStatSize();
                    pfd.close();
                }
            } catch (Exception ignored) {}
        }

        if (fileName == null || fileName.isEmpty()) {
            fileName = uri.getLastPathSegment();
        }

        boolean isPhotoOnly = "passport_photo".equalsIgnoreCase(pendingUploadDocCode);
        int fileErr = profileViewModel.validateUploadFile(fileName, mimeType, fileSize, isPhotoOnly);
        if (fileErr != 0) {
            Toast.makeText(this, getString(fileErr), Toast.LENGTH_LONG).show();
            return;
        }

        selectedFileUri = uri;
        if (pendingFileNameView != null) {
            pendingFileNameView.setText(fileName);
            View parentContainer = (View) pendingFileNameView.getParent();
            if (parentContainer != null) parentContainer.setVisibility(View.VISIBLE);
        }
        if (pendingConfirmButton != null) {
            pendingConfirmButton.setAlpha(1.0f);
            pendingConfirmButton.setEnabled(true);
        }
    }

    private void showUploadDocumentDialog(String docCode, String docTitle, int iconRes) {
        pendingUploadDocCode = docCode;
        pendingUploadDocName = docTitle;
        selectedFileUri = null;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_upload_document, null);

        ImageView uploadIcon = dialogView.findViewById(R.id.uploadIcon);
        TextView tvUploadTitle = dialogView.findViewById(R.id.tvUploadTitle);
        TextView tvUploadDocName = dialogView.findViewById(R.id.tvUploadDocName);
        TextView tvUploadFormats = dialogView.findViewById(R.id.tvUploadFormats);
        TextView btnChooseFile = dialogView.findViewById(R.id.btnChooseFile);
        TextView btnConfirmUpload = dialogView.findViewById(R.id.btnConfirmUpload);
        View btnCloseUpload = dialogView.findViewById(R.id.btnCloseUpload);
        View btnCancelUpload = dialogView.findViewById(R.id.btnCancelUpload);
        pendingFileNameView = dialogView.findViewById(R.id.tvSelectedFileName);
        pendingConfirmButton = btnConfirmUpload;

        if (uploadIcon != null && iconRes != 0) uploadIcon.setImageResource(iconRes);
        if (tvUploadDocName != null) tvUploadDocName.setText(docTitle);
        if ("passport_photo".equalsIgnoreCase(docCode) && tvUploadFormats != null) {
            tvUploadFormats.setText(getString(R.string.upload_accepted_formats_photo));
        }

        if (btnConfirmUpload != null) {
            btnConfirmUpload.setAlpha(0.5f);
            btnConfirmUpload.setEnabled(false);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        if (btnCloseUpload != null) btnCloseUpload.setOnClickListener(v -> dialog.dismiss());
        if (btnCancelUpload != null) btnCancelUpload.setOnClickListener(v -> dialog.dismiss());

        if (btnChooseFile != null) {
            btnChooseFile.setOnClickListener(v -> {
                if ("passport_photo".equalsIgnoreCase(docCode)) {
                    docPickerLauncher.launch("image/*");
                } else {
                    docPickerLauncher.launch("*/*");
                }
            });
        }

        if (btnConfirmUpload != null) {
            btnConfirmUpload.setOnClickListener(v -> {
                if (selectedFileUri == null) {
                    Toast.makeText(ProfileActivity.this, getString(R.string.err_file_empty), Toast.LENGTH_SHORT).show();
                    return;
                }
                dialog.dismiss();
                performDocumentUpload(pendingUploadDocCode, selectedFileUri);
            });
        }

        dialog.show();
    }

    private void performDocumentUpload(String docCode, android.net.Uri uri) {
        if (uri == null) return;
        Toast.makeText(this, getString(R.string.doc_upload_processing), Toast.LENGTH_SHORT).show();

        String fileName = "document_" + System.currentTimeMillis();
        try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) fileName = cursor.getString(nameIndex);
            }
        } catch (Exception ignored) {}

        String mimeType = getContentResolver().getType(uri);
        if (mimeType == null) mimeType = "application/octet-stream";

        profileViewModel.uploadDocument(docCode, uri, fileName, mimeType);
    }

    /** Renders the upload one-shot result (toast + refresh, same sequence as before). */
    private void handleUploadResult(com.hafiztraveltours.app.utils.ApiOpResult result) {
        if (result.success) {
            Toast.makeText(this, getString(R.string.doc_upload_success), Toast.LENGTH_SHORT).show();
            profileViewModel.loadStats();
            showTravelDocsBottomSheet();
            return;
        }
        String message;
        if (result.errorResponse != null) {
            message = com.hafiztraveltours.app.network.ApiErrors.userMessage(
                    this, result.errorResponse, result.fallbackResId);
        } else {
            message = com.hafiztraveltours.app.network.ApiErrors.userMessage(
                    this, result.error, result.fallbackResId);
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void openUploadedDocument(String docCode) {
        DocumentDto doc = userDocumentsMap.get(docCode != null ? docCode.toLowerCase() : "");
        if (doc == null || doc.filePath == null || doc.filePath.isEmpty()) {
            Toast.makeText(this, getString(R.string.doc_file_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        String url = doc.filePath;
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            String baseUrl = ApiClient.BASE_URL;
            if (baseUrl.endsWith("/api/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 5);
            } else if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            if (!url.startsWith("/")) {
                url = "/" + url;
            }
            url = baseUrl + url;
        }

        try {
            android.net.Uri uri = android.net.Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.doc_open_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void showDocRequirementsDialog(String docCode, String docTitle, int iconRes) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_doc_requirements, null);

        ImageView reqIcon = dialogView.findViewById(R.id.reqIcon);
        TextView tvReqTitle = dialogView.findViewById(R.id.tvReqTitle);
        TextView tvReqAcceptedFormats = dialogView.findViewById(R.id.tvReqAcceptedFormats);
        LinearLayout reqListContainer = dialogView.findViewById(R.id.reqListContainer);
        View btnCloseReq = dialogView.findViewById(R.id.btnCloseReq);
        View btnGotItReq = dialogView.findViewById(R.id.btnGotItReq);

        if (reqIcon != null && iconRes != 0) reqIcon.setImageResource(iconRes);
        if (tvReqTitle != null) tvReqTitle.setText(docTitle);

        if ("passport_photo".equalsIgnoreCase(docCode) && tvReqAcceptedFormats != null) {
            tvReqAcceptedFormats.setText(getString(R.string.upload_accepted_formats_photo));
        }

        String[] items;
        if ("passport".equalsIgnoreCase(docCode)) {
            items = new String[]{
                    getString(R.string.req_passport_1),
                    getString(R.string.req_passport_2),
                    getString(R.string.req_passport_3),
                    getString(R.string.req_passport_4)
            };
        } else if ("passport_photo".equalsIgnoreCase(docCode)) {
            items = new String[]{
                    getString(R.string.req_photo_1),
                    getString(R.string.req_photo_2),
                    getString(R.string.req_photo_3)
            };
        } else {
            items = new String[]{
                    getString(R.string.req_visa_1),
                    getString(R.string.req_visa_2),
                    getString(R.string.req_visa_3)
            };
        }

        if (reqListContainer != null) {
            reqListContainer.removeAllViews();
            for (String item : items) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                row.setPadding(0, 0, 0, Math.round(8 * getResources().getDisplayMetrics().density));

                ImageView checkIcon = new ImageView(this);
                int iconSize = Math.round(18 * getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(iconSize, iconSize);
                checkParams.rightMargin = Math.round(10 * getResources().getDisplayMetrics().density);
                checkIcon.setLayoutParams(checkParams);
                checkIcon.setImageResource(R.drawable.ic_check_circle_magenta);
                row.addView(checkIcon);

                TextView itemText = new TextView(this);
                itemText.setText(item);
                itemText.setTextColor(getResources().getColor(R.color.text_dark));
                itemText.setTextSize(12);
                row.addView(itemText);

                reqListContainer.addView(row);
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        if (btnCloseReq != null) btnCloseReq.setOnClickListener(v -> dialog.dismiss());
        if (btnGotItReq != null) btnGotItReq.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
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