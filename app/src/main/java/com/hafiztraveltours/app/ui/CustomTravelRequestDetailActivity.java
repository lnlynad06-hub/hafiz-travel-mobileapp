package com.hafiztraveltours.app.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.models.CreateCustomTravelRequest;
import com.hafiztraveltours.app.models.CustomTravelRequestDto;
import com.hafiztraveltours.app.models.QuotationDto;
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiErrors;
import com.hafiztraveltours.app.utils.RoomLabels;

import java.io.Serializable;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomTravelRequestDetailActivity extends BaseActivity {

    public static final String EXTRA_REQUEST_ID = "extra_request_id";
    public static final String EXTRA_REQUEST_DTO = "extra_request_dto";

    private int requestId = 0;
    private CustomTravelRequestDto requestDto = null;

    private TextView tvRequestNo;
    private TextView tvCreatedDate;
    private TextView tvStatusBadge;
    private CardView cardOfficialQuotation;
    private TextView tvQuotationNo;
    private TextView tvQuotationTotal;
    private TextView tvQuotationValidity;
    private TextView tvStatusNoticeBanner;
    private TextView tvDetailOverview;
    private TextView tvDetailTravellersRooms;
    private TextView tvDetailPreferences;
    private Button btnDetailEdit;
    private Button btnDetailCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_travel_request_detail);

        bindViews();
        parseIntent();
        loadFreshDetails();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (requestId > 0) {
            loadFreshDetails();
        }
    }

    private void bindViews() {
        ImageButton btnBack = findViewById(R.id.btnDetailBack);
        btnBack.setOnClickListener(v -> finish());

        tvRequestNo = findViewById(R.id.tvDetailRequestNo);
        tvCreatedDate = findViewById(R.id.tvDetailCreatedDate);
        tvStatusBadge = findViewById(R.id.tvDetailStatusBadge);
        cardOfficialQuotation = findViewById(R.id.cardOfficialQuotation);
        tvQuotationNo = findViewById(R.id.tvQuotationNo);
        tvQuotationTotal = findViewById(R.id.tvQuotationTotal);
        tvQuotationValidity = findViewById(R.id.tvQuotationValidity);
        tvStatusNoticeBanner = findViewById(R.id.tvStatusNoticeBanner);
        tvDetailOverview = findViewById(R.id.tvDetailOverview);
        tvDetailTravellersRooms = findViewById(R.id.tvDetailTravellersRooms);
        tvDetailPreferences = findViewById(R.id.tvDetailPreferences);
        btnDetailEdit = findViewById(R.id.btnDetailEdit);
        btnDetailCancel = findViewById(R.id.btnDetailCancel);

        btnDetailEdit.setOnClickListener(v -> {
            if (requestDto != null) {
                if (!requestDto.isEditable()) {
                    Toast.makeText(this, R.string.notice_request_under_review, Toast.LENGTH_LONG).show();
                    return;
                }
                Intent intent = new Intent(this, CustomTravelWizardActivity.class);
                intent.putExtra(CustomTravelWizardActivity.EXTRA_EXISTING_REQUEST, requestDto);
                startActivity(intent);
            }
        });

        btnDetailCancel.setOnClickListener(v -> confirmCancellation());
    }

    private void parseIntent() {
        Intent intent = getIntent();
        if (intent == null) return;

        requestId = intent.getIntExtra(EXTRA_REQUEST_ID, 0);
        Serializable ser = intent.getSerializableExtra(EXTRA_REQUEST_DTO);
        if (ser instanceof CustomTravelRequestDto) {
            requestDto = (CustomTravelRequestDto) ser;
            if (requestId == 0) requestId = requestDto.id;
            bindData(requestDto);
        }
    }

    private void loadFreshDetails() {
        if (requestId <= 0) return;

        ApiClient.getApiService().getCustomTravelRequestDetail(requestId)
                .enqueue(new Callback<ApiResponse<CustomTravelRequestDto>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<CustomTravelRequestDto>> call, Response<ApiResponse<CustomTravelRequestDto>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                            requestDto = response.body().data;
                            bindData(requestDto);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<CustomTravelRequestDto>> call, Throwable t) {
                        // Silent fallback to cached dto if network fails
                    }
                });
    }

    private void bindData(CustomTravelRequestDto dto) {
        if (dto == null) return;

        tvRequestNo.setText(dto.requestNo != null ? dto.requestNo : "CRQ-" + dto.id);
        tvCreatedDate.setText(dto.createdAt != null ? getString(R.string.request_submitted_on,
                dto.createdAt.substring(0, Math.min(10, dto.createdAt.length()))) : "");
        tvStatusBadge.setText(dto.getStatusLocalized(this));

        // Status Banner & Locked state feedback
        if ("under_review".equalsIgnoreCase(dto.status)) {
            tvStatusNoticeBanner.setVisibility(View.VISIBLE);
            tvStatusNoticeBanner.setText(R.string.notice_request_under_review);
        } else if ("submitted".equalsIgnoreCase(dto.status)) {
            tvStatusNoticeBanner.setVisibility(View.VISIBLE);
            tvStatusNoticeBanner.setText(R.string.notice_request_submitted);
        } else if ("cancelled".equalsIgnoreCase(dto.status)) {
            tvStatusNoticeBanner.setVisibility(View.VISIBLE);
            tvStatusNoticeBanner.setText(R.string.notice_request_cancelled);
        } else {
            tvStatusNoticeBanner.setVisibility(View.GONE);
        }

        // Official Quotation Card (Only when customer-visible)
        if (dto.hasCustomerVisibleQuotation()) {
            QuotationDto quo = dto.latestQuotation;
            cardOfficialQuotation.setVisibility(View.VISIBLE);
            tvQuotationNo.setText(getString(R.string.label_official_quotation_no, quo.quotationNo));
            tvQuotationTotal.setText(getString(R.string.label_quotation_total, String.format(Locale.US, "RM %,.2f", quo.totalAmount)));
            tvQuotationValidity.setText(getString(R.string.label_quotation_valid_until, quo.validUntil != null ? quo.validUntil : "-"));
        } else {
            cardOfficialQuotation.setVisibility(View.GONE);
        }

        // 1. Overview
        StringBuilder ov = new StringBuilder();
        ov.append(getString(R.string.summary_category_format,
                getString("umrah".equalsIgnoreCase(dto.travelType) ? R.string.travel_type_umrah : R.string.travel_type_tour))).append("\n");
        if (dto.getDisplayTitle() != null && !dto.getDisplayTitle().equals(dto.destination)) {
            ov.append(getString(R.string.summary_base_package_format, dto.getDisplayTitle())).append("\n");
        }
        ov.append(getString(R.string.summary_destination_format, dto.destination != null ? dto.destination : "-")).append("\n");
        ov.append(getString(R.string.summary_airport_format, dto.departureAirport != null && !dto.departureAirport.trim().isEmpty() ? dto.departureAirport : "-")).append("\n");
        ov.append(getString(R.string.summary_dates_format,
                dto.preferredDepartureDate != null ? dto.preferredDepartureDate : "-",
                dto.preferredReturnDate != null ? dto.preferredReturnDate : "-"));
        if (dto.isFlexibleDates) {
            ov.append(getString(R.string.summary_flexible_dates_suffix));
        }
        ov.append("\n").append(getString(R.string.summary_duration_format,
                dto.durationDays != null && dto.durationDays > 0 ? String.valueOf(dto.durationDays) : "-"));
        tvDetailOverview.setText(ov.toString());

        // 2. Travellers & Rooms
        StringBuilder tr = new StringBuilder();
        int totalPax = dto.getTotalPax();
        tr.append(getString(R.string.summary_traveller_counts, totalPax, dto.adultMaleCount,
                dto.adultFemaleCount, dto.childrenCount)).append("\n");
        tr.append(getString(R.string.summary_room_type_format,
                RoomLabels.resolve(this, dto.roomCategory, "QUAD"))).append("\n");
        tr.append(getString(R.string.summary_family_room_format,
                getString(dto.isFamilyRoom ? R.string.summary_family_room_yes : R.string.summary_family_room_no))).append("\n");
        tr.append(getString(R.string.summary_requested_rooms)).append("\n");
        if (dto.roomArrangements != null && !dto.roomArrangements.isEmpty()) {
            for (int i = 0; i < dto.roomArrangements.size(); i++) {
                tr.append("  • ").append(RoomLabels.resolve(this, dto.roomArrangements.get(i), i + 1)).append("\n");
            }
        } else {
            tr.append("  • ").append(getString(R.string.summary_room_fallback)).append("\n");
        }
        if (dto.hotelCategory != null) {
            tr.append(getString(R.string.summary_hotel_rating_format, dto.hotelCategory));
        }
        if (dto.hotelPreferences != null && !dto.hotelPreferences.isEmpty()) {
            tr.append("\n").append(getString(R.string.summary_hotel_notes_format, dto.hotelPreferences));
        }
        tvDetailTravellersRooms.setText(tr.toString().trim());

        // 3. Custom Preferences & Budget
        StringBuilder cp = new StringBuilder();
        if (dto.flightPreferences != null) {
            String airline = dto.flightPreferences.airline != null && !dto.flightPreferences.airline.isEmpty()
                    ? dto.flightPreferences.airline : getString(R.string.preference_any);
            cp.append(getString(R.string.summary_flight_format, airline,
                    localizedFlightClass(dto.flightPreferences.flightClass),
                    localizedFlightRouting(dto.flightPreferences.directTransit))).append("\n");
        }
        if (dto.transportationPreferences != null && !dto.transportationPreferences.isEmpty()) {
            cp.append(getString(R.string.summary_transport_format, dto.transportationPreferences)).append("\n");
        }
        if (dto.additionalServices != null && dto.additionalServices.contains("private_guide")) {
            cp.append(getString(R.string.summary_private_guide)).append("\n");
        }
        if (dto.budgetPerPax != null && dto.budgetPerPax > 0) {
            cp.append(getString(R.string.summary_budget_format,
                    String.format(Locale.US, "%,.2f", dto.budgetPerPax))).append("\n");
        }
        if (dto.specialRequirements != null && !dto.specialRequirements.isEmpty()) {
            cp.append(getString(R.string.summary_special_requirements_format, dto.specialRequirements));
        }
        tvDetailPreferences.setText(cp.toString().trim());

        // Action Buttons Visibility
        btnDetailEdit.setVisibility(dto.isEditable() ? View.VISIBLE : View.GONE);
        btnDetailCancel.setVisibility(dto.isCancellable() ? View.VISIBLE : View.GONE);
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

    private void confirmCancellation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.btn_cancel_request)
                .setMessage(R.string.confirm_cancel_custom_request)
                .setPositiveButton(R.string.confirm_yes_cancel, (dialog, which) -> executeCancellation())
                .setNegativeButton(R.string.action_no, null)
                .show();
    }

    private void executeCancellation() {
        if (requestId <= 0) return;

        btnDetailCancel.setEnabled(false);
        ApiClient.getApiService().cancelCustomTravelRequest(requestId)
                .enqueue(new Callback<ApiResponse<CustomTravelRequestDto>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<CustomTravelRequestDto>> call, Response<ApiResponse<CustomTravelRequestDto>> response) {
                        btnDetailCancel.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                            Toast.makeText(CustomTravelRequestDetailActivity.this, R.string.request_cancelled_success, Toast.LENGTH_SHORT).show();
                            requestDto = response.body().data;
                            bindData(requestDto);
                        } else {
                            String err = ApiErrors.userMessage(CustomTravelRequestDetailActivity.this, response, R.string.err_server);
                            Toast.makeText(CustomTravelRequestDetailActivity.this, err, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<CustomTravelRequestDto>> call, Throwable t) {
                        btnDetailCancel.setEnabled(true);
                        Toast.makeText(CustomTravelRequestDetailActivity.this, ApiErrors.userMessage(CustomTravelRequestDetailActivity.this, t, R.string.err_server_connection), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
