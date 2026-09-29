package com.hafiztraveltours.app.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.adapters.MyBookingsAdapter;
import com.hafiztraveltours.app.models.BookingDetailDto;
import com.hafiztraveltours.app.models.BookingDocumentsResponse;
import com.hafiztraveltours.app.models.BookingDto;
import com.hafiztraveltours.app.models.BookingListPage;
import com.hafiztraveltours.app.models.BookingRequest;
import com.hafiztraveltours.app.models.CancelBookingRequest;
import com.hafiztraveltours.app.models.DocumentDto;
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiErrors;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.utils.BottomNavHelper;
import com.hafiztraveltours.app.utils.DocumentStatus;
import com.hafiztraveltours.app.utils.HapticUtil;
import com.hafiztraveltours.app.utils.SessionManager;
import com.hafiztraveltours.app.utils.ViewStateController;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyBookingsActivity extends BaseActivity {

    private MyBookingsAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyContainer;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private ViewStateController viewState;
    private Call<?> bookingsCall;

    // Active bottom sheet context
    private BookingDto activeBooking;
    private Integer activeTravellerId = null;
    private String activeDocCode = null;
    private String activeDocTitle = null;
    private BottomSheetDialog activeDocsDialog;
    private View activeSheetView;
    private Uri pendingUploadUri = null;
    private TextView pendingFileNameView = null;
    private TextView pendingConfirmButton = null;

    private final ActivityResultLauncher<String> docPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    pendingUploadUri = uri;
                    String fileName = getFileName(uri);
                    if (pendingFileNameView != null) {
                        pendingFileNameView.setText(fileName);
                        View parent = (View) pendingFileNameView.getParent();
                        if (parent != null) parent.setVisibility(View.VISIBLE);
                    }
                    if (pendingConfirmButton != null) {
                        pendingConfirmButton.setEnabled(true);
                        pendingConfirmButton.setAlpha(1.0f);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_bookings);
        BottomNavHelper.setup(this, BottomNavHelper.Tab.BOOKING);

        findViewById(R.id.bookingsBackButton).setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.bookingsRecyclerView);
        emptyContainer = findViewById(R.id.bookingsEmptyContainer);
        progressBar = findViewById(R.id.bookingsProgressBar);
        swipeRefresh = findViewById(R.id.bookingsSwipeRefresh);

        TextView emptyText = findViewById(R.id.bookingsEmptyText);
        if (emptyText != null) emptyText.setText(getString(R.string.profile_no_booking));

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyBookingsAdapter(new ArrayList<>());
        adapter.setOnDocumentClickListener(this::showBookingDocsSheet);
        adapter.setOnPayClickListener(this::showQuickPaySheet);
        adapter.setOnCancelClickListener(this::showCancelBookingConfirmation);
        recyclerView.setAdapter(adapter);
        viewState = new ViewStateController(progressBar, recyclerView, emptyContainer);

        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeResources(
                    R.color.brand_magenta,
                    R.color.gold_accent,
                    R.color.brand_dark_pink);
            swipeRefresh.setOnRefreshListener(this::loadBookings);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBookings();
    }

    @Override
    protected void onDestroy() {
        if (bookingsCall != null) bookingsCall.cancel();
        super.onDestroy();
    }

    private void loadBookings() {
        if (!SessionManager.getInstance(this).isLoggedIn()) {
            showEmpty();
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            return;
        }
        if (swipeRefresh == null || !swipeRefresh.isRefreshing()) {
            viewState.showLoading();
        }

        if (bookingsCall != null) bookingsCall.cancel();
        Call<ApiResponse<BookingListPage>> call = ApiClient.getApiService().getBookings(null, 20);
        bookingsCall = call;
        call.enqueue(new Callback<ApiResponse<BookingListPage>>() {
            @Override
            public void onResponse(Call<ApiResponse<BookingListPage>> call,
                                   Response<ApiResponse<BookingListPage>> response) {
                if (isFinishing() || isDestroyed()) return;
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);

                BookingListPage page = (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) ? response.body().data : null;
                if (page != null && page.data != null && !page.data.isEmpty()) {
                    adapter.setItems(page.data);
                    viewState.showContent();
                } else if (page != null) {
                    showEmpty();
                } else {
                    Toast.makeText(MyBookingsActivity.this,
                            com.hafiztraveltours.app.network.ApiErrors.userMessage(MyBookingsActivity.this, response, R.string.err_network), Toast.LENGTH_SHORT).show();
                    if (adapter.getItemCount() == 0) showEmpty();
                    else viewState.showContent();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<BookingListPage>> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                Toast.makeText(MyBookingsActivity.this,
                        com.hafiztraveltours.app.network.ApiErrors.userMessage(MyBookingsActivity.this, t, R.string.err_network), Toast.LENGTH_SHORT).show();
                if (adapter.getItemCount() == 0) showEmpty();
                else viewState.showContent();
            }
        });
    }

    private void showEmpty() {
        viewState.showEmpty();
    }

    public void showQuickPaySheet(BookingDto booking) {
        if (booking == null || booking.id <= 0 || booking.balanceAmount <= 0) return;

        BottomSheetDialog payDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_quick_pay, null);
        payDialog.setContentView(sheetView);

        TextView tvPkgName = sheetView.findViewById(R.id.tvPayBookingPackageName);
        TextView tvBookingRef = sheetView.findViewById(R.id.tvPayBookingRef);
        TextView tvRemainingDue = sheetView.findViewById(R.id.tvPayTotalRemainingAmount);
        ImageView btnClose = sheetView.findViewById(R.id.btnClosePaySheet);

        View cardPreset = sheetView.findViewById(R.id.cardPayOptionPreset);
        ImageView ivCheckPreset = sheetView.findViewById(R.id.ivCheckPayPreset);
        TextView tvPresetLabel = sheetView.findViewById(R.id.tvPayPresetLabel);

        View cardCustom = sheetView.findViewById(R.id.cardPayOptionCustom);
        ImageView ivCheckCustom = sheetView.findViewById(R.id.ivCheckPayCustom);
        View layoutCustomInput = sheetView.findViewById(R.id.layoutCustomAmountInput);
        EditText etCustom = sheetView.findViewById(R.id.etCustomAmount);

        View cardFpx = sheetView.findViewById(R.id.cardPayMethodFpx);
        ImageView ivCheckFpx = sheetView.findViewById(R.id.ivCheckMethodFpx);
        View cardCard = sheetView.findViewById(R.id.cardPayMethodCard);
        ImageView ivCheckCard = sheetView.findViewById(R.id.ivCheckMethodCard);

        if (ivCheckPreset != null) ivCheckPreset.clearColorFilter();
        if (ivCheckCustom != null) ivCheckCustom.clearColorFilter();
        if (ivCheckFpx != null) ivCheckFpx.clearColorFilter();
        if (ivCheckCard != null) ivCheckCard.clearColorFilter();

        MaterialButton btnSubmit = sheetView.findViewById(R.id.btnSubmitQuickPay);
        ProgressBar pbLoading = sheetView.findViewById(R.id.pbPayLoading);

        if (btnClose != null) btnClose.setOnClickListener(v -> payDialog.dismiss());

        if (tvPkgName != null) tvPkgName.setText(booking.packageName != null ? booking.packageName : getString(R.string.booking_category_umrah));
        if (tvBookingRef != null) tvBookingRef.setText(booking.bookingNo != null ? booking.bookingNo : "BKG-" + booking.id);
        if (tvRemainingDue != null) tvRemainingDue.setText(BookingRequest.formatPrice(booking.balanceAmount));

        // Determine preset stage amount
        final double presetAmount;
        if (booking.depositRemaining > 0) {
            presetAmount = booking.depositRemaining;
            if (tvPresetLabel != null) {
                tvPresetLabel.setText(getString(R.string.pay_sheet_opt_deposit_preset, BookingRequest.formatPrice(presetAmount)));
            }
        } else {
            presetAmount = booking.balanceAmount;
            if (tvPresetLabel != null) {
                tvPresetLabel.setText(getString(R.string.pay_sheet_opt_balance_preset, BookingRequest.formatPrice(presetAmount)));
            }
        }

        final boolean[] isCustomSelected = {false};
        final String[] selectedMethod = {"fpx"};

        Runnable updateSubmitButtonText = () -> {
            if (btnSubmit == null) return;
            if (!isCustomSelected[0]) {
                btnSubmit.setText(getString(R.string.pay_sheet_btn_confirm, BookingRequest.formatPrice(presetAmount)));
            } else {
                String input = etCustom != null ? etCustom.getText().toString().trim() : "";
                double customAmt = 0;
                try {
                    customAmt = Double.parseDouble(input);
                } catch (Exception ignored) {}
                if (customAmt > 0) {
                    btnSubmit.setText(getString(R.string.pay_sheet_btn_confirm, BookingRequest.formatPrice(customAmt)));
                } else {
                    btnSubmit.setText(getString(R.string.booking_btn_pay_now));
                }
            }
        };

        updateSubmitButtonText.run();

        // Option 1 Preset Click
        if (cardPreset != null) {
            cardPreset.setOnClickListener(v -> {
                HapticUtil.click(v);
                isCustomSelected[0] = false;
                cardPreset.setBackgroundResource(R.drawable.bg_selection_card_selected);
                if (ivCheckPreset != null) {
                    ivCheckPreset.setImageResource(R.drawable.ic_checkbox_checked_pink);
                    ivCheckPreset.clearColorFilter();
                }

                if (cardCustom != null) cardCustom.setBackgroundResource(R.drawable.bg_selection_card_unselected);
                if (ivCheckCustom != null) {
                    ivCheckCustom.setImageResource(R.drawable.ic_checkbox_unchecked);
                    ivCheckCustom.clearColorFilter();
                }
                if (layoutCustomInput != null) layoutCustomInput.setVisibility(View.GONE);
                updateSubmitButtonText.run();
            });
        }

        // Option 2 Custom Click
        if (cardCustom != null) {
            cardCustom.setOnClickListener(v -> {
                HapticUtil.click(v);
                isCustomSelected[0] = true;
                cardCustom.setBackgroundResource(R.drawable.bg_selection_card_selected);
                if (ivCheckCustom != null) {
                    ivCheckCustom.setImageResource(R.drawable.ic_checkbox_checked_pink);
                    ivCheckCustom.clearColorFilter();
                }

                if (cardPreset != null) cardPreset.setBackgroundResource(R.drawable.bg_selection_card_unselected);
                if (ivCheckPreset != null) {
                    ivCheckPreset.setImageResource(R.drawable.ic_checkbox_unchecked);
                    ivCheckPreset.clearColorFilter();
                }
                if (layoutCustomInput != null) layoutCustomInput.setVisibility(View.VISIBLE);
                if (etCustom != null) etCustom.requestFocus();
                updateSubmitButtonText.run();
            });
        }

        if (etCustom != null) {
            etCustom.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (isCustomSelected[0]) updateSubmitButtonText.run();
                }
                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }

        // Payment Method selection (Only supported gateway providers)
        View.OnClickListener methodClickListener = v -> {
            HapticUtil.click(v);
            if (v == cardFpx) {
                selectedMethod[0] = "fpx";
            } else if (v == cardCard) {
                selectedMethod[0] = "card";
            }

            // Update visuals
            if (cardFpx != null) cardFpx.setBackgroundResource("fpx".equals(selectedMethod[0]) ? R.drawable.bg_selection_card_selected : R.drawable.bg_selection_card_unselected);
            if (ivCheckFpx != null) {
                ivCheckFpx.setImageResource("fpx".equals(selectedMethod[0]) ? R.drawable.ic_checkbox_checked_pink : R.drawable.ic_checkbox_unchecked);
                ivCheckFpx.clearColorFilter();
            }

            if (cardCard != null) cardCard.setBackgroundResource("card".equals(selectedMethod[0]) ? R.drawable.bg_selection_card_selected : R.drawable.bg_selection_card_unselected);
            if (ivCheckCard != null) {
                ivCheckCard.setImageResource("card".equals(selectedMethod[0]) ? R.drawable.ic_checkbox_checked_pink : R.drawable.ic_checkbox_unchecked);
                ivCheckCard.clearColorFilter();
            }
        };

        if (cardFpx != null) cardFpx.setOnClickListener(methodClickListener);
        if (cardCard != null) cardCard.setOnClickListener(methodClickListener);

        // Submit Button Click
        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                HapticUtil.click(v);
                double finalAmount;
                if (!isCustomSelected[0]) {
                    finalAmount = presetAmount;
                } else {
                    String amtStr = etCustom != null ? etCustom.getText().toString().trim() : "";
                    try {
                        finalAmount = Double.parseDouble(amtStr);
                    } catch (Exception e) {
                        Toast.makeText(MyBookingsActivity.this, getString(R.string.pay_sheet_err_min_amount), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (finalAmount <= 0) {
                        Toast.makeText(MyBookingsActivity.this, getString(R.string.pay_sheet_err_min_amount), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (finalAmount > booking.balanceAmount + 0.01) {
                        Toast.makeText(MyBookingsActivity.this, getString(R.string.pay_sheet_err_max_amount, BookingRequest.formatPrice(booking.balanceAmount)), Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                // Show loading
                btnSubmit.setEnabled(false);
                btnSubmit.setText("");
                if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);

                Map<String, Object> body = new HashMap<>();
                body.put("amount", finalAmount);
                body.put("payment_method", selectedMethod[0]);

                ApiClient.getApiService().payBooking(booking.id, body).enqueue(new Callback<ApiResponse<com.hafiztraveltours.app.models.BookingDetailDto>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<com.hafiztraveltours.app.models.BookingDetailDto>> call, Response<ApiResponse<com.hafiztraveltours.app.models.BookingDetailDto>> response) {
                        if (isFinishing() || isDestroyed()) return;
                        if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                        btnSubmit.setEnabled(true);
                        updateSubmitButtonText.run();

                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            payDialog.dismiss();
                            new AlertDialog.Builder(MyBookingsActivity.this)
                                    .setTitle(getString(R.string.receipt_title_official))
                                    .setMessage(getString(R.string.pay_sheet_success, BookingRequest.formatPrice(finalAmount)))
                                    .setPositiveButton(getString(R.string.pay_btn_view_receipt), (d, w) -> {
                                        d.dismiss();
                                        Intent recIntent = new Intent(MyBookingsActivity.this, ReceiptViewerActivity.class);
                                        recIntent.putExtra(ReceiptViewerActivity.EXTRA_BOOKING_ID, booking.id);
                                        recIntent.putExtra(ReceiptViewerActivity.EXTRA_BOOKING_NO, booking.bookingNo);
                                        startActivity(recIntent);
                                    })
                                    .setNegativeButton(getString(R.string.dialog_btn_close), (d, w) -> d.dismiss())
                                    .show();
                            loadBookings();
                        } else {
                            Toast.makeText(MyBookingsActivity.this,
                                    com.hafiztraveltours.app.network.ApiErrors.userMessage(MyBookingsActivity.this, response, R.string.err_network), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<com.hafiztraveltours.app.models.BookingDetailDto>> call, Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                        btnSubmit.setEnabled(true);
                        updateSubmitButtonText.run();
                        Toast.makeText(MyBookingsActivity.this,
                                com.hafiztraveltours.app.network.ApiErrors.userMessage(MyBookingsActivity.this, t, R.string.err_network), Toast.LENGTH_LONG).show();
                    }
                });
            });
        }

        payDialog.show();
    }

    public void showBookingDocsSheet(BookingDto booking) {
        if (booking == null || booking.id <= 0) return;

        activeBooking = booking;
        activeTravellerId = null;

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_travel_docs, null);
        dialog.setContentView(sheetView);

        activeDocsDialog = dialog;
        activeSheetView = sheetView;

        View btnClose = sheetView.findViewById(R.id.btnCloseSheet);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        View btnPassportReq = sheetView.findViewById(R.id.btnPassportReq);
        View btnMarriageReq = sheetView.findViewById(R.id.btnMarriageReq);
        View btnVisaReq = sheetView.findViewById(R.id.btnVisaReq);

        if (btnPassportReq != null) {
            btnPassportReq.setOnClickListener(v -> showDocInfoDialog(
                    getString(R.string.doc_passport_copy),
                    getString(R.string.doc_passport_validity_note)));
        }
        if (btnMarriageReq != null) {
            btnMarriageReq.setOnClickListener(v -> showDocInfoDialog(
                    getString(R.string.doc_passport_photo_title),
                    getString(R.string.doc_photo_spec_note)));
        }
        if (btnVisaReq != null) {
            btnVisaReq.setOnClickListener(v -> showDocInfoDialog(
                    getString(R.string.doc_travel_visa_title),
                    getString(R.string.doc_visa_company_note)));
        }

        loadTravellerDocuments(booking.id, null);
        dialog.show();
    }

    private void loadTravellerDocuments(int bookingId, Integer travellerId) {
        if (activeSheetView == null) return;

        ApiClient.getApiService().getBookingDocuments(bookingId, travellerId).enqueue(
                new Callback<BookingDocumentsResponse>() {
                    @Override
                    public void onResponse(Call<BookingDocumentsResponse> call, Response<BookingDocumentsResponse> response) {
                        if (isFinishing() || isDestroyed() || activeSheetView == null) return;

                        BookingDocumentsResponse body = response.body();
                        if (response.isSuccessful() && body != null) {
                            activeTravellerId = body.selectedTravellerId;
                            setupTravellersChips(body.travellers, body.selectedTravellerId);
                            renderDocumentsUi(body.data);
                        }
                    }

                    @Override
                    public void onFailure(Call<BookingDocumentsResponse> call, Throwable t) {
                        // Keep current UI
                    }
                });
    }

    private void setupTravellersChips(List<BookingDocumentsResponse.TravellerDto> travellers, Integer selectedId) {
        if (activeSheetView == null) return;

        View scrollContainer = activeSheetView.findViewById(R.id.scrollTravellerSelector);
        ChipGroup chipGroup = activeSheetView.findViewById(R.id.chipGroupTravellers);

        if (travellers == null || travellers.isEmpty() || travellers.size() <= 1) {
            if (scrollContainer != null) scrollContainer.setVisibility(View.GONE);
            return;
        }

        if (scrollContainer != null) scrollContainer.setVisibility(View.VISIBLE);
        if (chipGroup != null) {
            chipGroup.setOnCheckedChangeListener(null);
            chipGroup.removeAllViews();

            for (int i = 0; i < travellers.size(); i++) {
                BookingDocumentsResponse.TravellerDto t = travellers.get(i);
                Chip chip = new Chip(this);
                String label = (i + 1) + ". " + (t.name != null ? t.name : getString(R.string.passenger_lead_default_name));
                if (t.isPrimary) label += " " + getString(R.string.passenger_lead_tag);
                chip.setText(label);
                chip.setCheckable(true);
                chip.setClickable(true);
                chip.setChipBackgroundColorResource(R.color.white);
                chip.setChipStrokeColorResource(R.color.brand_magenta);
                chip.setChipStrokeWidth(2f);
                chip.setTextColor(getResources().getColor(R.color.brand_magenta));
                chip.setId(View.generateViewId());
                chip.setTag(t.id);

                if (selectedId != null && t.id == selectedId) {
                    chip.setChecked(true);
                } else if (selectedId == null && i == 0) {
                    chip.setChecked(true);
                }

                chipGroup.addView(chip);
            }

            chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
                Chip checkedChip = group.findViewById(checkedId);
                if (checkedChip != null && checkedChip.getTag() instanceof Integer) {
                    int newTravellerId = (Integer) checkedChip.getTag();
                    activeTravellerId = newTravellerId;
                    if (activeBooking != null) {
                        loadTravellerDocuments(activeBooking.id, newTravellerId);
                    }
                }
            });
        }
    }

    private void renderDocumentsUi(List<DocumentDto> docs) {
        if (activeSheetView == null) return;

        TextView tvPassportStatus = activeSheetView.findViewById(R.id.tvPassportStatus);
        TextView btnPassportAction = activeSheetView.findViewById(R.id.btnPassportAction);
        TextView tvPassportDates = activeSheetView.findViewById(R.id.tvPassportDates);
        TextView tvPassportGuidance = activeSheetView.findViewById(R.id.tvPassportGuidance);
        View passportRejectionContainer = activeSheetView.findViewById(R.id.passportRejectionContainer);
        TextView tvPassportRejectionReason = activeSheetView.findViewById(R.id.tvPassportRejectionReason);

        TextView tvMarriageStatus = activeSheetView.findViewById(R.id.tvMarriageStatus);
        TextView btnMarriageAction = activeSheetView.findViewById(R.id.btnMarriageAction);
        TextView tvMarriageDates = activeSheetView.findViewById(R.id.tvMarriageDates);
        TextView tvMarriageGuidance = activeSheetView.findViewById(R.id.tvMarriageGuidance);
        View marriageRejectionContainer = activeSheetView.findViewById(R.id.marriageRejectionContainer);
        TextView tvMarriageRejectionReason = activeSheetView.findViewById(R.id.tvMarriageRejectionReason);

        TextView tvVisaStatus = activeSheetView.findViewById(R.id.tvVisaStatus);
        TextView btnVisaAction = activeSheetView.findViewById(R.id.btnVisaAction);
        TextView tvVisaDates = activeSheetView.findViewById(R.id.tvVisaDates);
        TextView tvVisaGuidance = activeSheetView.findViewById(R.id.tvVisaGuidance);
        View visaRejectionContainer = activeSheetView.findViewById(R.id.visaRejectionContainer);
        TextView tvVisaRejectionReason = activeSheetView.findViewById(R.id.tvVisaRejectionReason);

        View cardDocProgressContainer = activeSheetView.findViewById(R.id.cardDocProgressContainer);
        TextView tvDocProgressPercent = activeSheetView.findViewById(R.id.tvDocProgressPercent);
        TextView tvDocProgressCount = activeSheetView.findViewById(R.id.tvDocProgressCount);
        ProgressBar pbDocVerification = activeSheetView.findViewById(R.id.pbDocVerification);
        TextView tvDocProgressMessage = activeSheetView.findViewById(R.id.tvDocProgressMessage);

        int verifiedCount = 0;
        int underReviewCount = 0;
        int rejectedCount = 0;
        final int requiredTotal = 2; // Passport + Photo

        if (docs != null) {
            for (DocumentDto doc : docs) {
                if (doc == null || doc.documentCode == null) continue;
                String code = doc.documentCode.toLowerCase();
                DocumentStatus st = DocumentStatus.from(doc.status);

                if ("passport".equals(code) || "passport_photo".equals(code)) {
                    if (st == DocumentStatus.VERIFIED) verifiedCount++;
                    else if (st == DocumentStatus.PENDING) underReviewCount++;
                    else if (st == DocumentStatus.REJECTED) rejectedCount++;
                }

                if ("passport".equals(code)) {
                    bindDocStatusUi(tvPassportStatus, btnPassportAction, tvPassportDates, tvPassportGuidance,
                            passportRejectionContainer, tvPassportRejectionReason, doc, "passport", getString(R.string.doc_passport_copy), R.drawable.ic_doc_passport);
                } else if ("passport_photo".equals(code)) {
                    bindDocStatusUi(tvMarriageStatus, btnMarriageAction, tvMarriageDates, tvMarriageGuidance,
                            marriageRejectionContainer, tvMarriageRejectionReason, doc, "passport_photo", getString(R.string.doc_passport_photo_title), R.drawable.ic_profile);
                } else if ("visa".equals(code) || "travel_visa".equals(code)) {
                    bindDocStatusUi(tvVisaStatus, btnVisaAction, tvVisaDates, tvVisaGuidance,
                            visaRejectionContainer, tvVisaRejectionReason, doc, "visa", getString(R.string.doc_travel_visa_title), R.drawable.ic_visa);
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
                tvDocProgressMessage.setTextColor(Color.parseColor("#DC2626"));
            } else if (verifiedCount == requiredTotal) {
                tvDocProgressMessage.setText(getString(R.string.msg_doc_progress_complete));
                tvDocProgressMessage.setTextColor(Color.parseColor("#047857"));
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

    private void bindDocStatusUi(TextView tvStatus, TextView btnAction, TextView tvDates, TextView tvGuidance,
                                 View rejectionContainer, TextView tvRejectionReason,
                                 DocumentDto doc, String docCode, String docTitle, int iconRes) {
        if (tvStatus == null || doc == null) return;
        DocumentStatus status = DocumentStatus.from(doc.status);

        if (rejectionContainer != null) rejectionContainer.setVisibility(View.GONE);

        if (status == DocumentStatus.PENDING) {
            tvStatus.setText(getString(R.string.doc_status_under_review));
            tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
            tvStatus.setTextColor(getResources().getColor(R.color.gold_accent));
            if (btnAction != null) {
                btnAction.setText(getString(R.string.doc_action_view));
                btnAction.setBackgroundResource(R.drawable.bg_button_white_square);
                btnAction.setTextColor(getResources().getColor(R.color.brand_magenta));
                btnAction.setOnClickListener(v -> Toast.makeText(this, getString(R.string.toast_doc_under_review), Toast.LENGTH_SHORT).show());
            }
            if (tvGuidance != null) {
                tvGuidance.setText(getString(R.string.doc_guidance_under_review));
                tvGuidance.setVisibility(View.VISIBLE);
            }
        } else if (status == DocumentStatus.VERIFIED) {
            tvStatus.setText(getString(R.string.doc_status_verified));
            tvStatus.setBackgroundResource(R.drawable.bg_status_verified);
            tvStatus.setTextColor(Color.parseColor("#047857"));
            if (btnAction != null) {
                btnAction.setText(getString(R.string.doc_action_view));
                btnAction.setBackgroundResource(R.drawable.bg_button_white_square);
                btnAction.setTextColor(getResources().getColor(R.color.brand_magenta));
                btnAction.setOnClickListener(v -> Toast.makeText(this, getString(R.string.toast_doc_verified), Toast.LENGTH_SHORT).show());
            }
            if (tvGuidance != null) {
                tvGuidance.setText(getString(R.string.doc_guidance_verified));
                tvGuidance.setVisibility(View.VISIBLE);
            }
        } else if (status == DocumentStatus.REJECTED) {
            tvStatus.setText(getString(R.string.doc_status_rejected));
            tvStatus.setBackgroundResource(R.drawable.bg_status_rejected);
            tvStatus.setTextColor(Color.parseColor("#DC2626"));
            if (rejectionContainer != null && doc.rejectionReason != null && !doc.rejectionReason.isEmpty()) {
                rejectionContainer.setVisibility(View.VISIBLE);
                if (tvRejectionReason != null) tvRejectionReason.setText(doc.rejectionReason);
            }
            if (btnAction != null) {
                btnAction.setText(getString(R.string.doc_action_replace));
                btnAction.setBackgroundResource(R.drawable.bg_button_pink);
                btnAction.setTextColor(getResources().getColor(R.color.white));
                btnAction.setOnClickListener(v -> showUploadDocumentDialog(docCode, docTitle, iconRes));
            }
        } else {
            tvStatus.setText(getString(R.string.doc_status_not_uploaded));
            tvStatus.setBackgroundResource(R.drawable.bg_status_not_uploaded);
            tvStatus.setTextColor(getResources().getColor(R.color.text_gray));
            if (btnAction != null) {
                btnAction.setText(getString(R.string.doc_action_upload));
                btnAction.setBackgroundResource(R.drawable.bg_button_pink);
                btnAction.setTextColor(getResources().getColor(R.color.white));
                btnAction.setOnClickListener(v -> showUploadDocumentDialog(docCode, docTitle, iconRes));
            }
        }
    }

    private void showUploadDocumentDialog(String docCode, String docTitle, int iconRes) {
        activeDocCode = docCode;
        activeDocTitle = docTitle;
        pendingUploadUri = null;

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
                if (pendingUploadUri == null) {
                    Toast.makeText(MyBookingsActivity.this, getString(R.string.err_file_empty), Toast.LENGTH_SHORT).show();
                    return;
                }
                dialog.dismiss();
                performUploadBookingDocument(activeDocCode, pendingUploadUri);
            });
        }

        dialog.show();
    }

    private void performUploadBookingDocument(String docCode, Uri uri) {
        if (activeBooking == null || uri == null) return;

        Toast.makeText(this, getString(R.string.doc_upload_processing), Toast.LENGTH_SHORT).show();

        try {
            String fileName = getFileName(uri);
            String mimeType = getContentResolver().getType(uri);
            if (mimeType == null) mimeType = "application/octet-stream";

            File tempFile = new File(getCacheDir(), "upload_" + System.currentTimeMillis() + "_" + fileName);
            try (InputStream in = getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }

            RequestBody codePart = RequestBody.create(MediaType.parse("text/plain"), docCode);
            RequestBody travellerPart = activeTravellerId != null
                    ? RequestBody.create(MediaType.parse("text/plain"), String.valueOf(activeTravellerId))
                    : null;
            RequestBody fileBody = RequestBody.create(MediaType.parse(mimeType), tempFile);
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", fileName, fileBody);

            ApiClient.getApiService().uploadBookingDocument(activeBooking.id, codePart, travellerPart, filePart)
                    .enqueue(new Callback<ApiResponse<DocumentDto>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<DocumentDto>> call, Response<ApiResponse<DocumentDto>> response) {
                            tempFile.delete();
                            if (isFinishing() || isDestroyed()) return;

                            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                Toast.makeText(MyBookingsActivity.this, getString(R.string.toast_doc_uploaded_success), Toast.LENGTH_SHORT).show();
                                loadTravellerDocuments(activeBooking.id, activeTravellerId);
                            } else {
                                Toast.makeText(MyBookingsActivity.this, getString(R.string.doc_upload_failed_retry), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<DocumentDto>> call, Throwable t) {
                            tempFile.delete();
                            if (isFinishing() || isDestroyed()) return;
                            Toast.makeText(MyBookingsActivity.this, getString(R.string.doc_upload_network_error), Toast.LENGTH_SHORT).show();
                        }
                    });

        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.doc_read_error), Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileName(Uri uri) {
        String fileName = "document_" + System.currentTimeMillis();
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) fileName = cursor.getString(nameIndex);
            }
        } catch (Exception ignored) {}
        return fileName;
    }

    private void showDocInfoDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.dialog_btn_understand), (d, w) -> d.dismiss())
                .show();
    }

    public void showCancelBookingConfirmation(BookingDto booking) {
        if (booking == null || booking.id <= 0) return;

        String bookingRef = booking.bookingNo != null ? booking.bookingNo : "BKG-" + booking.id;

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        container.setPadding(pad, dp(10), pad, dp(5));

        TextView tvMsg = new TextView(this);
        tvMsg.setText(getString(R.string.cancel_booking_confirm_message, bookingRef));
        tvMsg.setTextSize(14);
        tvMsg.setTextColor(getResources().getColor(R.color.text_dark));
        tvMsg.setLineSpacing(dp(2), 1.1f);
        container.addView(tvMsg);

        EditText etReason = new EditText(this);
        etReason.setHint(getString(R.string.cancel_booking_reason_hint));
        etReason.setTextSize(13);
        etReason.setPadding(dp(12), dp(10), dp(12), dp(10));
        etReason.setBackgroundResource(R.drawable.bg_input_box);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(14);
        etReason.setLayoutParams(lp);
        container.addView(etReason);

        new AlertDialog.Builder(this)
                .setTitle(R.string.cancel_booking_title)
                .setView(container)
                .setPositiveButton(R.string.cancel_booking_btn_confirm, (dialog, which) -> {
                    String reason = etReason.getText().toString().trim();
                    performCancelBooking(booking, reason);
                })
                .setNegativeButton(R.string.cancel_booking_keep, null)
                .show();
    }

    private void performCancelBooking(BookingDto booking, String reason) {
        String bookingRef = booking.bookingNo != null ? booking.bookingNo : "BKG-" + booking.id;
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setMessage(R.string.cancel_booking_processing)
                .setCancelable(false)
                .create();
        progressDialog.show();

        CancelBookingRequest req = new CancelBookingRequest(!reason.isEmpty() ? reason : null);
        ApiClient.getApiService().cancelBooking(booking.id, req)
                .enqueue(new Callback<ApiResponse<BookingDetailDto>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<BookingDetailDto>> call, Response<ApiResponse<BookingDetailDto>> response) {
                        if (isFinishing() || isDestroyed()) return;
                        progressDialog.dismiss();

                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Toast.makeText(MyBookingsActivity.this,
                                    getString(R.string.cancel_booking_success, bookingRef), Toast.LENGTH_LONG).show();
                            loadBookings();
                        } else {
                            String err = ApiErrors.userMessage(MyBookingsActivity.this, response, R.string.cancel_booking_failed);
                            Toast.makeText(MyBookingsActivity.this, err, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<BookingDetailDto>> call, Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        progressDialog.dismiss();
                        String err = ApiErrors.userMessage(MyBookingsActivity.this, t, R.string.cancel_booking_failed);
                        Toast.makeText(MyBookingsActivity.this, err, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
