package com.hafiztraveltours.app.ui;

import android.app.AlertDialog;
import com.hafiztraveltours.app.utils.DatePickerBottomSheetHelper;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import com.google.android.material.textfield.TextInputLayout;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.models.CreateCustomTravelRequest;
import com.hafiztraveltours.app.models.CustomTravelRequestDto;
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiErrors;
import com.hafiztraveltours.app.utils.CustomTravelHelper;
import com.hafiztraveltours.app.utils.RoomLabels;
import com.hafiztraveltours.app.utils.SessionManager;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomTravelWizardActivity extends BaseActivity {

    public static final String EXTRA_REQUEST_TYPE = "extra_request_type";
    public static final String EXTRA_TRAVEL_TYPE = "extra_travel_type";
    public static final String EXTRA_PACKAGE_ID = "extra_package_id";
    public static final String EXTRA_PACKAGE_NAME = "extra_package_name";
    public static final String EXTRA_DESTINATION = "extra_destination";
    public static final String EXTRA_DURATION_DAYS = "extra_duration_days";
    public static final String EXTRA_STARTING_PRICE = "extra_starting_price";
    public static final String EXTRA_EXISTING_REQUEST = "extra_existing_request";

    private static final String STATE_CURRENT_STAGE = "state_current_stage";
    private static final String STATE_REQUEST_DATA = "state_request_data";

    private int currentStage = 1;
    private CreateCustomTravelRequest requestData = new CreateCustomTravelRequest();
    private CustomTravelRequestDto existingRequest = null;
    private boolean isEditMode = false;

    // Base package info
    private Integer basePackageId = null;
    private String basePackageName = null;
    private String baseStartingPrice = null;

    // Top Bar & Navigation Views
    private TextView tvWizardTitle;
    private TextView tvWizardStepIndicator;
    private ProgressBar wizardProgressBar;
    private ImageButton btnWizardBack;
    private Button btnPrevStage;
    private Button btnNextStage;

    // Stage Containers
    private View stage1Container;
    private View stage2Container;
    private View stage3Container;
    private View stage4Container;

    // Stage 1 Views
    private CardView cardBasePackage;
    private TextView tvBasePackageName;
    private TextView tvBasePackagePrice;
    private RadioGroup rgTravelType;
    private RadioButton rbTypeUmrah;
    private RadioButton rbTypeTour;
    private TextInputLayout layoutDestination;
    private EditText etDestination;
    private EditText etDepartureAirport;
    private TextView tvDepartureDate;
    private TextView tvReturnDate;
    private CheckBox cbFlexibleDates;
    private EditText etDurationDays;

    // Stage 2 Views
    private TextView tvCountAdultMale;
    private TextView tvCountAdultFemale;
    private TextView tvCountChildren;
    private LinearLayout containerChildrenDetails;
    private RadioGroup rgRoomCategory;
    private RadioButton rbRoomQuint;
    private RadioButton rbRoomQuad;
    private RadioButton rbRoomTriple;
    private RadioButton rbRoomDouble;
    private RadioButton rbRoomSingle;
    private TextView tvUmrahSingleWarning;
    private SwitchCompat switchFamilyRoom;
    private TextView tvGeneratedRoomPreview;
    private Spinner spHotelCategory;
    private EditText etHotelPreferences;

    // Stage 3 Views
    private EditText etAirlinePreference;
    private Spinner spFlightClass;
    private RadioGroup rgFlightRouting;
    private RadioButton rbFlightDirect;
    private RadioButton rbFlightTransit;
    private RadioButton rbFlightAny;
    private EditText etTransportation;
    private CheckBox cbPrivateGuide;
    private Spinner spMeals;
    private EditText etItineraryNotes;
    private EditText etBudgetPerPax;
    private EditText etSpecialRequirements;

    // Stage 4 Views
    private TextView tvSummaryOverview;
    private TextView tvSummaryTravellersRooms;
    private TextView tvSummaryPreferences;

    private boolean isSubmitting = false;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_travel_wizard);

        parseIntentExtras();

        if (savedInstanceState != null) {
            currentStage = savedInstanceState.getInt(STATE_CURRENT_STAGE, 1);
        }

        bindViews();
        setupSpinners();
        setupListeners();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });
        populateInitialData();
        renderStage(currentStage);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        collectCurrentStageData();
        outState.putInt(STATE_CURRENT_STAGE, currentStage);
    }

    private void parseIntentExtras() {
        Intent intent = getIntent();
        if (intent == null) return;

        Serializable ser = intent.getSerializableExtra(EXTRA_EXISTING_REQUEST);
        if (ser instanceof CustomTravelRequestDto) {
            existingRequest = (CustomTravelRequestDto) ser;
            isEditMode = true;
        }

        requestData.requestType = intent.getStringExtra(EXTRA_REQUEST_TYPE);
        if (requestData.requestType == null || requestData.requestType.isEmpty()) {
            requestData.requestType = isEditMode && existingRequest.packageId != null ? "customize_package" : "custom_trip";
        }

        requestData.travelType = intent.getStringExtra(EXTRA_TRAVEL_TYPE);
        if (requestData.travelType == null || requestData.travelType.isEmpty()) {
            requestData.travelType = isEditMode ? existingRequest.travelType : "umrah";
        }

        if (intent.hasExtra(EXTRA_PACKAGE_ID)) {
            basePackageId = intent.getIntExtra(EXTRA_PACKAGE_ID, 0);
            if (basePackageId == 0) basePackageId = null;
            requestData.packageId = basePackageId;
        }
        basePackageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
        baseStartingPrice = intent.getStringExtra(EXTRA_STARTING_PRICE);

        String dest = intent.getStringExtra(EXTRA_DESTINATION);
        if (dest != null && !dest.isEmpty()) {
            requestData.destination = dest;
        }

        int duration = intent.getIntExtra(EXTRA_DURATION_DAYS, 0);
        if (duration > 0) {
            requestData.durationDays = duration;
        }

        if (isEditMode && existingRequest != null) {
            mapExistingRequestToFormData(existingRequest);
        }
    }

    private void mapExistingRequestToFormData(CustomTravelRequestDto dto) {
        requestData.requestType = dto.requestType;
        requestData.travelType = dto.travelType;
        requestData.packageId = dto.packageId;
        requestData.destination = dto.destination;
        requestData.departureAirport = dto.departureAirport;
        requestData.preferredDepartureDate = dto.preferredDepartureDate;
        requestData.preferredReturnDate = dto.preferredReturnDate;
        requestData.isFlexibleDates = dto.isFlexibleDates;
        requestData.durationDays = dto.durationDays;
        requestData.adultMaleCount = dto.adultMaleCount;
        requestData.adultFemaleCount = dto.adultFemaleCount;
        requestData.childrenCount = dto.childrenCount;
        requestData.childrenDetails = dto.childrenDetails != null ? new ArrayList<>(dto.childrenDetails) : new ArrayList<>();
        requestData.roomCategory = dto.roomCategory;
        requestData.isFamilyRoom = dto.isFamilyRoom;
        requestData.hotelCategory = dto.hotelCategory;
        requestData.hotelPreferences = dto.hotelPreferences;
        requestData.flightPreferences = dto.flightPreferences;
        requestData.transportationPreferences = dto.transportationPreferences;
        requestData.itineraryPreferences = dto.itineraryPreferences;
        requestData.budgetPerPax = dto.budgetPerPax;
        requestData.specialRequirements = dto.specialRequirements;
    }

    private void bindViews() {
        tvWizardTitle = findViewById(R.id.wizardTitle);
        tvWizardStepIndicator = findViewById(R.id.wizardStepIndicator);
        wizardProgressBar = findViewById(R.id.wizardProgressBar);
        btnWizardBack = findViewById(R.id.wizardBackButton);
        btnPrevStage = findViewById(R.id.btnWizardBack);
        btnNextStage = findViewById(R.id.btnWizardNext);

        stage1Container = findViewById(R.id.stage1Container);
        stage2Container = findViewById(R.id.stage2Container);
        stage3Container = findViewById(R.id.stage3Container);
        stage4Container = findViewById(R.id.stage4Container);

        // Stage 1
        cardBasePackage = findViewById(R.id.cardBasePackage);
        tvBasePackageName = findViewById(R.id.tvBasePackageName);
        tvBasePackagePrice = findViewById(R.id.tvBasePackagePrice);
        rgTravelType = findViewById(R.id.rgTravelType);
        rbTypeUmrah = findViewById(R.id.rbTypeUmrah);
        rbTypeTour = findViewById(R.id.rbTypeTour);
        layoutDestination = findViewById(R.id.layoutDestination);
        etDestination = findViewById(R.id.etDestination);
        etDepartureAirport = findViewById(R.id.etDepartureAirport);
        tvDepartureDate = findViewById(R.id.tvPreferredDepartureDate);
        tvReturnDate = findViewById(R.id.tvPreferredReturnDate);
        cbFlexibleDates = findViewById(R.id.cbFlexibleDates);
        etDurationDays = findViewById(R.id.etDurationDays);

        // Stage 2
        tvCountAdultMale = findViewById(R.id.tvCountAdultMale);
        tvCountAdultFemale = findViewById(R.id.tvCountAdultFemale);
        tvCountChildren = findViewById(R.id.tvCountChildren);
        containerChildrenDetails = findViewById(R.id.containerChildrenDetails);
        rgRoomCategory = findViewById(R.id.rgRoomCategory);
        rbRoomQuint = findViewById(R.id.rbRoomQuint);
        rbRoomQuad = findViewById(R.id.rbRoomQuad);
        rbRoomTriple = findViewById(R.id.rbRoomTriple);
        rbRoomDouble = findViewById(R.id.rbRoomDouble);
        rbRoomSingle = findViewById(R.id.rbRoomSingle);
        tvUmrahSingleWarning = findViewById(R.id.tvUmrahSingleWarning);
        switchFamilyRoom = findViewById(R.id.switchFamilyRoom);
        tvGeneratedRoomPreview = findViewById(R.id.tvGeneratedRoomPreview);
        spHotelCategory = findViewById(R.id.spHotelCategory);
        etHotelPreferences = findViewById(R.id.etHotelPreferences);

        // Stage 3
        etAirlinePreference = findViewById(R.id.etAirlinePreference);
        spFlightClass = findViewById(R.id.spFlightClass);
        rgFlightRouting = findViewById(R.id.rgFlightRouting);
        rbFlightDirect = findViewById(R.id.rbFlightDirect);
        rbFlightTransit = findViewById(R.id.rbFlightTransit);
        rbFlightAny = findViewById(R.id.rbFlightAny);
        etTransportation = findViewById(R.id.etTransportation);
        cbPrivateGuide = findViewById(R.id.cbPrivateGuide);
        spMeals = findViewById(R.id.spMeals);
        etItineraryNotes = findViewById(R.id.etItineraryNotes);
        etBudgetPerPax = findViewById(R.id.etBudgetPerPax);
        etSpecialRequirements = findViewById(R.id.etSpecialRequirements);

        // Stage 4
        tvSummaryOverview = findViewById(R.id.tvSummaryOverview);
        tvSummaryTravellersRooms = findViewById(R.id.tvSummaryTravellersRooms);
        tvSummaryPreferences = findViewById(R.id.tvSummaryPreferences);

        findViewById(R.id.btnEditStage1).setOnClickListener(v -> renderStage(1));
        findViewById(R.id.btnEditStage2).setOnClickListener(v -> renderStage(2));
        findViewById(R.id.btnEditStage3).setOnClickListener(v -> renderStage(3));
    }

    private void setupSpinners() {
        // Hotel Star Rating Spinner
        String[] hotelCategories = new String[] {
                getString(R.string.hotel_star_any),
                getString(R.string.hotel_star_5),
                getString(R.string.hotel_star_4),
                getString(R.string.hotel_star_3)
        };
        ArrayAdapter<String> hotelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, hotelCategories);
        spHotelCategory.setAdapter(hotelAdapter);

        // Flight Class Spinner
        String[] flightClasses = new String[] {
                getString(R.string.flight_class_economy),
                getString(R.string.flight_class_premium_economy),
                getString(R.string.flight_class_business),
                getString(R.string.flight_class_first)
        };
        ArrayAdapter<String> flightAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, flightClasses);
        spFlightClass.setAdapter(flightAdapter);

        // Meals Spinner
        String[] mealsOptions = new String[] {
                getString(R.string.meals_full_board),
                getString(R.string.meals_half_board),
                getString(R.string.meals_breakfast_only),
                getString(R.string.meals_none)
        };
        ArrayAdapter<String> mealsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, mealsOptions);
        spMeals.setAdapter(mealsAdapter);
    }

    private void setupListeners() {
        btnWizardBack.setOnClickListener(v -> handleBackNavigation());
        btnPrevStage.setOnClickListener(v -> {
            if (currentStage > 1) {
                renderStage(currentStage - 1);
            }
        });

        btnNextStage.setOnClickListener(v -> {
            if (currentStage < 4) {
                if (validateStage(currentStage)) {
                    collectCurrentStageData();
                    renderStage(currentStage + 1);
                }
            } else {
                submitCustomTravelRequest();
            }
        });

        // Travel Type Selection
        rgTravelType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbTypeUmrah) {
                requestData.travelType = "umrah";
                rbTypeUmrah.setTextColor(getColor(R.color.brand_magenta));
                rbTypeTour.setTextColor(getColor(R.color.text_gray));
            } else {
                requestData.travelType = "tour";
                rbTypeTour.setTextColor(getColor(R.color.brand_magenta));
                rbTypeUmrah.setTextColor(getColor(R.color.text_gray));
            }
            updateRoomCategoryAvailability();
            updateRoomArrangementPreview();
        });

        etDestination.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (layoutDestination != null && s.length() > 0) {
                    layoutDestination.setError(null);
                    layoutDestination.setErrorEnabled(false);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Date Pickers
        View btnPickDep = findViewById(R.id.btnPickDepartureDate);
        if (btnPickDep != null) btnPickDep.setOnClickListener(v -> showDatePicker(true));
        tvDepartureDate.setOnClickListener(v -> showDatePicker(true));

        View btnPickRet = findViewById(R.id.btnPickReturnDate);
        if (btnPickRet != null) btnPickRet.setOnClickListener(v -> showDatePicker(false));
        tvReturnDate.setOnClickListener(v -> showDatePicker(false));

        // Counters in Stage 2
        findViewById(R.id.btnMinusAdultMale).setOnClickListener(v -> {
            int current = parseCount(tvCountAdultMale.getText().toString());
            if (current > 0) {
                tvCountAdultMale.setText(String.valueOf(current - 1));
                updateRoomArrangementPreview();
            }
        });
        findViewById(R.id.btnPlusAdultMale).setOnClickListener(v -> {
            int current = parseCount(tvCountAdultMale.getText().toString());
            tvCountAdultMale.setText(String.valueOf(current + 1));
            updateRoomArrangementPreview();
        });

        findViewById(R.id.btnMinusAdultFemale).setOnClickListener(v -> {
            int current = parseCount(tvCountAdultFemale.getText().toString());
            if (current > 0) {
                tvCountAdultFemale.setText(String.valueOf(current - 1));
                updateRoomArrangementPreview();
            }
        });
        findViewById(R.id.btnPlusAdultFemale).setOnClickListener(v -> {
            int current = parseCount(tvCountAdultFemale.getText().toString());
            tvCountAdultFemale.setText(String.valueOf(current + 1));
            updateRoomArrangementPreview();
        });

        findViewById(R.id.btnMinusChildren).setOnClickListener(v -> {
            int current = parseCount(tvCountChildren.getText().toString());
            if (current > 0) {
                int newCount = current - 1;
                tvCountChildren.setText(String.valueOf(newCount));
                syncChildrenCount(newCount);
                updateRoomArrangementPreview();
            }
        });
        findViewById(R.id.btnPlusChildren).setOnClickListener(v -> {
            int current = parseCount(tvCountChildren.getText().toString());
            int newCount = current + 1;
            tvCountChildren.setText(String.valueOf(newCount));
            syncChildrenCount(newCount);
            updateRoomArrangementPreview();
        });

        rgRoomCategory.setOnCheckedChangeListener((group, checkedId) -> {
            updateRoomCategoryStyles();
            updateRoomArrangementPreview();
        });

        switchFamilyRoom.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateRoomArrangementPreview();
        });

        // Routing RadioGroup
        rgFlightRouting.setOnCheckedChangeListener((group, checkedId) -> {
            rbFlightDirect.setTextColor(checkedId == R.id.rbFlightDirect ? getColor(R.color.brand_magenta) : getColor(R.color.text_gray));
            rbFlightTransit.setTextColor(checkedId == R.id.rbFlightTransit ? getColor(R.color.brand_magenta) : getColor(R.color.text_gray));
            rbFlightAny.setTextColor(checkedId == R.id.rbFlightAny ? getColor(R.color.brand_magenta) : getColor(R.color.text_gray));
        });
    }

    private void populateInitialData() {
        if (isEditMode) {
            tvWizardTitle.setText(R.string.custom_travel_wizard_edit_title);
        }

        // Base package card
        if (basePackageName != null && !basePackageName.isEmpty()) {
            cardBasePackage.setVisibility(View.VISIBLE);
            tvBasePackageName.setText(basePackageName);
            if (baseStartingPrice != null && !baseStartingPrice.isEmpty()) {
                tvBasePackagePrice.setText(baseStartingPrice);
                tvBasePackagePrice.setVisibility(View.VISIBLE);
            } else {
                tvBasePackagePrice.setVisibility(View.GONE);
            }
        } else {
            cardBasePackage.setVisibility(View.GONE);
        }

        // Category
        if ("tour".equalsIgnoreCase(requestData.travelType)) {
            rbTypeTour.setChecked(true);
        } else {
            rbTypeUmrah.setChecked(true);
        }

        boolean isCustomizePackage = "customize_package".equalsIgnoreCase(requestData.requestType);

        if (requestData.destination != null && !requestData.destination.trim().isEmpty()) {
            etDestination.setText(requestData.destination);
        } else if (isCustomizePackage && basePackageName != null) {
            etDestination.setText(basePackageName);
        } else {
            etDestination.setText("");
        }

        if (requestData.departureAirport != null && !requestData.departureAirport.trim().isEmpty()) {
            etDepartureAirport.setText(requestData.departureAirport);
        } else {
            etDepartureAirport.setText("");
        }

        if (requestData.preferredDepartureDate != null && !requestData.preferredDepartureDate.trim().isEmpty()) {
            tvDepartureDate.setText(ProfileActivity.formatDateDisplay(this, requestData.preferredDepartureDate));
            tvDepartureDate.setTextColor(getColor(R.color.text_dark));
        } else {
            tvDepartureDate.setText(R.string.hint_select_date);
            tvDepartureDate.setTextColor(getColor(R.color.text_muted));
        }

        if (requestData.preferredReturnDate != null && !requestData.preferredReturnDate.trim().isEmpty()) {
            tvReturnDate.setText(ProfileActivity.formatDateDisplay(this, requestData.preferredReturnDate));
            tvReturnDate.setTextColor(getColor(R.color.text_dark));
        } else {
            tvReturnDate.setText(R.string.hint_select_date);
            tvReturnDate.setTextColor(getColor(R.color.text_muted));
        }

        cbFlexibleDates.setChecked(requestData.isFlexibleDates != null && requestData.isFlexibleDates);

        if (requestData.durationDays != null && requestData.durationDays > 0) {
            etDurationDays.setText(String.valueOf(requestData.durationDays));
        } else {
            etDurationDays.setText("");
        }

        // Stage 2
        tvCountAdultMale.setText(String.valueOf(Math.max(1, requestData.adultMaleCount)));
        tvCountAdultFemale.setText(String.valueOf(requestData.adultFemaleCount));
        tvCountChildren.setText(String.valueOf(requestData.childrenCount));
        syncChildrenCount(requestData.childrenCount);

        switchFamilyRoom.setChecked(requestData.isFamilyRoom != null && requestData.isFamilyRoom);

        // Room Category
        String rc = requestData.roomCategory != null ? requestData.roomCategory.toLowerCase() : "quad";
        if ("quint".equals(rc)) rbRoomQuint.setChecked(true);
        else if ("triple".equals(rc)) rbRoomTriple.setChecked(true);
        else if ("double".equals(rc)) rbRoomDouble.setChecked(true);
        else if ("single".equals(rc)) rbRoomSingle.setChecked(true);
        else rbRoomQuad.setChecked(true);

        updateRoomCategoryAvailability();
        updateRoomCategoryStyles();
        updateRoomArrangementPreview();

        if (requestData.hotelPreferences != null) {
            etHotelPreferences.setText(requestData.hotelPreferences);
        }

        // Stage 3
        if (requestData.flightPreferences != null) {
            if (requestData.flightPreferences.airline != null) {
                etAirlinePreference.setText(requestData.flightPreferences.airline);
            }
            if ("transit".equalsIgnoreCase(requestData.flightPreferences.directTransit)) {
                rbFlightTransit.setChecked(true);
            } else if ("any".equalsIgnoreCase(requestData.flightPreferences.directTransit)) {
                rbFlightAny.setChecked(true);
            } else {
                rbFlightDirect.setChecked(true);
            }
        }

        if (requestData.transportationPreferences != null) {
            etTransportation.setText(requestData.transportationPreferences);
        }

        if (requestData.itineraryPreferences != null) {
            etItineraryNotes.setText(requestData.itineraryPreferences);
        }

        if (requestData.budgetPerPax != null && requestData.budgetPerPax > 0) {
            etBudgetPerPax.setText(String.format(Locale.US, "%.0f", requestData.budgetPerPax));
        }

        if (requestData.specialRequirements != null) {
            etSpecialRequirements.setText(requestData.specialRequirements);
        }
    }

    private void renderStage(int stage) {
        currentStage = Math.max(1, Math.min(4, stage));

        stage1Container.setVisibility(currentStage == 1 ? View.VISIBLE : View.GONE);
        stage2Container.setVisibility(currentStage == 2 ? View.VISIBLE : View.GONE);
        stage3Container.setVisibility(currentStage == 3 ? View.VISIBLE : View.GONE);
        stage4Container.setVisibility(currentStage == 4 ? View.VISIBLE : View.GONE);

        wizardProgressBar.setProgress(currentStage);

        String stageTitle;
        switch (currentStage) {
            case 1:
                stageTitle = getString(R.string.wizard_stage_1_title);
                break;
            case 2:
                stageTitle = getString(R.string.wizard_stage_2_title);
                break;
            case 3:
                stageTitle = getString(R.string.wizard_stage_3_title);
                break;
            case 4:
            default:
                stageTitle = getString(R.string.wizard_stage_4_title);
                populateSummaryStage();
                break;
        }

        tvWizardStepIndicator.setText(getString(R.string.wizard_step_indicator, currentStage, stageTitle));

        btnPrevStage.setVisibility(currentStage > 1 ? View.VISIBLE : View.GONE);
        if (currentStage == 4) {
            btnNextStage.setText(isEditMode ? R.string.btn_update_request : R.string.btn_submit_request);
        } else {
            btnNextStage.setText(R.string.btn_next_stage);
        }

        // Scroll to top of stage
        androidx.core.widget.NestedScrollView sv = findViewById(R.id.wizardScrollView);
        if (sv != null) sv.smoothScrollTo(0, 0);
    }

    private boolean validateStage(int stage) {
        if (stage == 1) {
            String dest = etDestination.getText().toString().trim();
            if (dest.isEmpty()) {
                if (layoutDestination != null) {
                    layoutDestination.setErrorEnabled(true);
                    layoutDestination.setError(getString(R.string.err_destination_required));
                } else {
                    etDestination.setError(getString(R.string.err_destination_required));
                }
                etDestination.requestFocus();
                return false;
            } else if (layoutDestination != null) {
                layoutDestination.setError(null);
                layoutDestination.setErrorEnabled(false);
            }

            if (requestData.preferredDepartureDate == null || requestData.preferredDepartureDate.trim().isEmpty()) {
                Toast.makeText(this, getString(R.string.err_departure_date_required), Toast.LENGTH_SHORT).show();
                return false;
            }

            if (requestData.preferredReturnDate != null && !requestData.preferredReturnDate.trim().isEmpty()) {
                if (requestData.preferredReturnDate.compareTo(requestData.preferredDepartureDate) < 0) {
                    Toast.makeText(this, getString(R.string.err_return_date_before_departure), Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
            return true;
        }

        if (stage == 2) {
            int males = parseCount(tvCountAdultMale.getText().toString());
            int females = parseCount(tvCountAdultFemale.getText().toString());
            if (males + females < 1) {
                Toast.makeText(this, getString(R.string.err_at_least_one_adult), Toast.LENGTH_SHORT).show();
                return false;
            }

            // Umrah Single Room Guard
            String selectedCategory = getSelectedRoomCategory();
            if ("umrah".equalsIgnoreCase(requestData.travelType) && "single".equalsIgnoreCase(selectedCategory)) {
                Toast.makeText(this, getString(R.string.err_umrah_single_prohibited), Toast.LENGTH_LONG).show();
                return false;
            }
            return true;
        }

        return true;
    }

    private void collectCurrentStageData() {
        if (currentStage == 1) {
            requestData.destination = etDestination.getText().toString().trim();
            requestData.departureAirport = etDepartureAirport.getText().toString().trim();
            requestData.isFlexibleDates = cbFlexibleDates.isChecked();
            String durStr = etDurationDays.getText().toString().trim();
            if (!durStr.isEmpty()) {
                try {
                    requestData.durationDays = Integer.parseInt(durStr);
                } catch (Exception e) {
                    requestData.durationDays = null;
                }
            } else {
                requestData.durationDays = null;
            }
        } else if (currentStage == 2) {
            requestData.adultMaleCount = parseCount(tvCountAdultMale.getText().toString());
            requestData.adultFemaleCount = parseCount(tvCountAdultFemale.getText().toString());
            requestData.childrenCount = parseCount(tvCountChildren.getText().toString());
            requestData.roomCategory = getSelectedRoomCategory();
            requestData.isFamilyRoom = switchFamilyRoom.isChecked();
            requestData.hotelPreferences = etHotelPreferences.getText().toString().trim();

            Object selectedHotel = spHotelCategory.getSelectedItem();
            if (selectedHotel != null) {
                requestData.hotelCategory = selectedHotel.toString();
            }

            // Sync dynamic room arrangements
            requestData.roomArrangements = CustomTravelHelper.generateRoomArrangements(
                    requestData.travelType,
                    requestData.roomCategory,
                    requestData.adultMaleCount,
                    requestData.adultFemaleCount,
                    requestData.childrenDetails,
                    requestData.isFamilyRoom
            );
        } else if (currentStage == 3) {
            String airline = etAirlinePreference.getText().toString().trim();
            String flightClass = "economy";
            int flightClassPos = spFlightClass.getSelectedItemPosition();
            if (flightClassPos == 1) flightClass = "premium_economy";
            else if (flightClassPos == 2) flightClass = "business";
            else if (flightClassPos == 3) flightClass = "first";

            String directTransit = "direct";
            int routingId = rgFlightRouting.getCheckedRadioButtonId();
            if (routingId == R.id.rbFlightTransit) directTransit = "transit";
            else if (routingId == R.id.rbFlightAny) directTransit = "any";

            requestData.flightPreferences = new CreateCustomTravelRequest.FlightPreferences(airline, flightClass, directTransit);
            requestData.transportationPreferences = etTransportation.getText().toString().trim();

            if (cbPrivateGuide.isChecked()) {
                requestData.additionalServices.add("private_guide");
            } else {
                requestData.additionalServices.remove("private_guide");
            }

            requestData.itineraryPreferences = etItineraryNotes.getText().toString().trim();

            String budgetStr = etBudgetPerPax.getText().toString().trim();
            if (!budgetStr.isEmpty()) {
                try {
                    requestData.budgetPerPax = Double.parseDouble(budgetStr);
                } catch (Exception ignored) {}
            }

            requestData.specialRequirements = etSpecialRequirements.getText().toString().trim();
        }
    }

    private void updateRoomCategoryAvailability() {
        boolean isUmrah = "umrah".equalsIgnoreCase(requestData.travelType);

        if (isUmrah) {
            // Umrah allows quint, quad, triple, double; prohibits single
            rbRoomQuint.setVisibility(View.VISIBLE);
            rbRoomQuad.setVisibility(View.VISIBLE);
            rbRoomSingle.setVisibility(View.GONE);
            tvUmrahSingleWarning.setVisibility(View.GONE);
            if (rbRoomSingle.isChecked()) {
                rbRoomQuad.setChecked(true);
            }
        } else {
            // Tour allows triple, double, single; quint and quad typically not standard tour
            rbRoomSingle.setVisibility(View.VISIBLE);
            rbRoomQuint.setVisibility(View.GONE);
            rbRoomQuad.setVisibility(View.GONE);
            tvUmrahSingleWarning.setVisibility(View.GONE);
            if (rbRoomQuint.isChecked() || rbRoomQuad.isChecked()) {
                rbRoomDouble.setChecked(true);
            }
        }
    }

    private void updateRoomCategoryStyles() {
        int checkedId = rgRoomCategory.getCheckedRadioButtonId();

        setChipColor(rbRoomQuint, checkedId == R.id.rbRoomQuint);
        setChipColor(rbRoomQuad, checkedId == R.id.rbRoomQuad);
        setChipColor(rbRoomTriple, checkedId == R.id.rbRoomTriple);
        setChipColor(rbRoomDouble, checkedId == R.id.rbRoomDouble);
        setChipColor(rbRoomSingle, checkedId == R.id.rbRoomSingle);
    }

    private void setChipColor(RadioButton rb, boolean selected) {
        if (rb != null) {
            rb.setTextColor(getColor(selected ? R.color.brand_magenta : R.color.text_dark));
            rb.setBackgroundResource(selected ? R.drawable.bg_category_chip_active : R.drawable.bg_category_chip_inactive);
        }
    }

    private String getSelectedRoomCategory() {
        int id = rgRoomCategory.getCheckedRadioButtonId();
        if (id == R.id.rbRoomQuint) return "quint";
        if (id == R.id.rbRoomTriple) return "triple";
        if (id == R.id.rbRoomDouble) return "double";
        if (id == R.id.rbRoomSingle) return "single";
        return "quad";
    }

    private void updateRoomArrangementPreview() {
        String travelType = "umrah".equalsIgnoreCase(requestData.travelType) ? "umrah" : "tour";
        String roomCategory = getSelectedRoomCategory();
        int males = parseCount(tvCountAdultMale.getText().toString());
        int females = parseCount(tvCountAdultFemale.getText().toString());
        boolean isFamily = switchFamilyRoom.isChecked();

        List<CreateCustomTravelRequest.RoomArrangement> arrangements = CustomTravelHelper.generateRoomArrangements(
                travelType,
                roomCategory,
                males,
                females,
                requestData.childrenDetails,
                isFamily
        );

        if (arrangements.isEmpty()) {
            tvGeneratedRoomPreview.setText(R.string.room_preview_empty);
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arrangements.size(); i++) {
            CreateCustomTravelRequest.RoomArrangement arr = arrangements.get(i);
            sb.append("• ").append(RoomLabels.resolve(this, arr, i + 1));
            if (i < arrangements.size() - 1) sb.append("\n");
        }
        tvGeneratedRoomPreview.setText(sb.toString());
    }

    private void syncChildrenCount(int count) {
        int currentSize = requestData.childrenDetails.size();
        if (count > currentSize) {
            for (int i = currentSize; i < count; i++) {
                requestData.childrenDetails.add(new CreateCustomTravelRequest.ChildDetail("", 0, "male"));
            }
        } else if (count < currentSize) {
            while (requestData.childrenDetails.size() > count) {
                requestData.childrenDetails.remove(requestData.childrenDetails.size() - 1);
            }
        }
        renderChildrenViews();
    }

    private void renderChildrenViews() {
        containerChildrenDetails.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < requestData.childrenDetails.size(); i++) {
            final int index = i;
            final CreateCustomTravelRequest.ChildDetail child = requestData.childrenDetails.get(index);
            View childView = inflater.inflate(R.layout.item_wizard_child_entry, containerChildrenDetails, false);

            TextView tvHeader = childView.findViewById(R.id.tvChildHeader);
            TextView tvAgeBadge = childView.findViewById(R.id.tvChildAgeBadge);
            TextView tvDob = childView.findViewById(R.id.tvChildDob);
            RadioGroup rgGender = childView.findViewById(R.id.rgChildGender);
            RadioButton rbMale = childView.findViewById(R.id.rbChildMale);
            RadioButton rbFemale = childView.findViewById(R.id.rbChildFemale);

            tvHeader.setText(getString(R.string.label_child_entry, (index + 1),
                    getString("male".equalsIgnoreCase(child.gender) ? R.string.child_gender_male : R.string.child_gender_female)));

            if (child.dob != null && !child.dob.isEmpty()) {
                tvDob.setText(child.dob);
                int age = CustomTravelHelper.calculateChildAge(child.dob, requestData.preferredDepartureDate);
                child.age = age;
                tvAgeBadge.setText(getString(R.string.label_child_age_calc, age));
            } else {
                tvDob.setText(R.string.label_child_dob);
                tvAgeBadge.setText(R.string.child_age_unknown);
            }

            if ("female".equalsIgnoreCase(child.gender)) {
                rbFemale.setChecked(true);
            } else {
                rbMale.setChecked(true);
            }

            tvDob.setOnClickListener(v -> showChildDatePicker(child, tvDob, tvAgeBadge));

            rgGender.setOnCheckedChangeListener((group, checkedId) -> {
                child.gender = checkedId == R.id.rbChildFemale ? "female" : "male";
                tvHeader.setText(getString(R.string.label_child_entry, (index + 1),
                        getString("male".equalsIgnoreCase(child.gender) ? R.string.child_gender_male : R.string.child_gender_female)));
                updateRoomArrangementPreview();
            });

            containerChildrenDetails.addView(childView);
        }
    }

    private void showDatePicker(boolean isDeparture) {
        String todayIso = dateFormat.format(new Date());
        String minDate;
        String title;
        String sub;
        String[] holder;

        if (isDeparture) {
            minDate = todayIso;
            title = getString(R.string.label_pref_dep_date);
            sub = getString(R.string.hint_select_departure_date_sub);
            holder = new String[]{requestData.preferredDepartureDate != null ? requestData.preferredDepartureDate : ""};
        } else {
            minDate = (requestData.preferredDepartureDate != null && !requestData.preferredDepartureDate.trim().isEmpty())
                    ? requestData.preferredDepartureDate : todayIso;
            title = getString(R.string.label_pref_ret_date);
            sub = getString(R.string.hint_select_return_date_sub);
            holder = new String[]{requestData.preferredReturnDate != null ? requestData.preferredReturnDate : ""};
        }

        DatePickerBottomSheetHelper.showTravelDatePicker(
                this,
                title,
                sub,
                minDate,
                isDeparture ? tvDepartureDate : tvReturnDate,
                holder,
                (isoDate, formattedDisplayDate, age) -> {
                    if (isDeparture) {
                        requestData.preferredDepartureDate = isoDate;
                        if (requestData.preferredReturnDate != null && requestData.preferredReturnDate.compareTo(isoDate) < 0) {
                            requestData.preferredReturnDate = null;
                            tvReturnDate.setText(R.string.hint_select_date);
                            tvReturnDate.setTextColor(getColor(R.color.text_muted));
                        }
                        renderChildrenViews();
                        updateRoomArrangementPreview();
                    } else {
                        requestData.preferredReturnDate = isoDate;
                    }
                    calculateDurationFromDates();
                }
        );
    }

    private void showChildDatePicker(CreateCustomTravelRequest.ChildDetail child, TextView tvDob, TextView tvAgeBadge) {
        String[] holder = new String[]{child.dob != null ? child.dob : ""};
        String title = getString(R.string.label_child_dob);
        String sub = getString(R.string.profile_dob_sheet_sub);

        DatePickerBottomSheetHelper.show(
                this,
                title,
                sub,
                false,
                true,
                tvDob,
                null,
                holder,
                (isoDate, formattedDisplayDate, age) -> {
                    child.dob = isoDate;
                    int calcAge = CustomTravelHelper.calculateChildAge(isoDate, requestData.preferredDepartureDate);
                    child.age = calcAge;
                    tvAgeBadge.setText(getString(R.string.label_child_age_calc, calcAge));
                    updateRoomArrangementPreview();
                }
        );
    }

    private void calculateDurationFromDates() {
        String depStr = requestData.preferredDepartureDate;
        String retStr = requestData.preferredReturnDate;

        if (depStr == null || retStr == null || depStr.trim().isEmpty() || retStr.trim().isEmpty()) {
            return;
        }

        try {
            Date dep = dateFormat.parse(depStr);
            Date ret = dateFormat.parse(retStr);
            if (dep != null && ret != null && !ret.before(dep)) {
                long diffMs = ret.getTime() - dep.getTime();
                long days = (diffMs / (1000 * 60 * 60 * 24)) + 1;
                etDurationDays.setText(String.valueOf(days));
                requestData.durationDays = (int) days;
            }
        } catch (Exception ignored) {}
    }

    private void populateSummaryStage() {
        collectCurrentStageData();

        // 1. Overview
        StringBuilder ov = new StringBuilder();
        ov.append(getString(R.string.summary_category_format,
                getString("umrah".equalsIgnoreCase(requestData.travelType) ? R.string.travel_type_umrah : R.string.travel_type_tour))).append("\n");
        if (basePackageName != null) {
            ov.append(getString(R.string.summary_base_package_format, basePackageName)).append("\n");
        }
        ov.append(getString(R.string.summary_destination_format, requestData.destination != null && !requestData.destination.trim().isEmpty() ? requestData.destination : "-")).append("\n");
        ov.append(getString(R.string.summary_airport_format, requestData.departureAirport != null && !requestData.departureAirport.trim().isEmpty() ? requestData.departureAirport : "-")).append("\n");
        ov.append(getString(R.string.summary_dates_format,
                requestData.preferredDepartureDate != null && !requestData.preferredDepartureDate.trim().isEmpty() ? requestData.preferredDepartureDate : "-",
                requestData.preferredReturnDate != null && !requestData.preferredReturnDate.trim().isEmpty() ? requestData.preferredReturnDate : "-"));
        if (requestData.isFlexibleDates != null && requestData.isFlexibleDates) {
            ov.append(getString(R.string.summary_flexible_dates_suffix));
        }
        ov.append("\n").append(getString(R.string.summary_duration_format,
                requestData.durationDays != null && requestData.durationDays > 0 ? String.valueOf(requestData.durationDays) : "-"));
        tvSummaryOverview.setText(ov.toString());

        // 2. Travellers & Rooms
        StringBuilder tr = new StringBuilder();
        int totalPax = requestData.adultMaleCount + requestData.adultFemaleCount + requestData.childrenCount;
        tr.append(getString(R.string.summary_traveller_counts, totalPax, requestData.adultMaleCount,
                requestData.adultFemaleCount, requestData.childrenCount)).append("\n");
        tr.append(getString(R.string.summary_room_type_format,
                RoomLabels.resolve(this, requestData.roomCategory, "QUAD"))).append("\n");
        tr.append(getString(R.string.summary_family_room_format,
                getString(requestData.isFamilyRoom != null && requestData.isFamilyRoom
                        ? R.string.summary_family_room_yes : R.string.summary_family_room_no))).append("\n");
        tr.append(getString(R.string.summary_suggested_rooms)).append("\n");
        if (requestData.roomArrangements != null && !requestData.roomArrangements.isEmpty()) {
            for (int i = 0; i < requestData.roomArrangements.size(); i++) {
                tr.append("  • ").append(RoomLabels.resolve(this,
                        requestData.roomArrangements.get(i), i + 1)).append("\n");
            }
        } else {
            tr.append("  • ").append(getString(R.string.summary_room_fallback)).append("\n");
        }
        if (requestData.hotelCategory != null) {
            tr.append(getString(R.string.summary_hotel_rating_format, requestData.hotelCategory));
        }
        tvSummaryTravellersRooms.setText(tr.toString().trim());

        // 3. Custom Preferences & Budget
        StringBuilder cp = new StringBuilder();
        if (requestData.flightPreferences != null) {
            String airline = requestData.flightPreferences.airline != null && !requestData.flightPreferences.airline.isEmpty()
                    ? requestData.flightPreferences.airline : getString(R.string.preference_any);
            cp.append(getString(R.string.summary_flight_format, airline,
                    localizedFlightClass(requestData.flightPreferences.flightClass),
                    localizedFlightRouting(requestData.flightPreferences.directTransit))).append("\n");
        }
        if (requestData.transportationPreferences != null && !requestData.transportationPreferences.isEmpty()) {
            cp.append(getString(R.string.summary_transport_format, requestData.transportationPreferences)).append("\n");
        }
        if (requestData.additionalServices.contains("private_guide")) {
            cp.append(getString(R.string.summary_private_guide)).append("\n");
        }
        if (requestData.budgetPerPax != null && requestData.budgetPerPax > 0) {
            cp.append(getString(R.string.summary_budget_format,
                    String.format(Locale.US, "%,.2f", requestData.budgetPerPax))).append("\n");
        }
        if (requestData.specialRequirements != null && !requestData.specialRequirements.isEmpty()) {
            cp.append(getString(R.string.summary_special_requirements_format, requestData.specialRequirements));
        }
        tvSummaryPreferences.setText(cp.toString().trim());
    }

    private String localizedFlightClass(String value) {
        if ("premium_economy".equalsIgnoreCase(value)) return getString(R.string.flight_class_premium_economy);
        if ("business".equalsIgnoreCase(value)) return getString(R.string.flight_class_business);
        if ("first".equalsIgnoreCase(value)) return getString(R.string.flight_class_first);
        return getString(R.string.flight_class_economy);
    }

    private String localizedFlightRouting(String value) {
        if ("transit".equalsIgnoreCase(value)) return getString(R.string.flight_routing_transit);
        if ("any".equalsIgnoreCase(value)) return getString(R.string.flight_routing_any);
        return getString(R.string.flight_routing_direct);
    }

    private void submitCustomTravelRequest() {
        if (isSubmitting) return;

        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            Toast.makeText(this, R.string.login_required_custom_request, Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            return;
        }

        collectCurrentStageData();
        isSubmitting = true;
        btnNextStage.setEnabled(false);
        btnNextStage.setText(R.string.submitting_request);

        if (isEditMode && existingRequest != null) {
            ApiClient.getApiService().updateCustomTravelRequest(existingRequest.id, requestData)
                    .enqueue(new Callback<ApiResponse<CustomTravelRequestDto>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<CustomTravelRequestDto>> call, Response<ApiResponse<CustomTravelRequestDto>> response) {
                            isSubmitting = false;
                            btnNextStage.setEnabled(true);
                            btnNextStage.setText(R.string.btn_update_request);

                            if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                                Toast.makeText(CustomTravelWizardActivity.this, R.string.request_updated_success, Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                String err = ApiErrors.userMessage(CustomTravelWizardActivity.this, response, R.string.err_validation);
                                Toast.makeText(CustomTravelWizardActivity.this, err, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<CustomTravelRequestDto>> call, Throwable t) {
                            isSubmitting = false;
                            btnNextStage.setEnabled(true);
                            btnNextStage.setText(R.string.btn_update_request);
                            Toast.makeText(CustomTravelWizardActivity.this, ApiErrors.userMessage(CustomTravelWizardActivity.this, t, R.string.err_server_connection), Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            ApiClient.getApiService().createCustomTravelRequest(requestData)
                    .enqueue(new Callback<ApiResponse<CustomTravelRequestDto>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<CustomTravelRequestDto>> call, Response<ApiResponse<CustomTravelRequestDto>> response) {
                            isSubmitting = false;
                            btnNextStage.setEnabled(true);
                            btnNextStage.setText(R.string.btn_submit_request);

                            if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                                CustomTravelRequestDto created = response.body().data;
                                showSubmissionSuccessDialog(created.requestNo);
                            } else {
                                String err = ApiErrors.userMessage(CustomTravelWizardActivity.this, response, R.string.err_validation);
                                Toast.makeText(CustomTravelWizardActivity.this, err, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<CustomTravelRequestDto>> call, Throwable t) {
                            isSubmitting = false;
                            btnNextStage.setEnabled(true);
                            btnNextStage.setText(R.string.btn_submit_request);
                            Toast.makeText(CustomTravelWizardActivity.this, ApiErrors.userMessage(CustomTravelWizardActivity.this, t, R.string.err_server_connection), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void showSubmissionSuccessDialog(String requestNo) {
        String message = getString(R.string.dialog_submission_success_message, requestNo != null ? requestNo : "CRQ");

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_submission_success_title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.btn_view_my_quotations, (dialog, which) -> {
                    dialog.dismiss();
                    Intent intent = new Intent(CustomTravelWizardActivity.this, MyBookingsActivity.class);
                    intent.putExtra("extra_default_tab", "quotations");
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(R.string.dialog_close, (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .show();
    }

    private void handleBackNavigation() {
        if (currentStage > 1) {
            renderStage(currentStage - 1);
        } else {
            finish();
        }
    }

    private int parseCount(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
