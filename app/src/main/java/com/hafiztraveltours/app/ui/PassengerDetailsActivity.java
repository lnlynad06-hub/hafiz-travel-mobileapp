package com.hafiztraveltours.app.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import android.text.Editable;
import android.text.TextWatcher;

import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.adapters.CountryDropdownAdapter;
import com.hafiztraveltours.app.models.BookingRequest;
import com.hafiztraveltours.app.models.PackageDetail;
import com.hafiztraveltours.app.network.UserDto;
import com.hafiztraveltours.app.utils.HapticUtil;
import com.hafiztraveltours.app.utils.LocaleHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PassengerDetailsActivity extends BaseActivity {

    public static final String EXTRA_BOOKING_REQUEST = "extra_booking_request";

    private BookingRequest bookingRequest;
    private TextView txtPackageName;
    private TextView txtDepartureDate;
    private TextView txtRoomAndPax;
    private TextView txtTotalAmount;
    private TextView txtPaxCountFooter;

    private LinearLayout leadProfileContainer;
    private LinearLayout additionalTravellersContainer;
    private View btnAddTravellerCard;
    private TextView txtNoAdditionalTravellers;

    private boolean isLeadProfileComplete = false;
    private List<PassengerDetailsViewModel.MissingField> missingFields = new ArrayList<>();
    private UserDto currentUserProfile = null;
    private PassengerDetailsViewModel.LeadEvaluation currentEvaluation = null;
    private PassengerDetailsViewModel passengerViewModel;

    // Additional Travellers list
    private final List<AdditionalTravellerHolder> additionalTravellers = new ArrayList<>();

    private static class AdditionalTravellerHolder {
        int index; // 1-based index among additional travellers (2, 3...)
        boolean isExpanded = true;

        View cardView;
        View headerRow;
        TextView titleText;
        TextView txtNamePreview;
        ImageView expandIcon;
        View btnDelete;
        View expandableBody;

        TextInputEditText inputName;
        TextView errorName;

        View fieldDobContainer;
        TextView tvDobValue;
        TextView tvDobAge;
        String selectedDob = null;

        View fieldGenderContainer;
        ImageView ivGenderBadge;
        TextView tvGenderValue;
        String selectedGender = null;

        View fieldNationalityContainer;
        TextView tvNationalityFlag;
        TextView tvNationalityValue;
        String selectedNationality = null;

        View layoutIcSection;
        TextInputEditText inputIc;
        TextView errorIc;

        View layoutPassportSection;
        TextInputEditText inputPassport;
        TextView errorPassport;
        View fieldPassportExpiryContainer;
        TextView tvPassportExpiryValue;
        TextView errorPassportExpiry;
        String selectedPassportExpiry = null;
        View fieldIssuingCountryContainer;
        TextView tvIssuingCountryFlag;
        TextView tvIssuingCountryValue;
        String selectedIssuingCountry = null;

        View layoutExtraSection;
        View layoutClothesSizeSection;
        MaterialAutoCompleteTextView inputClothesSize;

        View layoutMahramSection;
        MaterialAutoCompleteTextView inputMahram;
        TextInputEditText inputRelationship;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_details);

        findViewById(R.id.passengerBackButton).setOnClickListener(v -> finish());

        bookingRequest = (BookingRequest) getIntent().getSerializableExtra(EXTRA_BOOKING_REQUEST);

        if (bookingRequest == null) {
            finish();
            return;
        }

        txtPackageName = findViewById(R.id.passengerPackageName);
        txtDepartureDate = findViewById(R.id.passengerDepartureDate);
        txtRoomAndPax = findViewById(R.id.passengerRoomAndPax);
        txtTotalAmount = findViewById(R.id.passengerTotalAmount);
        txtPaxCountFooter = findViewById(R.id.txtPaxCountFooter);

        leadProfileContainer = findViewById(R.id.leadProfileContainer);
        additionalTravellersContainer = findViewById(R.id.additionalTravellersContainer);
        btnAddTravellerCard = findViewById(R.id.btnAddTravellerCard);
        txtNoAdditionalTravellers = findViewById(R.id.txtNoAdditionalTravellers);

        txtPackageName.setText(bookingRequest.packageName);
        if (bookingRequest.selectedDepartureDate != null && !bookingRequest.selectedDepartureDate.trim().isEmpty()) {
            txtDepartureDate.setText(getString(R.string.passenger_departure_format, bookingRequest.selectedDepartureDate.trim()));
            txtDepartureDate.setVisibility(View.VISIBLE);
        } else {
            txtDepartureDate.setVisibility(View.GONE);
        }

        txtRoomAndPax.setText(getString(R.string.passenger_room_pax_format, bookingRequest.roomLabel, bookingRequest.adultPaxCount));
        txtTotalAmount.setText(bookingRequest.totalAmountFormatted);

        btnAddTravellerCard.setOnClickListener(v -> {
            com.hafiztraveltours.app.utils.HapticUtil.click(v);
            addAdditionalTraveller();
        });

        passengerViewModel = new androidx.lifecycle.ViewModelProvider(this)
                .get(PassengerDetailsViewModel.class);
        observePassengerState();

        findViewById(R.id.btnProceedToReview).setOnClickListener(v -> {
            com.hafiztraveltours.app.utils.HapticUtil.click(v);
            validateAndProceed();
        });

        // Initialize required additional traveller count based on adultPaxCount
        int extraNeeded = Math.max(0, bookingRequest.adultPaxCount - 1);
        for (int i = 0; i < extraNeeded; i++) {
            addAdditionalTraveller();
        }
        updateFooterCount();
    }



    @Override
    protected void onResume() {
        super.onResume();
        // Re-check after returning from Edit Profile (rule E).
        passengerViewModel.loadProfile(bookingRequest != null ? bookingRequest.packageDetail : null);
    }

    /** Wires ViewModel state to rendering (H1/Phase 8). */
    private void observePassengerState() {
        passengerViewModel.getProfileData().observe(this, user -> {
            currentUserProfile = user;
        });
        passengerViewModel.getEvaluation().observe(this, eval -> {
            if (eval == null) return;
            currentEvaluation = eval;
            isLeadProfileComplete = eval.complete;
            missingFields = eval.missing != null ? eval.missing : new ArrayList<>();
            renderLeadProfileState();
        });
    }

    private void renderLeadProfileState() {
        leadProfileContainer.removeAllViews();

        if (isLeadProfileComplete && currentUserProfile != null) {
            // COMPLETE PROFILE UI
            TextView txtBadge = new TextView(this);
            txtBadge.setText(getString(R.string.passenger_profile_complete));
            txtBadge.setTextSize(11);
            txtBadge.setTypeface(null, Typeface.BOLD);
            txtBadge.setTextColor(Color.parseColor("#047857"));
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setShape(GradientDrawable.RECTANGLE);
            badgeBg.setCornerRadius(dp(6));
            badgeBg.setColor(Color.parseColor("#D1FAE5"));
            txtBadge.setBackground(badgeBg);
            txtBadge.setPadding(dp(8), dp(4), dp(8), dp(4));
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bp.bottomMargin = dp(10);
            txtBadge.setLayoutParams(bp);
            leadProfileContainer.addView(txtBadge);

            TextView txtName = new TextView(this);
            txtName.setText(currentUserProfile.name);
            txtName.setTextSize(15);
            txtName.setTypeface(null, Typeface.BOLD);
            txtName.setTextColor(getResources().getColor(R.color.text_dark));
            leadProfileContainer.addView(txtName);

            TextView txtDetails = new TextView(this);
            StringBuilder sb = new StringBuilder();
            if (currentUserProfile.passportNumber != null && !currentUserProfile.passportNumber.trim().isEmpty()) {
                sb.append(getString(R.string.passenger_passport_label, maskPassport(currentUserProfile.passportNumber))).append("\n");
            }
            String nationality = (currentUserProfile.nationality != null && !currentUserProfile.nationality.trim().isEmpty())
                    ? currentUserProfile.nationality : "Malaysia";
            sb.append(getString(R.string.passenger_nationality_label, nationality));

            txtDetails.setText(sb.toString());
            txtDetails.setTextSize(12);
            txtDetails.setTextColor(getResources().getColor(R.color.text_gray));
            LinearLayout.LayoutParams dpParam = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dpParam.topMargin = dp(4);
            txtDetails.setLayoutParams(dpParam);
            leadProfileContainer.addView(txtDetails);

            // Document statuses
            LinearLayout docStatusRow = new LinearLayout(this);
            docStatusRow.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams drp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            drp.topMargin = dp(10);
            docStatusRow.setLayoutParams(drp);

            PackageDetail pkg = bookingRequest.packageDetail;
            boolean reqPassport = pkg != null ? pkg.requiresPassport : true;
            boolean reqIc = pkg != null ? pkg.requiresIc : true;

            if (reqPassport) {
                boolean hasPassport = currentEvaluation != null && currentEvaluation.hasPassportDoc;
                TextView passTag = new TextView(this);
                passTag.setText(hasPassport
                        ? getString(R.string.passenger_doc_passport_available)
                        : getString(R.string.passenger_doc_passport_pending));
                passTag.setTextSize(11);
                passTag.setTextColor(hasPassport ? Color.parseColor("#047857") : Color.parseColor("#D97706"));
                docStatusRow.addView(passTag);

                boolean hasPhoto = currentEvaluation != null && currentEvaluation.hasPhotoDoc;
                TextView photoTag = new TextView(this);
                photoTag.setText(hasPhoto
                        ? getString(R.string.passenger_doc_photo_available)
                        : getString(R.string.passenger_doc_photo_pending));
                photoTag.setTextSize(11);
                photoTag.setTextColor(hasPhoto ? Color.parseColor("#047857") : Color.parseColor("#D97706"));
                LinearLayout.LayoutParams photoLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                photoLp.topMargin = dp(2);
                photoTag.setLayoutParams(photoLp);
                docStatusRow.addView(photoTag);

                if (currentEvaluation != null && currentEvaluation.hasPassportValidityWarning) {
                    TextView validityTag = new TextView(this);
                    validityTag.setText(getString(R.string.passenger_passport_validity_info, currentEvaluation.reqValidityMonths));
                    validityTag.setTextSize(11);
                    validityTag.setTextColor(Color.parseColor("#D97706"));
                    LinearLayout.LayoutParams vLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    vLp.topMargin = dp(2);
                    validityTag.setLayoutParams(vLp);
                    docStatusRow.addView(validityTag);
                } else if (currentEvaluation != null && currentEvaluation.hasPassportExpiryMissing) {
                    TextView expiryTag = new TextView(this);
                    expiryTag.setText(getString(R.string.passenger_passport_expiry_pending));
                    expiryTag.setTextSize(11);
                    expiryTag.setTextColor(Color.parseColor("#D97706"));
                    LinearLayout.LayoutParams eLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    eLp.topMargin = dp(2);
                    expiryTag.setLayoutParams(eLp);
                    docStatusRow.addView(expiryTag);
                }

                TextView visaTag = new TextView(this);
                visaTag.setText(getString(R.string.passenger_doc_visa_info));
                visaTag.setTextSize(11);
                visaTag.setTextColor(Color.parseColor("#6B7280"));
                LinearLayout.LayoutParams visaLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                visaLp.topMargin = dp(2);
                visaTag.setLayoutParams(visaLp);
                docStatusRow.addView(visaTag);
            }
            if (reqIc) {
                boolean hasIc = currentEvaluation != null && currentEvaluation.hasIcDoc;
                if (hasIc) {
                    TextView icTag = new TextView(this);
                    icTag.setText(getString(R.string.passenger_doc_ic_available));
                    icTag.setTextSize(11);
                    icTag.setTextColor(Color.parseColor("#047857"));
                    LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    icLp.topMargin = dp(2);
                    icTag.setLayoutParams(icLp);
                    docStatusRow.addView(icTag);
                }
            }
            leadProfileContainer.addView(docStatusRow);

            // Edit Profile Button
            TextView btnEdit = new TextView(this);
            btnEdit.setText(getString(R.string.passenger_btn_edit_profile));
            btnEdit.setTextSize(12);
            btnEdit.setTypeface(null, Typeface.BOLD);
            btnEdit.setTextColor(getResources().getColor(R.color.brand_magenta));
            btnEdit.setBackgroundResource(R.drawable.bg_input_box);
            btnEdit.setPadding(dp(12), dp(8), dp(12), dp(8));
            btnEdit.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ep.topMargin = dp(12);
            btnEdit.setLayoutParams(ep);
            btnEdit.setOnClickListener(v -> openEditProfileScreen());
            leadProfileContainer.addView(btnEdit);

        } else {
            // INCOMPLETE PROFILE UI
            TextView txtBadge = new TextView(this);
            txtBadge.setText(getString(R.string.passenger_profile_incomplete));
            txtBadge.setTextSize(11);
            txtBadge.setTypeface(null, Typeface.BOLD);
            txtBadge.setTextColor(Color.parseColor("#B91C1C"));
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setShape(GradientDrawable.RECTANGLE);
            badgeBg.setCornerRadius(dp(6));
            badgeBg.setColor(Color.parseColor("#FEE2E2"));
            txtBadge.setBackground(badgeBg);
            txtBadge.setPadding(dp(8), dp(4), dp(8), dp(4));
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bp.bottomMargin = dp(8);
            txtBadge.setLayoutParams(bp);
            leadProfileContainer.addView(txtBadge);

            TextView txtWarn = new TextView(this);
            txtWarn.setText(getString(R.string.passenger_profile_incomplete_warn));
            txtWarn.setTextSize(12);
            txtWarn.setTextColor(getResources().getColor(R.color.text_dark));
            leadProfileContainer.addView(txtWarn);

            TextView txtMissingHeader = new TextView(this);
            txtMissingHeader.setText(getString(R.string.passenger_missing_fields_header));
            txtMissingHeader.setTextSize(11);
            txtMissingHeader.setTypeface(null, Typeface.BOLD);
            txtMissingHeader.setTextColor(Color.parseColor("#B91C1C"));
            LinearLayout.LayoutParams mhp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mhp.topMargin = dp(10);
            txtMissingHeader.setLayoutParams(mhp);
            leadProfileContainer.addView(txtMissingHeader);

            StringBuilder sb = new StringBuilder();
            for (PassengerDetailsViewModel.MissingField field : missingFields) {
                if (field == null || field.kind == null) continue;
                String label;
                switch (field.kind) {
                    case NOT_LOGGED_IN:
                        label = getString(R.string.passenger_not_logged_in);
                        break;
                    case NAME:
                        label = getString(R.string.passenger_missing_name);
                        break;
                    case IC:
                        label = getString(R.string.passenger_missing_ic);
                        break;
                    case PASSPORT:
                        label = getString(R.string.passenger_missing_passport);
                        break;
                    case PASSPORT_EXPIRY:
                        label = getString(R.string.passenger_missing_passport_expiry);
                        break;
                    case PASSPORT_VALIDITY:
                        label = getString(R.string.passenger_missing_passport_validity, field.validityMonths);
                        break;
                    case PASSPORT_DOC:
                        label = getString(R.string.passenger_missing_passport_doc);
                        break;
                    case IC_DOC:
                        label = getString(R.string.passenger_missing_ic_doc);
                        break;
                    case CLOTHES_SIZE:
                    default:
                        label = getString(R.string.passenger_missing_clothes_size);
                        break;
                }
                sb.append("• ").append(label).append("\n");
            }
            TextView txtMissingList = new TextView(this);
            txtMissingList.setText(sb.toString().trim());
            txtMissingList.setTextSize(11);
            txtMissingList.setTextColor(Color.parseColor("#991B1B"));
            LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mlp.topMargin = dp(4);
            txtMissingList.setLayoutParams(mlp);
            leadProfileContainer.addView(txtMissingList);

            // COMPLETE PROFILE CTA BUTTON
            LinearLayout btnComplete = new LinearLayout(this);
            btnComplete.setOrientation(LinearLayout.HORIZONTAL);
            btnComplete.setBackgroundResource(R.drawable.bg_booking_primary_button);
            btnComplete.setGravity(android.view.Gravity.CENTER);
            btnComplete.setPadding(dp(16), dp(10), dp(16), dp(10));
            LinearLayout.LayoutParams cbp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cbp.topMargin = dp(14);
            btnComplete.setLayoutParams(cbp);

            TextView btnText = new TextView(this);
            btnText.setText(getString(R.string.passenger_btn_complete_profile));
            btnText.setTextSize(12);
            btnText.setTypeface(null, Typeface.BOLD);
            btnText.setTextColor(Color.WHITE);
            btnComplete.addView(btnText);

            btnComplete.setOnClickListener(v -> openEditProfileScreen());
            leadProfileContainer.addView(btnComplete);
        }
    }

    private void openEditProfileScreen() {
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra(ProfileActivity.EXTRA_ACTION, ProfileActivity.ACTION_EDIT_PROFILE);
        startActivity(intent);
    }

    private String maskPassport(String passport) {
        if (passport == null || passport.length() <= 4) return "****";
        return passport.substring(0, 2) + "****" + passport.substring(passport.length() - 2);
    }

    private void addAdditionalTraveller() {
        PackageDetail pkg = bookingRequest.packageDetail;
        boolean reqPassport = pkg != null ? pkg.requiresPassport : true;
        boolean reqIc = pkg != null ? pkg.requiresIc : true;
        boolean reqMahram = pkg != null ? pkg.requiresMahram : (pkg != null && pkg.isUmrah);
        boolean reqClothesSize = pkg != null ? pkg.requiresClothesSize : (pkg != null && pkg.isUmrah);

        int index = additionalTravellers.size() + 2; // Traveller 2, 3...

        AdditionalTravellerHolder holder = new AdditionalTravellerHolder();
        holder.index = index;

        View card = getLayoutInflater().inflate(R.layout.item_additional_traveller_card, additionalTravellersContainer, false);
        holder.cardView = card;
        holder.headerRow = card.findViewById(R.id.headerRowTraveller);
        holder.titleText = card.findViewById(R.id.txtTravellerIndex);
        holder.txtNamePreview = card.findViewById(R.id.txtTravellerNamePreview);
        holder.btnDelete = card.findViewById(R.id.btnDeleteTraveller);
        holder.expandIcon = card.findViewById(R.id.imgExpandCollapse);
        holder.expandableBody = card.findViewById(R.id.layoutExpandableBody);

        holder.inputName = card.findViewById(R.id.inputName);
        holder.errorName = card.findViewById(R.id.errorName);

        holder.fieldDobContainer = card.findViewById(R.id.fieldDobContainer);
        holder.tvDobValue = card.findViewById(R.id.tvDobValue);
        holder.tvDobAge = card.findViewById(R.id.tvDobAge);

        holder.fieldGenderContainer = card.findViewById(R.id.fieldGenderContainer);
        holder.ivGenderBadge = card.findViewById(R.id.ivGenderBadge);
        holder.tvGenderValue = card.findViewById(R.id.tvGenderValue);

        holder.fieldNationalityContainer = card.findViewById(R.id.fieldNationalityContainer);
        holder.tvNationalityFlag = card.findViewById(R.id.tvNationalityFlag);
        holder.tvNationalityValue = card.findViewById(R.id.tvNationalityValue);

        holder.layoutIcSection = card.findViewById(R.id.layoutIcSection);
        holder.inputIc = card.findViewById(R.id.inputIc);
        holder.errorIc = card.findViewById(R.id.errorIc);

        holder.layoutPassportSection = card.findViewById(R.id.layoutPassportSection);
        holder.inputPassport = card.findViewById(R.id.inputPassport);
        holder.errorPassport = card.findViewById(R.id.errorPassport);
        holder.fieldPassportExpiryContainer = card.findViewById(R.id.fieldPassportExpiryContainer);
        holder.tvPassportExpiryValue = card.findViewById(R.id.tvPassportExpiryValue);
        holder.errorPassportExpiry = card.findViewById(R.id.errorPassportExpiry);
        holder.fieldIssuingCountryContainer = card.findViewById(R.id.fieldIssuingCountryContainer);
        holder.tvIssuingCountryFlag = card.findViewById(R.id.tvIssuingCountryFlag);
        holder.tvIssuingCountryValue = card.findViewById(R.id.tvIssuingCountryValue);

        holder.layoutExtraSection = card.findViewById(R.id.layoutExtraSection);
        holder.layoutClothesSizeSection = card.findViewById(R.id.layoutClothesSizeSection);
        holder.inputClothesSize = card.findViewById(R.id.inputClothesSize);

        holder.layoutMahramSection = card.findViewById(R.id.layoutMahramSection);
        holder.inputMahram = card.findViewById(R.id.inputMahram);
        holder.inputRelationship = card.findViewById(R.id.inputRelationship);

        // Configure Title
        holder.titleText.setText(getString(R.string.passenger_traveller_title_format, index));

        // Configure Name & Real-time preview
        holder.inputName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String val = s.toString().trim();
                if (val.isEmpty()) {
                    holder.txtNamePreview.setText(R.string.passenger_default_preview_name);
                } else {
                    holder.txtNamePreview.setText(val);
                }
                updateMahramDropdownOptions();
            }
        });

        // Configure Default Nationality (Malaysia)
        String defNationality = getString(R.string.default_nationality);
        holder.selectedNationality = defNationality;
        holder.tvNationalityValue.setText(defNationality);
        holder.tvNationalityValue.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
        holder.tvNationalityFlag.setText(CountryDropdownAdapter.getFlagForCountry(defNationality));
        holder.tvNationalityFlag.setVisibility(View.VISIBLE);

        // Configure Default Issuing Country (Malaysia)
        String defCountry = getString(R.string.default_country);
        holder.selectedIssuingCountry = defCountry;
        holder.tvIssuingCountryValue.setText(defCountry);
        holder.tvIssuingCountryValue.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
        holder.tvIssuingCountryFlag.setText(CountryDropdownAdapter.getFlagForCountry(defCountry));
        holder.tvIssuingCountryFlag.setVisibility(View.VISIBLE);

        // Configure Pickers & Bottom Sheets
        holder.fieldDobContainer.setOnClickListener(v -> showDobPickerBottomSheet(holder));
        holder.fieldGenderContainer.setOnClickListener(v -> showGenderPickerBottomSheet(holder));
        holder.fieldNationalityContainer.setOnClickListener(v -> showNationalityPickerBottomSheet(holder));
        holder.fieldPassportExpiryContainer.setOnClickListener(v -> showPassportExpiryPickerBottomSheet(holder));
        holder.fieldIssuingCountryContainer.setOnClickListener(v -> showIssuingCountryPickerBottomSheet(holder));

        // Package requirement visibility toggles
        if (!reqIc && holder.layoutIcSection != null) {
            holder.layoutIcSection.setVisibility(View.GONE);
        }

        if (!reqPassport && holder.layoutPassportSection != null) {
            holder.layoutPassportSection.setVisibility(View.GONE);
        }

        if (!reqClothesSize && holder.layoutClothesSizeSection != null) {
            holder.layoutClothesSizeSection.setVisibility(View.GONE);
        } else if (holder.inputClothesSize != null) {
            holder.inputClothesSize.setInputType(InputType.TYPE_NULL);
            holder.inputClothesSize.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new String[]{"S", "M", "L", "XL", "2XL", "3XL", "4XL", "Custom"}));
            holder.inputClothesSize.setOnClickListener(v -> holder.inputClothesSize.showDropDown());
        }

        if (!reqMahram && holder.layoutMahramSection != null) {
            holder.layoutMahramSection.setVisibility(View.GONE);
        } else if (holder.inputMahram != null) {
            holder.inputMahram.setInputType(InputType.TYPE_NULL);
            holder.inputMahram.setOnClickListener(v -> holder.inputMahram.showDropDown());
        }

        if (!reqClothesSize && !reqMahram && holder.layoutExtraSection != null) {
            holder.layoutExtraSection.setVisibility(View.GONE);
        }

        // Delete button
        holder.btnDelete.setOnClickListener(v -> {
            additionalTravellersContainer.removeView(card);
            additionalTravellers.remove(holder);
            reindexAdditionalTravellers();
            updateMahramDropdownOptions();
            updateFooterCount();
        });

        // Expand / Collapse Header
        holder.headerRow.setOnClickListener(v -> {
            HapticUtil.click(v);
            holder.isExpanded = !holder.isExpanded;
            holder.expandableBody.setVisibility(holder.isExpanded ? View.VISIBLE : View.GONE);
            holder.expandIcon.setRotation(holder.isExpanded ? 0 : 180);
        });

        additionalTravellers.add(holder);
        additionalTravellersContainer.addView(card);

        if (txtNoAdditionalTravellers != null) {
            txtNoAdditionalTravellers.setVisibility(View.GONE);
        }

        updateMahramDropdownOptions();
        updateFooterCount();
    }

    private void showDobPickerBottomSheet(AdditionalTravellerHolder holder) {
        String[] dateHolder = new String[]{holder.selectedDob != null ? holder.selectedDob : ""};
        showDatePickerBottomSheet(
                getString(R.string.profile_dob_sheet_title),
                getString(R.string.profile_dob_sheet_sub),
                false,
                holder.tvDobValue,
                holder.tvDobAge,
                dateHolder,
                () -> holder.selectedDob = dateHolder[0]
        );
    }

    private void showPassportExpiryPickerBottomSheet(AdditionalTravellerHolder holder) {
        String[] dateHolder = new String[]{holder.selectedPassportExpiry != null ? holder.selectedPassportExpiry : ""};
        showDatePickerBottomSheet(
                getString(R.string.profile_passport_expiry_sheet_title),
                getString(R.string.profile_passport_expiry_sheet_sub),
                true,
                holder.tvPassportExpiryValue,
                null,
                dateHolder,
                () -> {
                    holder.selectedPassportExpiry = dateHolder[0];
                    if (holder.errorPassportExpiry != null) {
                        holder.errorPassportExpiry.setVisibility(View.GONE);
                    }
                }
        );
    }

    private void showDatePickerBottomSheet(
            String title,
            String subtitle,
            boolean isExpiryPicker,
            TextView tvTargetValue,
            TextView tvTargetAge,
            String[] selectedDateHolder,
            Runnable onDateSelected) {

        BottomSheetDialog sheetDialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_dob_picker, null);
        sheetDialog.setContentView(sheetView);

        if (sheetDialog.getWindow() != null) {
            sheetDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            sheetDialog.getWindow().setDimAmount(0.55f);
        }

        View btnClose = sheetView.findViewById(R.id.btnCloseDobSheet);
        if (btnClose != null) btnClose.setOnClickListener(v -> sheetDialog.dismiss());

        TextView tvSheetTitle = sheetView.findViewById(R.id.tvDobSheetTitle);
        if (tvSheetTitle != null && title != null && !title.isEmpty()) {
            tvSheetTitle.setText(title);
        }

        TextView tvSheetSub = sheetView.findViewById(R.id.tvDobSheetSub);
        if (tvSheetSub != null && subtitle != null && !subtitle.isEmpty()) {
            tvSheetSub.setText(subtitle);
        }

        TextView tvPreview = sheetView.findViewById(R.id.tvDobPreviewText);
        TextView tvPreviewAge = sheetView.findViewById(R.id.tvDobPreviewAge);
        NumberPicker npDay = sheetView.findViewById(R.id.npDobDay);
        NumberPicker npMonth = sheetView.findViewById(R.id.npDobMonth);
        NumberPicker npYear = sheetView.findViewById(R.id.npDobYear);
        View btnDone = sheetView.findViewById(R.id.btnDoneDob);

        Calendar today = Calendar.getInstance();
        int curYear = today.get(Calendar.YEAR);

        int minYear = isExpiryPicker ? (curYear - 10) : 1900;
        int maxYear = isExpiryPicker ? (curYear + 25) : curYear;

        int initYear = isExpiryPicker ? (curYear + 5) : (curYear - 26);
        int initMonth = isExpiryPicker ? today.get(Calendar.MONTH) : 0;
        int initDay = isExpiryPicker ? today.get(Calendar.DAY_OF_MONTH) : 1;

        String currentDateStr = selectedDateHolder != null && selectedDateHolder.length > 0 ? selectedDateHolder[0] : "";
        if (currentDateStr != null && !currentDateStr.trim().isEmpty()) {
            try {
                String[] parts = currentDateStr.trim().split("-");
                if (parts.length == 3) {
                    initYear = Integer.parseInt(parts[0]);
                    initMonth = Integer.parseInt(parts[1]) - 1;
                    initDay = Integer.parseInt(parts[2]);
                }
            } catch (Exception ignored) {}
        }

        boolean isMalay = "ms".equalsIgnoreCase(LocaleHelper.getSavedLanguage(this));
        String[] monthNames = isMalay
                ? new String[]{"Jan", "Feb", "Mac", "Apr", "Mei", "Jun", "Jul", "Ogo", "Sep", "Okt", "Nov", "Dis"}
                : new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        if (npMonth != null) {
            npMonth.setMinValue(0);
            npMonth.setMaxValue(11);
            npMonth.setDisplayedValues(monthNames);
            npMonth.setValue(Math.max(0, Math.min(11, initMonth)));
            npMonth.setWrapSelectorWheel(true);
        }

        if (npYear != null) {
            npYear.setMinValue(minYear);
            npYear.setMaxValue(maxYear);
            npYear.setValue(Math.max(minYear, Math.min(maxYear, initYear)));
            npYear.setWrapSelectorWheel(false);
        }

        Runnable updateDayMax = () -> {
            int y = npYear != null ? npYear.getValue() : curYear;
            int m = npMonth != null ? npMonth.getValue() : 0;
            Calendar tempCal = Calendar.getInstance();
            tempCal.set(Calendar.YEAR, y);
            tempCal.set(Calendar.MONTH, m);
            tempCal.set(Calendar.DAY_OF_MONTH, 1);
            int maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);
            if (npDay != null) {
                int oldVal = npDay.getValue();
                npDay.setMinValue(1);
                npDay.setMaxValue(maxDays);
                npDay.setValue(Math.max(1, Math.min(maxDays, oldVal)));
                npDay.setWrapSelectorWheel(true);
            }
        };

        updateDayMax.run();
        if (npDay != null) {
            npDay.setValue(Math.max(1, Math.min(31, initDay)));
        }

        Runnable updatePreview = () -> {
            int d = npDay != null ? npDay.getValue() : 1;
            int m = npMonth != null ? npMonth.getValue() : 0;
            int y = npYear != null ? npYear.getValue() : curYear;
            String iso = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d);
            String displayStr = ProfileActivity.formatDateDisplay(this, iso);
            if (tvPreview != null) {
                tvPreview.setText(displayStr);
            }
            if (isExpiryPicker) {
                if (tvPreviewAge != null) {
                    tvPreviewAge.setVisibility(View.GONE);
                }
            } else {
                int age = curYear - y;
                if (today.get(Calendar.MONTH) < m ||
                        (today.get(Calendar.MONTH) == m && today.get(Calendar.DAY_OF_MONTH) < d)) {
                    age--;
                }
                if (tvPreviewAge != null) {
                    if (age >= 0) {
                        tvPreviewAge.setText(getString(R.string.profile_age_format, age));
                        tvPreviewAge.setVisibility(View.VISIBLE);
                    } else {
                        tvPreviewAge.setVisibility(View.GONE);
                    }
                }
            }
        };

        updatePreview.run();

        NumberPicker.OnValueChangeListener changeListener = (picker, oldVal, newVal) -> {
            if (picker == npMonth || picker == npYear) {
                updateDayMax.run();
            }
            updatePreview.run();
        };

        if (npDay != null) npDay.setOnValueChangedListener(changeListener);
        if (npMonth != null) npMonth.setOnValueChangedListener(changeListener);
        if (npYear != null) npYear.setOnValueChangedListener(changeListener);

        if (btnDone != null) {
            btnDone.setOnClickListener(v -> {
                HapticUtil.click(v);
                int d = npDay != null ? npDay.getValue() : 1;
                int m = npMonth != null ? npMonth.getValue() : 0;
                int y = npYear != null ? npYear.getValue() : curYear;
                String iso = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d);
                if (selectedDateHolder != null && selectedDateHolder.length > 0) {
                    selectedDateHolder[0] = iso;
                }
                if (tvTargetValue != null) {
                    tvTargetValue.setText(ProfileActivity.formatDateDisplay(this, iso));
                    tvTargetValue.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
                }
                if (!isExpiryPicker && tvTargetAge != null) {
                    int age = curYear - y;
                    if (today.get(Calendar.MONTH) < m ||
                            (today.get(Calendar.MONTH) == m && today.get(Calendar.DAY_OF_MONTH) < d)) {
                        age--;
                    }
                    if (age >= 0) {
                        tvTargetAge.setText(getString(R.string.profile_age_format, age));
                        tvTargetAge.setVisibility(View.VISIBLE);
                    } else {
                        tvTargetAge.setVisibility(View.GONE);
                    }
                }
                if (onDateSelected != null) {
                    onDateSelected.run();
                }
                sheetDialog.dismiss();
            });
        }

        sheetDialog.show();
    }

    private void updateGenderFieldDisplay(ImageView ivGenderBadge, TextView tvGenderValue, String gender) {
        if (tvGenderValue == null) return;
        String maleStr = getString(R.string.gender_male);
        String femaleStr = getString(R.string.gender_female);
        boolean isMale = "male".equalsIgnoreCase(gender) || "lelaki".equalsIgnoreCase(gender)
                || maleStr.equalsIgnoreCase(gender);
        boolean isFemale = "female".equalsIgnoreCase(gender) || "perempuan".equalsIgnoreCase(gender)
                || femaleStr.equalsIgnoreCase(gender);

        if (isMale) {
            tvGenderValue.setText(maleStr);
            tvGenderValue.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
            if (ivGenderBadge != null) {
                ivGenderBadge.setImageResource(R.drawable.ic_gender_male);
                ivGenderBadge.setBackgroundResource(R.drawable.bg_gender_badge_male);
                ivGenderBadge.setColorFilter(ContextCompat.getColor(this, R.color.gender_male_icon));
                int pad = dp(2);
                ivGenderBadge.setPadding(pad, pad, pad, pad);
                ivGenderBadge.setVisibility(View.VISIBLE);
            }
        } else if (isFemale) {
            tvGenderValue.setText(femaleStr);
            tvGenderValue.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
            if (ivGenderBadge != null) {
                ivGenderBadge.setImageResource(R.drawable.ic_gender_female);
                ivGenderBadge.setBackgroundResource(R.drawable.bg_gender_badge_female);
                ivGenderBadge.setColorFilter(ContextCompat.getColor(this, R.color.gender_female_icon));
                int pad = dp(2);
                ivGenderBadge.setPadding(pad, pad, pad, pad);
                ivGenderBadge.setVisibility(View.VISIBLE);
            }
        } else {
            tvGenderValue.setText(getString(R.string.profile_field_gender_placeholder));
            tvGenderValue.setTextColor(ContextCompat.getColor(this, R.color.input_hint));
            if (ivGenderBadge != null) {
                ivGenderBadge.setVisibility(View.GONE);
            }
        }
    }

    private void showGenderPickerBottomSheet(AdditionalTravellerHolder holder) {
        BottomSheetDialog sheetDialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_gender_picker, null);
        sheetDialog.setContentView(sheetView);

        if (sheetDialog.getWindow() != null) {
            sheetDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            sheetDialog.getWindow().setDimAmount(0.55f);
        }

        View btnClose = sheetView.findViewById(R.id.btnCloseGenderSheet);
        if (btnClose != null) btnClose.setOnClickListener(v -> sheetDialog.dismiss());

        View itemMale = sheetView.findViewById(R.id.itemGenderMale);
        View itemFemale = sheetView.findViewById(R.id.itemGenderFemale);
        TextView tvMale = sheetView.findViewById(R.id.tvGenderMaleText);
        TextView tvFemale = sheetView.findViewById(R.id.tvGenderFemaleText);
        ImageView ivMaleCheck = sheetView.findViewById(R.id.ivGenderMaleCheck);
        ImageView ivMaleUncheck = sheetView.findViewById(R.id.ivGenderMaleUnchecked);
        ImageView ivFemaleCheck = sheetView.findViewById(R.id.ivGenderFemaleCheck);
        ImageView ivFemaleUncheck = sheetView.findViewById(R.id.ivGenderFemaleUnchecked);

        String maleLabel = getString(R.string.gender_male);
        String femaleLabel = getString(R.string.gender_female);

        Runnable updateCards = () -> {
            String current = holder.selectedGender != null ? holder.selectedGender : "";
            boolean male = "male".equalsIgnoreCase(current) || "lelaki".equalsIgnoreCase(current) || maleLabel.equalsIgnoreCase(current);
            boolean female = "female".equalsIgnoreCase(current) || "perempuan".equalsIgnoreCase(current) || femaleLabel.equalsIgnoreCase(current);

            if (itemMale != null) {
                itemMale.setBackgroundResource(male ? R.drawable.bg_selection_card_selected : R.drawable.bg_selection_card_unselected);
            }
            if (tvMale != null) {
                tvMale.setTextColor(ContextCompat.getColor(this, male ? R.color.pink_dark : R.color.text_dark));
                tvMale.setTypeface(null, male ? Typeface.BOLD : Typeface.NORMAL);
            }
            if (ivMaleCheck != null) ivMaleCheck.setVisibility(male ? View.VISIBLE : View.GONE);
            if (ivMaleUncheck != null) ivMaleUncheck.setVisibility(male ? View.GONE : View.VISIBLE);

            if (itemFemale != null) {
                itemFemale.setBackgroundResource(female ? R.drawable.bg_selection_card_selected : R.drawable.bg_selection_card_unselected);
            }
            if (tvFemale != null) {
                tvFemale.setTextColor(ContextCompat.getColor(this, female ? R.color.pink_dark : R.color.text_dark));
                tvFemale.setTypeface(null, female ? Typeface.BOLD : Typeface.NORMAL);
            }
            if (ivFemaleCheck != null) ivFemaleCheck.setVisibility(female ? View.VISIBLE : View.GONE);
            if (ivFemaleUncheck != null) ivFemaleUncheck.setVisibility(female ? View.GONE : View.VISIBLE);
        };

        updateCards.run();

        if (itemMale != null) {
            itemMale.setOnClickListener(v -> {
                HapticUtil.click(v);
                holder.selectedGender = maleLabel;
                updateGenderFieldDisplay(holder.ivGenderBadge, holder.tvGenderValue, maleLabel);
                updateCards.run();
                itemMale.postDelayed(sheetDialog::dismiss, 120);
            });
        }

        if (itemFemale != null) {
            itemFemale.setOnClickListener(v -> {
                HapticUtil.click(v);
                holder.selectedGender = femaleLabel;
                updateGenderFieldDisplay(holder.ivGenderBadge, holder.tvGenderValue, femaleLabel);
                updateCards.run();
                itemFemale.postDelayed(sheetDialog::dismiss, 120);
            });
        }

        sheetDialog.show();
    }

    private void showNationalityPickerBottomSheet(AdditionalTravellerHolder holder) {
        String[] countryHolder = new String[]{holder.selectedNationality != null ? holder.selectedNationality : ""};
        showCountrySearchBottomSheet(
                getString(R.string.profile_nationality_sheet_title),
                getString(R.string.profile_nationality_sheet_sub),
                holder.tvNationalityFlag,
                holder.tvNationalityValue,
                countryHolder,
                () -> holder.selectedNationality = countryHolder[0]
        );
    }

    private void showIssuingCountryPickerBottomSheet(AdditionalTravellerHolder holder) {
        String[] countryHolder = new String[]{holder.selectedIssuingCountry != null ? holder.selectedIssuingCountry : ""};
        showCountrySearchBottomSheet(
                getString(R.string.profile_issuing_country_sheet_title),
                getString(R.string.profile_issuing_country_sheet_sub),
                holder.tvIssuingCountryFlag,
                holder.tvIssuingCountryValue,
                countryHolder,
                () -> holder.selectedIssuingCountry = countryHolder[0]
        );
    }

    private void showCountrySearchBottomSheet(
            String title,
            String subtitle,
            TextView tvTargetFlag,
            TextView tvTargetValue,
            String[] selectedCountryHolder,
            Runnable onCountrySelected) {

        BottomSheetDialog sheetDialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_country_search_picker, null);
        sheetDialog.setContentView(sheetView);

        if (sheetDialog.getWindow() != null) {
            sheetDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            sheetDialog.getWindow().setDimAmount(0.55f);
            sheetDialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        TextView tvSheetTitle = sheetView.findViewById(R.id.tvCountrySheetTitle);
        if (tvSheetTitle != null && title != null && !title.isEmpty()) {
            tvSheetTitle.setText(title);
        }

        TextView tvSheetSub = sheetView.findViewById(R.id.tvCountrySheetSub);
        if (tvSheetSub != null && subtitle != null && !subtitle.isEmpty()) {
            tvSheetSub.setText(subtitle);
        }

        View btnClose = sheetView.findViewById(R.id.btnCloseCountrySheet);
        if (btnClose != null) btnClose.setOnClickListener(v -> sheetDialog.dismiss());

        EditText etSearch = sheetView.findViewById(R.id.etCountrySearch);
        ImageView btnClear = sheetView.findViewById(R.id.btnClearCountrySearch);
        ListView lvCountries = sheetView.findViewById(R.id.lvCountryPicker);
        TextView tvNoCountries = sheetView.findViewById(R.id.tvNoCountries);

        String[] seaCountries = getResources().getStringArray(R.array.sea_countries);
        CountryDropdownAdapter adapter = new CountryDropdownAdapter(this, seaCountries);
        String curCountry = selectedCountryHolder != null && selectedCountryHolder.length > 0 ? selectedCountryHolder[0] : "";
        adapter.setSelectedCountry(curCountry);

        CountryDropdownAdapter.OnCountrySelectedListener onSelected = (chosen, pos) -> {
            if (chosen != null && !chosen.trim().isEmpty()) {
                if (selectedCountryHolder != null && selectedCountryHolder.length > 0) {
                    selectedCountryHolder[0] = chosen;
                }
                if (tvTargetFlag != null) {
                    tvTargetFlag.setText(CountryDropdownAdapter.getFlagForCountry(chosen));
                    tvTargetFlag.setVisibility(View.VISIBLE);
                }
                if (tvTargetValue != null) {
                    tvTargetValue.setText(chosen);
                    tvTargetValue.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
                }
                if (onCountrySelected != null) {
                    onCountrySelected.run();
                }
            }
            sheetDialog.dismiss();
        };

        adapter.setOnCountrySelectedListener(onSelected);

        if (lvCountries != null) {
            lvCountries.setAdapter(adapter);
            lvCountries.setOnItemClickListener((parent, view, position, id) -> {
                HapticUtil.click(view);
                String chosen = adapter.getItem(position);
                onSelected.onCountrySelected(chosen, position);
            });

            int selectedPos = adapter.getPositionForCountry(curCountry);
            if (selectedPos >= 0) {
                lvCountries.setSelection(selectedPos);
            }
        }

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s != null ? s.toString() : "";
                    if (btnClear != null) {
                        btnClear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    adapter.getFilter().filter(query, countResult -> {
                        if (tvNoCountries != null) {
                            tvNoCountries.setVisibility(adapter.getCount() == 0 ? View.VISIBLE : View.GONE);
                        }
                    });
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (btnClear != null && etSearch != null) {
            btnClear.setOnClickListener(v -> etSearch.setText(""));
        }

        sheetDialog.show();
    }

    private void reindexAdditionalTravellers() {
        if (txtNoAdditionalTravellers != null) {
            txtNoAdditionalTravellers.setVisibility(additionalTravellers.isEmpty() ? View.VISIBLE : View.GONE);
        }
        for (int i = 0; i < additionalTravellers.size(); i++) {
            AdditionalTravellerHolder h = additionalTravellers.get(i);
            h.index = i + 2;
            h.titleText.setText(getString(R.string.passenger_traveller_title_format, h.index));
            String nameVal = h.inputName != null ? h.inputName.getText().toString().trim() : "";
            if (nameVal.isEmpty()) {
                h.txtNamePreview.setText(R.string.passenger_default_preview_name);
            }
        }
    }

    private void updateMahramDropdownOptions() {
        List<String> travellerOptions = new ArrayList<>();
        // Option 1: Lead traveller profile
        String leadName = (currentUserProfile != null && currentUserProfile.name != null && !currentUserProfile.name.isEmpty())
                ? currentUserProfile.name : getString(R.string.passenger_lead_default_name);
        travellerOptions.add("1. " + leadName + " " + getString(R.string.passenger_lead_tag));

        // Options 2..N: Additional travellers
        for (int i = 0; i < additionalTravellers.size(); i++) {
            String name = additionalTravellers.get(i).inputName != null ? additionalTravellers.get(i).inputName.getText().toString().trim() : "";
            if (name.isEmpty()) name = getString(R.string.passenger_traveller_title_format, (i + 2));
            travellerOptions.add((i + 2) + ". " + name);
        }

        for (AdditionalTravellerHolder h : additionalTravellers) {
            if (h.inputMahram != null) {
                h.inputMahram.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, travellerOptions));
            }
        }
    }

    private void updateFooterCount() {
        int totalPax = 1 + additionalTravellers.size();
        txtRoomAndPax.setText(getString(R.string.passenger_room_pax_format, bookingRequest.roomLabel, bookingRequest.adultPaxCount));
        txtTotalAmount.setText(bookingRequest.totalAmountFormatted);
        txtPaxCountFooter.setText(getString(R.string.passenger_footer_count_format, totalPax, bookingRequest.adultPaxCount));
    }

    private void showDatePicker(EditText editText) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dpd = new DatePickerDialog(this, (view, year1, monthOfYear, dayOfMonth) -> {
            String formattedDate = String.format(Locale.US, "%04d-%02d-%02d", year1, monthOfYear + 1, dayOfMonth);
            editText.setText(formattedDate);
        }, year, month, day);
        dpd.show();
    }

    private TextView createFieldLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(11);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(getResources().getColor(R.color.text_dark));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(8);
        tv.setLayoutParams(p);
        return tv;
    }

    private EditText createEditText(String hint, int inputType) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setInputType(inputType);
        et.setTextSize(12);
        et.setTextColor(getResources().getColor(R.color.text_dark));
        et.setHintTextColor(getResources().getColor(R.color.text_muted));
        et.setBackgroundResource(R.drawable.bg_input_box);
        et.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(4);
        et.setLayoutParams(p);
        return et;
    }

    private TextView createErrorTextView() {
        TextView tv = new TextView(this);
        tv.setTextSize(10);
        tv.setTextColor(getResources().getColor(R.color.error_red));
        tv.setVisibility(View.GONE);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(2);
        tv.setLayoutParams(p);
        return tv;
    }

    private void validateAndProceed() {
        // 1. Validate Lead Traveller Profile Status
        if (!isLeadProfileComplete) {
            Toast.makeText(this, getString(R.string.passenger_err_complete_lead_profile), Toast.LENGTH_LONG).show();
            leadProfileContainer.requestFocus();
            return;
        }

        // 2. Validate Total Pax Count vs Selected Booking Pax (exact match required)
        int totalPax = 1 + additionalTravellers.size();
        PassengerDetailsViewModel.PaxCheck paxCheck =
                passengerViewModel.checkPaxCount(totalPax, bookingRequest.adultPaxCount);
        if (paxCheck == PassengerDetailsViewModel.PaxCheck.UNDER) {
            Toast.makeText(this, getString(R.string.passenger_err_pax_count_mismatch, bookingRequest.adultPaxCount, totalPax, Math.max(0, bookingRequest.adultPaxCount - totalPax)), Toast.LENGTH_LONG).show();
            return;
        } else if (paxCheck == PassengerDetailsViewModel.PaxCheck.OVER) {
            Toast.makeText(this, getString(R.string.passenger_err_pax_over, totalPax, bookingRequest.adultPaxCount, totalPax - bookingRequest.adultPaxCount), Toast.LENGTH_LONG).show();
            return;
        }

        // 3. Validate Additional Travellers
        PackageDetail pkg = bookingRequest.packageDetail;
        boolean reqPassport = pkg != null ? pkg.requiresPassport : true;
        boolean reqIc = pkg != null ? pkg.requiresIc : true;
        View firstErrorView = null;
        boolean allValid = true;
        java.util.List<PassengerDetailsViewModel.TravellerInput> travellerInputs = new java.util.ArrayList<>();

        for (AdditionalTravellerHolder holder : additionalTravellers) {
            if (holder.errorName != null) holder.errorName.setVisibility(View.GONE);
            if (holder.errorIc != null) holder.errorIc.setVisibility(View.GONE);
            if (holder.errorPassport != null) holder.errorPassport.setVisibility(View.GONE);
            if (holder.errorPassportExpiry != null) holder.errorPassportExpiry.setVisibility(View.GONE);

            PassengerDetailsViewModel.TravellerInput in = readTravellerInput(holder);
            travellerInputs.add(in);
            PassengerDetailsViewModel.TravellerErrors errors =
                    passengerViewModel.validateTraveller(in, reqPassport, reqIc);

            if (errors.nameErr != 0) {
                if (holder.errorName != null) {
                    holder.errorName.setText(getString(errors.nameErr));
                    holder.errorName.setVisibility(View.VISIBLE);
                }
                allValid = false;
                if (firstErrorView == null) firstErrorView = holder.inputName;
            }

            if (reqIc && holder.inputIc != null && errors.icErr != 0) {
                if (holder.errorIc != null) {
                    holder.errorIc.setText(getString(errors.icErr));
                    holder.errorIc.setVisibility(View.VISIBLE);
                }
                allValid = false;
                if (firstErrorView == null) firstErrorView = holder.inputIc;
            }

            if (reqPassport && holder.inputPassport != null && errors.passportErr != 0) {
                if (holder.errorPassport != null) {
                    holder.errorPassport.setText(getString(errors.passportErr));
                    holder.errorPassport.setVisibility(View.VISIBLE);
                }
                allValid = false;
                if (firstErrorView == null) firstErrorView = holder.inputPassport;
            }

            if (reqPassport && errors.expiryErr != 0) {
                if (holder.errorPassportExpiry != null) {
                    holder.errorPassportExpiry.setText(getString(errors.expiryErr));
                    holder.errorPassportExpiry.setVisibility(View.VISIBLE);
                }
                allValid = false;
                if (firstErrorView == null) firstErrorView = holder.fieldPassportExpiryContainer;
            }
        }

        if (!allValid) {
            if (firstErrorView != null) firstErrorView.requestFocus();
            Toast.makeText(this, getString(R.string.passenger_err_complete_all_additional), Toast.LENGTH_SHORT).show();
            return;
        }

        // 4. Construct BookingRequest.passengers snapshot (frozen; later edits can't mutate it)
        bookingRequest.passengers.clear();

        // Lead passenger (Profile Snapshot)
        bookingRequest.passengers.add(passengerViewModel.buildLeadPassenger(currentUserProfile));

        // Additional passengers (Manual Input Snapshot)
        for (PassengerDetailsViewModel.TravellerInput in : travellerInputs) {
            bookingRequest.passengers.add(passengerViewModel.buildAdditionalPassenger(in));
        }

        // Navigate to Booking Summary
        Intent intent = new Intent(this, BookingSummaryActivity.class);
        intent.putExtra(BookingSummaryActivity.EXTRA_BOOKING_REQUEST, bookingRequest);
        startActivity(intent);
    }

    /** Reads card inputs; null marks fields whose input view doesn't exist or is not applicable. */
    private PassengerDetailsViewModel.TravellerInput readTravellerInput(AdditionalTravellerHolder holder) {
        PassengerDetailsViewModel.TravellerInput in = new PassengerDetailsViewModel.TravellerInput();
        in.title = null;
        in.fullName = holder.inputName != null ? holder.inputName.getText().toString().trim() : "";
        in.icNumber = (holder.inputIc != null && holder.layoutIcSection != null && holder.layoutIcSection.getVisibility() == View.VISIBLE)
                ? holder.inputIc.getText().toString().trim() : null;
        in.passportNumber = (holder.inputPassport != null && holder.layoutPassportSection != null && holder.layoutPassportSection.getVisibility() == View.VISIBLE)
                ? holder.inputPassport.getText().toString().trim() : null;
        in.passportExpiryDate = (holder.layoutPassportSection != null && holder.layoutPassportSection.getVisibility() == View.VISIBLE)
                ? holder.selectedPassportExpiry : null;
        in.issuingCountry = (holder.layoutPassportSection != null && holder.layoutPassportSection.getVisibility() == View.VISIBLE)
                ? holder.selectedIssuingCountry : null;
        in.dateOfBirth = holder.selectedDob;
        in.gender = holder.selectedGender;
        in.nationality = holder.selectedNationality;
        in.clothesSize = (holder.inputClothesSize != null && holder.layoutClothesSizeSection != null && holder.layoutClothesSizeSection.getVisibility() == View.VISIBLE)
                ? holder.inputClothesSize.getText().toString().trim() : null;
        in.mahramText = (holder.inputMahram != null && holder.layoutMahramSection != null && holder.layoutMahramSection.getVisibility() == View.VISIBLE)
                ? holder.inputMahram.getText().toString().trim() : null;
        in.relationship = (holder.inputRelationship != null && holder.layoutMahramSection != null && holder.layoutMahramSection.getVisibility() == View.VISIBLE)
                ? holder.inputRelationship.getText().toString().trim() : null;
        return in;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
