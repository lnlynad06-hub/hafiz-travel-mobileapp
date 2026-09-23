package com.hafiztraveltours.app.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.BookingRequest;
import com.hafiztraveltours.app.models.PackageDetail;
import com.hafiztraveltours.app.network.UserDto;
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

        LinearLayout cardView;
        LinearLayout headerRow;
        TextView titleText;
        ImageView expandIcon;
        TextView btnDelete;
        LinearLayout expandableBody;

        MaterialAutoCompleteTextView inputTitle;
        EditText inputName;
        TextView errorName;

        EditText inputIc;
        TextView errorIc;

        EditText inputPassport;
        TextView errorPassport;
        EditText inputPassportExpiry;
        TextView errorPassportExpiry;
        EditText inputIssuingCountry;

        EditText inputDob;
        MaterialAutoCompleteTextView inputGender;
        EditText inputNationality;
        MaterialAutoCompleteTextView inputClothesSize;

        MaterialAutoCompleteTextView inputMahram;
        EditText inputRelationship;
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

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_detail_card);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.topMargin = dp(10);
        card.setLayoutParams(cp);

        // Header Row
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText(getString(R.string.passenger_traveller_title_format, index));
        title.setTextSize(13);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(getResources().getColor(R.color.brand_magenta));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        title.setLayoutParams(tp);
        headerRow.addView(title);

        TextView btnDelete = new TextView(this);
        btnDelete.setText(getString(R.string.passenger_delete));
        btnDelete.setTextSize(11);
        btnDelete.setTextColor(Color.parseColor("#DC2626"));
        btnDelete.setPadding(dp(8), dp(4), dp(8), dp(4));
        btnDelete.setOnClickListener(v -> {
            additionalTravellersContainer.removeView(card);
            additionalTravellers.remove(holder);
            reindexAdditionalTravellers();
            updateMahramDropdownOptions();
            updateFooterCount();
        });
        headerRow.addView(btnDelete);

        ImageView expandIcon = new ImageView(this);
        expandIcon.setImageResource(R.drawable.ic_back);
        expandIcon.setRotation(90);
        expandIcon.setColorFilter(getResources().getColor(R.color.text_gray));
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(20), dp(20));
        ip.leftMargin = dp(6);
        expandIcon.setLayoutParams(ip);
        headerRow.addView(expandIcon);

        card.addView(headerRow);
        holder.headerRow = headerRow;
        holder.titleText = title;
        holder.btnDelete = btnDelete;
        holder.expandIcon = expandIcon;

        // Expandable Body
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(0, dp(10), 0, 0);

        // Name & Title Row
        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);

        MaterialAutoCompleteTextView inputTitle = new MaterialAutoCompleteTextView(this);
        inputTitle.setHint(getString(R.string.passenger_field_title_hint));
        inputTitle.setText(getString(R.string.default_title_mr), false);
        inputTitle.setTextSize(12);
        inputTitle.setTextColor(getResources().getColor(R.color.text_dark));
        inputTitle.setBackgroundResource(R.drawable.bg_input_box);
        inputTitle.setPadding(dp(8), dp(8), dp(8), dp(8));
        inputTitle.setInputType(InputType.TYPE_NULL);
        String[] titleOpts = new String[]{"Mr", "Mrs", "Ms", "Dr", "Dato", "Datuk", "Tan Sri"};
        inputTitle.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, titleOpts));
        LinearLayout.LayoutParams titleParam = new LinearLayout.LayoutParams(dp(80), ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParam.topMargin = dp(12);
        titleParam.rightMargin = dp(6);
        inputTitle.setLayoutParams(titleParam);
        nameRow.addView(inputTitle);
        holder.inputTitle = inputTitle;

        LinearLayout fnCol = new LinearLayout(this);
        fnCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams fnParam = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        fnCol.setLayoutParams(fnParam);
        fnCol.addView(createFieldLabel(getString(R.string.passenger_field_name_label)));
        holder.inputName = createEditText(getString(R.string.passenger_field_name_hint), InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        holder.errorName = createErrorTextView();
        fnCol.addView(holder.inputName);
        fnCol.addView(holder.errorName);
        nameRow.addView(fnCol);
        body.addView(nameRow);

        // DOB & Gender Row
        LinearLayout dobGenderRow = new LinearLayout(this);
        dobGenderRow.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout colDob = new LinearLayout(this);
        colDob.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colDobP = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        colDobP.rightMargin = dp(6);
        colDob.setLayoutParams(colDobP);
        colDob.addView(createFieldLabel(getString(R.string.passenger_field_dob_label)));
        holder.inputDob = createEditText("YYYY-MM-DD", InputType.TYPE_NULL);
        holder.inputDob.setFocusable(false);
        holder.inputDob.setOnClickListener(v -> showDatePicker(holder.inputDob));
        colDob.addView(holder.inputDob);

        LinearLayout colGender = new LinearLayout(this);
        colGender.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colGenP = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        colGender.setLayoutParams(colGenP);
        colGender.addView(createFieldLabel(getString(R.string.passenger_field_gender_label)));
        holder.inputGender = new MaterialAutoCompleteTextView(this);
        holder.inputGender.setHint(getString(R.string.passenger_field_gender_hint));
        holder.inputGender.setTextSize(12);
        holder.inputGender.setTextColor(getResources().getColor(R.color.text_dark));
        holder.inputGender.setBackgroundResource(R.drawable.bg_input_box);
        holder.inputGender.setPadding(dp(8), dp(8), dp(8), dp(8));
        holder.inputGender.setInputType(InputType.TYPE_NULL);
        String[] genderOpts = new String[]{getString(R.string.gender_male), getString(R.string.gender_female)};
        holder.inputGender.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genderOpts));
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gp.topMargin = dp(4);
        holder.inputGender.setLayoutParams(gp);
        colGender.addView(holder.inputGender);

        dobGenderRow.addView(colDob);
        dobGenderRow.addView(colGender);
        body.addView(dobGenderRow);

        // Nationality
        body.addView(createFieldLabel(getString(R.string.passenger_field_nationality_label)));
        holder.inputNationality = createEditText(getString(R.string.default_nationality), InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        holder.inputNationality.setText(getString(R.string.default_nationality));
        body.addView(holder.inputNationality);

        // IC if required
        if (reqIc) {
            body.addView(createFieldLabel(getString(R.string.passenger_field_ic_label)));
            holder.inputIc = createEditText(getString(R.string.passenger_field_ic_hint), InputType.TYPE_CLASS_NUMBER);
            holder.errorIc = createErrorTextView();
            body.addView(holder.inputIc);
            body.addView(holder.errorIc);
        }

        // Passport if required
        if (reqPassport) {
            body.addView(createFieldLabel(getString(R.string.passenger_field_passport_label)));
            holder.inputPassport = createEditText(getString(R.string.passenger_field_passport_hint), InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
            holder.errorPassport = createErrorTextView();
            body.addView(holder.inputPassport);
            body.addView(holder.errorPassport);

            LinearLayout passRow = new LinearLayout(this);
            passRow.setOrientation(LinearLayout.HORIZONTAL);

            LinearLayout colEx = new LinearLayout(this);
            colEx.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams colExP = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            colExP.rightMargin = dp(6);
            colEx.setLayoutParams(colExP);
            colEx.addView(createFieldLabel(getString(R.string.passenger_field_passport_expiry_label)));
            holder.inputPassportExpiry = createEditText("YYYY-MM-DD", InputType.TYPE_NULL);
            holder.inputPassportExpiry.setFocusable(false);
            holder.inputPassportExpiry.setOnClickListener(v -> showDatePicker(holder.inputPassportExpiry));
            holder.errorPassportExpiry = createErrorTextView();
            colEx.addView(holder.inputPassportExpiry);
            colEx.addView(holder.errorPassportExpiry);

            LinearLayout colCtry = new LinearLayout(this);
            colCtry.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams colCtryP = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            colCtry.setLayoutParams(colCtryP);
            colCtry.addView(createFieldLabel(getString(R.string.passenger_field_issuing_country_label)));
            holder.inputIssuingCountry = createEditText(getString(R.string.default_country), InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            holder.inputIssuingCountry.setText(getString(R.string.default_country));
            colCtry.addView(holder.inputIssuingCountry);

            passRow.addView(colEx);
            passRow.addView(colCtry);
            body.addView(passRow);
        }

        // Clothes Size if required
        if (reqClothesSize) {
            body.addView(createFieldLabel(getString(R.string.passenger_field_clothes_size_label)));
            holder.inputClothesSize = new MaterialAutoCompleteTextView(this);
            holder.inputClothesSize.setHint(getString(R.string.passenger_field_clothes_size_hint));
            holder.inputClothesSize.setTextSize(12);
            holder.inputClothesSize.setTextColor(getResources().getColor(R.color.text_dark));
            holder.inputClothesSize.setBackgroundResource(R.drawable.bg_input_box);
            holder.inputClothesSize.setPadding(dp(8), dp(8), dp(8), dp(8));
            holder.inputClothesSize.setInputType(InputType.TYPE_NULL);
            holder.inputClothesSize.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new String[]{"S", "M", "L", "XL", "2XL", "3XL", "4XL", "Custom"}));
            LinearLayout.LayoutParams szp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            szp.topMargin = dp(4);
            holder.inputClothesSize.setLayoutParams(szp);
            body.addView(holder.inputClothesSize);
        }

        // Mahram Selection if package requires mahram
        if (reqMahram) {
            body.addView(createFieldLabel(getString(R.string.passenger_field_mahram_label)));
            holder.inputMahram = new MaterialAutoCompleteTextView(this);
            holder.inputMahram.setHint(getString(R.string.passenger_field_mahram_hint));
            holder.inputMahram.setTextSize(12);
            holder.inputMahram.setTextColor(getResources().getColor(R.color.text_dark));
            holder.inputMahram.setBackgroundResource(R.drawable.bg_input_box);
            holder.inputMahram.setPadding(dp(8), dp(8), dp(8), dp(8));
            holder.inputMahram.setInputType(InputType.TYPE_NULL);
            LinearLayout.LayoutParams mhp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mhp.topMargin = dp(4);
            holder.inputMahram.setLayoutParams(mhp);
            body.addView(holder.inputMahram);

            body.addView(createFieldLabel(getString(R.string.passenger_field_mahram_relation_label)));
            holder.inputRelationship = createEditText(getString(R.string.passenger_field_mahram_relation_hint), InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            body.addView(holder.inputRelationship);
        }

        card.addView(body);
        holder.cardView = card;
        holder.expandableBody = body;

        headerRow.setOnClickListener(v -> {
            com.hafiztraveltours.app.utils.HapticUtil.click(v);
            holder.isExpanded = !holder.isExpanded;
            body.setVisibility(holder.isExpanded ? View.VISIBLE : View.GONE);
            expandIcon.setRotation(holder.isExpanded ? 90 : 270);
        });

        additionalTravellers.add(holder);
        additionalTravellersContainer.addView(card);

        if (txtNoAdditionalTravellers != null) {
            txtNoAdditionalTravellers.setVisibility(View.GONE);
        }

        updateMahramDropdownOptions();
        updateFooterCount();
    }

    private void reindexAdditionalTravellers() {
        if (txtNoAdditionalTravellers != null) {
            txtNoAdditionalTravellers.setVisibility(additionalTravellers.isEmpty() ? View.VISIBLE : View.GONE);
        }
        for (int i = 0; i < additionalTravellers.size(); i++) {
            AdditionalTravellerHolder h = additionalTravellers.get(i);
            h.index = i + 2;
            h.titleText.setText(getString(R.string.passenger_traveller_title_format, h.index));
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
            String name = additionalTravellers.get(i).inputName.getText().toString().trim();
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

            if (reqPassport && holder.inputPassportExpiry != null && errors.expiryErr != 0) {
                if (holder.errorPassportExpiry != null) {
                    holder.errorPassportExpiry.setText(getString(errors.expiryErr));
                    holder.errorPassportExpiry.setVisibility(View.VISIBLE);
                }
                allValid = false;
                if (firstErrorView == null) firstErrorView = holder.inputPassportExpiry;
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

    /** Reads card inputs; null marks fields whose input view doesn't exist (not applicable). */
    private PassengerDetailsViewModel.TravellerInput readTravellerInput(AdditionalTravellerHolder holder) {
        PassengerDetailsViewModel.TravellerInput in = new PassengerDetailsViewModel.TravellerInput();
        in.title = holder.inputTitle != null ? holder.inputTitle.getText().toString().trim() : null;
        in.fullName = holder.inputName.getText().toString().trim();
        in.icNumber = holder.inputIc != null ? holder.inputIc.getText().toString().trim() : null;
        in.passportNumber = holder.inputPassport != null ? holder.inputPassport.getText().toString().trim() : null;
        in.passportExpiryDate = holder.inputPassportExpiry != null
                ? holder.inputPassportExpiry.getText().toString().trim() : null;
        in.issuingCountry = holder.inputIssuingCountry != null
                ? holder.inputIssuingCountry.getText().toString().trim() : null;
        in.dateOfBirth = holder.inputDob != null ? holder.inputDob.getText().toString().trim() : null;
        in.gender = holder.inputGender != null ? holder.inputGender.getText().toString().trim() : null;
        in.nationality = holder.inputNationality != null
                ? holder.inputNationality.getText().toString().trim() : null;
        in.clothesSize = holder.inputClothesSize != null
                ? holder.inputClothesSize.getText().toString().trim() : null;
        in.mahramText = holder.inputMahram != null ? holder.inputMahram.getText().toString().trim() : null;
        in.relationship = holder.inputRelationship != null
                ? holder.inputRelationship.getText().toString().trim() : null;
        return in;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
