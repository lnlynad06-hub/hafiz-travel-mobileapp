package com.hafiztraveltours.app.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.hafiztraveltours.app.utils.MoneyFormat;
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
        ImageView btnClose = sheetView.findViewById(R.id.btnClosePaySheet);

        TextView tvDueAmount = sheetView.findViewById(R.id.tvPaymentDueAmount);
        TextView tvPurposeBadge = sheetView.findViewById(R.id.tvPaymentPurposeBadge);

        TextView tvTotalAmount = sheetView.findViewById(R.id.tvPayPackageTotal);
        TextView tvPaidAmount = sheetView.findViewById(R.id.tvPayPreviouslyPaid);
        TextView tvCurrentAmount = sheetView.findViewById(R.id.tvPayCurrentAmount);
        TextView tvBalanceAfter = sheetView.findViewById(R.id.tvPayBalanceAfter);

        MaterialButton btnSubmit = sheetView.findViewById(R.id.btnSubmitQuickPay);

        if (btnClose != null) btnClose.setOnClickListener(v -> payDialog.dismiss());

        String packageName = booking.packageName != null ? booking.packageName : getString(R.string.booking_category_umrah);
        String bookingRef = booking.bookingNo != null ? booking.bookingNo : "BKG-" + booking.id;

        if (tvPkgName != null) tvPkgName.setText(packageName);
        if (tvBookingRef != null) tvBookingRef.setText(getString(R.string.booking_ref_prefix) + " " + bookingRef);

        // Determine payment stage amount & purpose
        final double requiredDep = booking.getRequiredDepositAmount();
        final double remainingDep = booking.getDepositRemainingAmount();
        final double totalBal = booking.balanceAmount > 0 ? booking.balanceAmount : Math.max(0, booking.totalAmount - booking.paidAmount);

        final double[] selectedAmount = new double[1];
        final String[] selectedPurpose = new String[1];
        final String[] selectedType = new String[1];

        View layoutInstallmentOptions = sheetView.findViewById(R.id.layoutInstallmentOptions);
        Button btnPreset100 = sheetView.findViewById(R.id.btnPayPreset100);
        Button btnPreset200 = sheetView.findViewById(R.id.btnPayPreset200);
        Button btnPreset500 = sheetView.findViewById(R.id.btnPayPreset500);
        Button btnPresetFull = sheetView.findViewById(R.id.btnPayPresetFull);

        Runnable updateUI = () -> {
            double amt = selectedAmount[0];
            String formatted = MoneyFormat.formatRMCents(amt);
            if (tvDueAmount != null) tvDueAmount.setText(formatted);
            if (tvPurposeBadge != null) tvPurposeBadge.setText(selectedPurpose[0]);
            if (tvCurrentAmount != null) tvCurrentAmount.setText(formatted);
            double balAfter = Math.max(0, booking.totalAmount - (booking.paidAmount + amt));
            if (tvBalanceAfter != null) tvBalanceAfter.setText(MoneyFormat.formatRMCents(balAfter));
            if (btnSubmit != null) {
                btnSubmit.setText(getString(R.string.pay_sheet_btn_pay_format, formatted));
            }
        };

        if (remainingDep > 0) {
            if (layoutInstallmentOptions != null) layoutInstallmentOptions.setVisibility(View.GONE);
            selectedAmount[0] = remainingDep;
            selectedType[0] = "deposit";
            selectedPurpose[0] = (booking.paidAmount <= 0)
                    ? getString(R.string.pay_sheet_purpose_initial_deposit)
                    : getString(R.string.pay_sheet_purpose_remaining_deposit);
            updateUI.run();
        } else {
            if (layoutInstallmentOptions != null) layoutInstallmentOptions.setVisibility(View.VISIBLE);

            final Button[] buttons = new Button[]{btnPreset100, btnPreset200, btnPreset500, btnPresetFull};
            final double[] amounts = new double[]{100.0, 200.0, 500.0, totalBal};

            class PresetHandler {
                void select(int idx) {
                    double chosenAmt = amounts[idx];
                    if (chosenAmt >= totalBal) {
                        chosenAmt = totalBal;
                        selectedPurpose[0] = getString(R.string.pay_sheet_purpose_final_payment);
                        selectedType[0] = "balance";
                    } else {
                        selectedPurpose[0] = getString(R.string.pay_sheet_purpose_additional_payment);
                        selectedType[0] = "installment";
                    }
                    selectedAmount[0] = chosenAmt;
                    updateUI.run();

                    for (int b = 0; b < buttons.length; b++) {
                        if (buttons[b] == null) continue;
                        if (b == idx) {
                            buttons[b].setBackgroundColor(Color.parseColor("#9C0E63"));
                            buttons[b].setTextColor(Color.WHITE);
                        } else {
                            buttons[b].setBackgroundColor(Color.TRANSPARENT);
                            buttons[b].setTextColor(Color.parseColor("#4B5563"));
                        }
                    }
                }
            }
            final PresetHandler handler = new PresetHandler();

            for (int i = 0; i < buttons.length; i++) {
                final int buttonIdx = i;
                if (buttons[i] != null) {
                    if (i < 3 && amounts[i] > totalBal) {
                        buttons[i].setEnabled(false);
                        buttons[i].setAlpha(0.4f);
                    } else {
                        buttons[i].setEnabled(true);
                        buttons[i].setAlpha(1.0f);
                        buttons[i].setOnClickListener(v -> {
                            HapticUtil.click(v);
                            handler.select(buttonIdx);
                        });
                    }
                }
            }

            // Default preset selection: RM 100 if available, else full balance
            if (totalBal > 100) {
                handler.select(0);
            } else {
                handler.select(3);
            }
        }

        if (tvTotalAmount != null) tvTotalAmount.setText(MoneyFormat.formatRMCents(booking.totalAmount));
        if (tvPaidAmount != null) tvPaidAmount.setText(MoneyFormat.formatRMCents(booking.paidAmount));

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                HapticUtil.click(v);
                payDialog.dismiss();
                showPaymentConfirmationDialog(booking, selectedAmount[0], selectedPurpose[0], selectedType[0]);
            });
        }

        payDialog.show();
    }

    private void showPaymentConfirmationDialog(BookingDto booking, double paymentAmount, String paymentPurpose, String paymentType) {
        String bookingRef = booking.bookingNo != null ? booking.bookingNo : "BKG-" + booking.id;
        String packageName = booking.packageName != null ? booking.packageName : getString(R.string.booking_category_umrah);
        String formattedAmount = MoneyFormat.formatRMCents(paymentAmount);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirm_payment, null);
        AlertDialog confirmDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (confirmDialog.getWindow() != null) {
            confirmDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvAmount = dialogView.findViewById(R.id.tvConfirmPayAmount);
        TextView tvSubtitle = dialogView.findViewById(R.id.tvConfirmPaySubtitle);
        TextView tvPurpose = dialogView.findViewById(R.id.tvConfirmPayPurpose);
        View btnCancel = dialogView.findViewById(R.id.btnCancelPayConfirm);
        View btnSubmit = dialogView.findViewById(R.id.btnSubmitPayConfirm);

        if (tvAmount != null) tvAmount.setText(formattedAmount);
        if (tvSubtitle != null) tvSubtitle.setText(packageName + " • " + bookingRef);
        if (tvPurpose != null) tvPurpose.setText(paymentPurpose);

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                HapticUtil.click(v);
                confirmDialog.dismiss();
            });
        }

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                HapticUtil.click(v);
                confirmDialog.dismiss();
                executeMockPayment(booking, paymentAmount, paymentPurpose, paymentType);
            });
        }

        confirmDialog.show();
    }

    private void executeMockPayment(BookingDto booking, double paymentAmount, String paymentPurpose, String paymentType) {
        String formattedAmount = MoneyFormat.formatRMCents(paymentAmount);

        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setMessage(R.string.pay_sheet_processing)
                .setCancelable(false)
                .create();
        progressDialog.show();

        Map<String, Object> body = new HashMap<>();
        body.put("amount", paymentAmount);
        body.put("payment_method", "toyyibpay");
        body.put("payment_type", paymentType);

        ApiClient.getApiService().payBooking(booking.id, body).enqueue(new Callback<ApiResponse<BookingDetailDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<BookingDetailDto>> call, Response<ApiResponse<BookingDetailDto>> response) {
                if (isFinishing() || isDestroyed()) return;
                progressDialog.dismiss();

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    BookingDetailDto detail = response.body().data;
                    if (detail != null) {
                        booking.paidAmount = detail.paidAmount;
                        booking.balanceAmount = detail.balanceAmount;
                        booking.depositPaid = detail.depositPaid;
                        booking.depositRemaining = detail.depositRemaining;
                        booking.isDepositPaid = detail.isDepositPaid;
                        booking.isMerchandiseEligible = detail.isMerchandiseEligible;
                        booking.status = detail.status;
                        booking.paymentStatus = detail.paymentStatus;
                        booking.hasReceipts = true;
                        booking.receiptsCount = detail.receipts != null ? detail.receipts.size() : Math.max(1, booking.receiptsCount + 1);
                    } else {
                        booking.paidAmount += paymentAmount;
                        booking.balanceAmount = Math.max(0, booking.balanceAmount - paymentAmount);
                        booking.depositPaid += paymentAmount;
                        booking.depositRemaining = Math.max(0, booking.depositRemaining - paymentAmount);
                        if (booking.depositRemaining <= 0) {
                            booking.isDepositPaid = true;
                            booking.isMerchandiseEligible = true;
                        }
                    }
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }

                    // Show Payment Successful Dialog
                    showPaymentSuccessfulDialog(booking, paymentAmount);

                    // Sync latest state from backend
                    loadBookings();
                } else {
                    String err = ApiErrors.userMessage(MyBookingsActivity.this, response, R.string.err_network);
                    Toast.makeText(MyBookingsActivity.this, err, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<BookingDetailDto>> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                progressDialog.dismiss();
                String err = ApiErrors.userMessage(MyBookingsActivity.this, t, R.string.err_network);
                Toast.makeText(MyBookingsActivity.this, err, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showPaymentSuccessfulDialog(BookingDto booking, double paymentAmount) {
        String bookingRef = booking.bookingNo != null ? booking.bookingNo : "BKG-" + booking.id;
        String packageName = booking.packageName != null ? booking.packageName : getString(R.string.booking_category_umrah);
        String formattedAmount = MoneyFormat.formatRMCents(paymentAmount);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_payment_successful, null);
        AlertDialog successDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (successDialog.getWindow() != null) {
            successDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvAmount = dialogView.findViewById(R.id.tvSuccessAmount);
        TextView tvSubtitle = dialogView.findViewById(R.id.tvSuccessSubtitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvSuccessMessage);
        View btnViewReceipt = dialogView.findViewById(R.id.btnSuccessViewReceipt);
        View btnDone = dialogView.findViewById(R.id.btnSuccessDone);

        if (tvAmount != null) tvAmount.setText(formattedAmount);
        if (tvSubtitle != null) tvSubtitle.setText(packageName + " • " + bookingRef);
        if (tvMessage != null) tvMessage.setText(getString(R.string.pay_success_dialog_msg, formattedAmount));

        if (btnViewReceipt != null) {
            btnViewReceipt.setOnClickListener(v -> {
                HapticUtil.click(v);
                successDialog.dismiss();
                Intent recIntent = new Intent(MyBookingsActivity.this, ReceiptViewerActivity.class);
                recIntent.putExtra(ReceiptViewerActivity.EXTRA_BOOKING_ID, booking.id);
                recIntent.putExtra(ReceiptViewerActivity.EXTRA_BOOKING_NO, booking.bookingNo);
                startActivity(recIntent);
            });
        }

        if (btnDone != null) {
            btnDone.setOnClickListener(v -> {
                HapticUtil.click(v);
                successDialog.dismiss();
            });
        }

        successDialog.show();
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
        displayCancellationDialog(booking);
    }

    private void displayCancellationDialog(BookingDto booking) {
        String bookingRef = booking.bookingNo != null ? booking.bookingNo : "BKG-" + booking.id;
        String pkgName = booking.packageName != null ? booking.packageName : getString(R.string.booking_category_umrah);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_cancel_booking, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvSubtitle = dialogView.findViewById(R.id.tvCancelDialogSubtitle);
        EditText etReason = dialogView.findViewById(R.id.etCancelReasonInput);
        View btnKeep = dialogView.findViewById(R.id.btnCancelKeep);
        View btnConfirm = dialogView.findViewById(R.id.btnCancelConfirm);

        if (tvSubtitle != null) {
            tvSubtitle.setText(pkgName + " • " + bookingRef);
        }

        ApiClient.getApiService().getCancellationQuote(booking.id)
                .enqueue(new Callback<ApiResponse<com.hafiztraveltours.app.models.CancellationQuoteDto>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<com.hafiztraveltours.app.models.CancellationQuoteDto>> call,
                                           Response<ApiResponse<com.hafiztraveltours.app.models.CancellationQuoteDto>> response) {
                        if (isFinishing() || isDestroyed() || !dialog.isShowing()) return;

                        com.hafiztraveltours.app.models.CancellationQuoteDto quote =
                                (response.isSuccessful() && response.body() != null && response.body().isSuccess())
                                        ? response.body().data : null;

                        populateQuoteBreakdown(dialogView, quote);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<com.hafiztraveltours.app.models.CancellationQuoteDto>> call, Throwable t) {
                        // Keep cancellation dialog active without breakdown if quote fails
                    }
                });

        if (btnKeep != null) {
            btnKeep.setOnClickListener(v -> {
                HapticUtil.click(v);
                dialog.dismiss();
            });
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                HapticUtil.click(v);
                dialog.dismiss();
                String reason = etReason != null ? etReason.getText().toString().trim() : "";
                performCancelBooking(booking, reason);
            });
        }

        dialog.show();
    }

    private void populateQuoteBreakdown(View dialogView, com.hafiztraveltours.app.models.CancellationQuoteDto quote) {
        if (quote == null || dialogView == null) return;
        View layoutBreakdown = dialogView.findViewById(R.id.layoutQuoteBreakdown);
        TextView tvWorkingDays = dialogView.findViewById(R.id.tvQuoteWorkingDays);
        TextView tvCharge = dialogView.findViewById(R.id.tvQuoteCharge);
        TextView tvDepositNote = dialogView.findViewById(R.id.tvQuoteDepositNote);
        TextView tvRefund = dialogView.findViewById(R.id.tvQuoteRefund);
        TextView tvRefundPeriod = dialogView.findViewById(R.id.tvQuoteRefundPeriod);
        TextView tvNoRefund = dialogView.findViewById(R.id.tvQuoteNoRefund);

        if (layoutBreakdown != null) {
            layoutBreakdown.setVisibility(View.VISIBLE);

            if (tvWorkingDays != null && quote.workingDaysToDeparture != null) {
                tvWorkingDays.setText(getString(R.string.cancel_quote_working_days, quote.workingDaysToDeparture));
                tvWorkingDays.setVisibility(View.VISIBLE);
            }

            if (tvCharge != null) {
                String pctStr = Math.round(quote.chargePercentage) + "%";
                tvCharge.setText(getString(R.string.cancel_quote_charge_summary, BookingRequest.formatPrice(quote.chargeAmount), pctStr));
                tvCharge.setVisibility(View.VISIBLE);
            }

            if (tvDepositNote != null) {
                tvDepositNote.setVisibility(View.VISIBLE);
            }

            if (quote.refundAmount > 0) {
                if (tvRefund != null) {
                    tvRefund.setText(getString(R.string.cancel_quote_estimated_refund, BookingRequest.formatPrice(quote.refundAmount)));
                    tvRefund.setVisibility(View.VISIBLE);
                }
                if (tvRefundPeriod != null) {
                    tvRefundPeriod.setVisibility(View.VISIBLE);
                }
                if (tvNoRefund != null) {
                    tvNoRefund.setVisibility(View.GONE);
                }
            } else if (quote.paidAmount > 0) {
                if (tvRefund != null) tvRefund.setVisibility(View.GONE);
                if (tvRefundPeriod != null) tvRefundPeriod.setVisibility(View.GONE);
                if (tvNoRefund != null) tvNoRefund.setVisibility(View.VISIBLE);
            } else {
                if (tvRefund != null) tvRefund.setVisibility(View.GONE);
                if (tvRefundPeriod != null) tvRefundPeriod.setVisibility(View.GONE);
                if (tvNoRefund != null) tvNoRefund.setVisibility(View.GONE);
            }
        }
    }

    private void performCancelBooking(BookingDto booking, String reason) {
        String bookingRef = booking.bookingNo != null ? booking.bookingNo : "BKG-" + booking.id;
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setMessage(R.string.cancel_booking_processing)
                .setCancelable(false)
                .create();
        progressDialog.show();

        CancelBookingRequest req = new CancelBookingRequest(!reason.isEmpty() ? reason : null);
        Log.d("MyBookingsActivity", "Cancel request initiated -> Method: POST, Path: /v1/bookings/" + booking.id + "/cancel, bookingId=" + booking.id);

        ApiClient.getApiService().cancelBooking(booking.id, req)
                .enqueue(new Callback<ApiResponse<BookingDetailDto>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<BookingDetailDto>> call, Response<ApiResponse<BookingDetailDto>> response) {
                        if (isFinishing() || isDestroyed()) return;
                        progressDialog.dismiss();

                        String contentType = response.headers() != null ? response.headers().get("Content-Type") : "unknown";
                        Log.d("MyBookingsActivity", "Cancel response received -> Code: " + response.code() + ", Content-Type: " + contentType);

                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Toast.makeText(MyBookingsActivity.this,
                                    getString(R.string.cancel_booking_success, bookingRef), Toast.LENGTH_LONG).show();
                            
                            // Optimistically update memory state and immediately refresh UI
                            booking.status = "cancelled";
                            booking.isCancellable = Boolean.FALSE;
                            if (adapter != null) {
                                adapter.notifyDataSetChanged();
                            }
                            loadBookings();
                        } else {
                            String err = ApiErrors.userMessage(MyBookingsActivity.this, response, R.string.cancel_booking_failed);
                            Log.w("MyBookingsActivity", "Cancel failed -> Parsed Error: " + com.hafiztraveltours.app.utils.LogSanitizer.sanitize(err));
                            Toast.makeText(MyBookingsActivity.this, err, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<BookingDetailDto>> call, Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        progressDialog.dismiss();
                        String err = ApiErrors.userMessage(MyBookingsActivity.this, t, R.string.cancel_booking_failed);
                        Log.e("MyBookingsActivity", "Cancel transport failure -> " + com.hafiztraveltours.app.utils.LogSanitizer.sanitize(t.getMessage()), t);
                        Toast.makeText(MyBookingsActivity.this, err, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
