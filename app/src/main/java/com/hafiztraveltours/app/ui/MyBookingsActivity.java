package com.hafiztraveltours.app.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.adapters.MyBookingsAdapter;
import com.hafiztraveltours.app.models.BookingDto;
import com.hafiztraveltours.app.models.BookingListPage;
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.utils.BottomNavHelper;
import com.hafiztraveltours.app.utils.LocaleHelper;
import com.hafiztraveltours.app.utils.SessionManager;

import java.util.ArrayList;

public class MyBookingsActivity extends BaseActivity {

    private MyBookingsAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyContainer;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private com.hafiztraveltours.app.utils.ViewStateController viewState;
    private retrofit2.Call<?> bookingsCall;

    
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
        recyclerView.setAdapter(adapter);
        viewState = new com.hafiztraveltours.app.utils.ViewStateController(
                progressBar, recyclerView, emptyContainer);

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

        // M8 single-flight: a new load cancels the previous identical request.
        if (bookingsCall != null) bookingsCall.cancel();
        retrofit2.Call<ApiResponse<BookingListPage>> call =
                ApiClient.getApiService().getBookings(null, 20);
        bookingsCall = call;
        call.enqueue(new retrofit2.Callback<ApiResponse<BookingListPage>>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse<BookingListPage>> call,
                                   retrofit2.Response<ApiResponse<BookingListPage>> response) {
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
            public void onFailure(retrofit2.Call<ApiResponse<BookingListPage>> call, Throwable t) {
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

    public void showBookingDocsSheet(BookingDto booking) {
        if (booking == null || booking.id <= 0) return;

        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_booking_docs, null);
        dialog.setContentView(sheetView);

        View btnClose = sheetView.findViewById(R.id.btnCloseTripDocs);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        TextView tvTitle = sheetView.findViewById(R.id.tvTripDocsTitle);
        if (tvTitle != null && booking.packageName != null) {
            tvTitle.setText(booking.packageName);
        }

        View progress = sheetView.findViewById(R.id.tripDocsProgress);
        android.widget.LinearLayout container = sheetView.findViewById(R.id.tripDocsContainer);

        if (progress != null) progress.setVisibility(View.VISIBLE);

        ApiClient.getApiService().getBookingDocuments(booking.id).enqueue(
                new retrofit2.Callback<ApiResponse<java.util.List<com.hafiztraveltours.app.models.DocumentDto>>>() {
                    @Override
                    public void onResponse(
                            retrofit2.Call<ApiResponse<java.util.List<com.hafiztraveltours.app.models.DocumentDto>>> call,
                            retrofit2.Response<ApiResponse<java.util.List<com.hafiztraveltours.app.models.DocumentDto>>> response) {
                        if (isFinishing() || isDestroyed()) return;
                        if (progress != null) progress.setVisibility(View.GONE);

                        java.util.List<com.hafiztraveltours.app.models.DocumentDto> docs =
                                (response.isSuccessful() && response.body() != null && response.body().isSuccess())
                                        ? response.body().data : null;

                        if (docs != null && container != null) {
                            container.removeAllViews();
                            if (docs.isEmpty()) {
                                TextView emptyTv = new TextView(MyBookingsActivity.this);
                                emptyTv.setText(getString(R.string.bookings_docs_all_linked));
                                emptyTv.setTextColor(getResources().getColor(R.color.text_gray));
                                emptyTv.setPadding(0, 32, 0, 32);
                                emptyTv.setGravity(android.view.Gravity.CENTER);
                                container.addView(emptyTv);
                            } else {
                                int autoReusedCount = 0;
                                int verifiedCount = 0;
                                for (com.hafiztraveltours.app.models.DocumentDto d : docs) {
                                    if (d.isAutoReused) autoReusedCount++;
                                    if (com.hafiztraveltours.app.utils.DocumentStatus.from(d != null ? d.status : null)
                                            == com.hafiztraveltours.app.utils.DocumentStatus.VERIFIED) verifiedCount++;
                                }

                                TextView summaryHeader = new TextView(MyBookingsActivity.this);
                                summaryHeader.setText(getString(R.string.bookings_doc_readiness_format, verifiedCount, docs.size(), autoReusedCount));
                                summaryHeader.setTextColor(getResources().getColor(R.color.brand_magenta));
                                summaryHeader.setTextSize(12);
                                summaryHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                                summaryHeader.setPadding(0, 0, 0, 16);
                                container.addView(summaryHeader);

                                for (com.hafiztraveltours.app.models.DocumentDto doc : docs) {
                                    container.addView(renderBookingDocCard(doc));
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(
                            retrofit2.Call<ApiResponse<java.util.List<com.hafiztraveltours.app.models.DocumentDto>>> call,
                            Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        if (progress != null) progress.setVisibility(View.GONE);
                        Toast.makeText(MyBookingsActivity.this, com.hafiztraveltours.app.network.ApiErrors.userMessage(MyBookingsActivity.this, t, R.string.err_network), Toast.LENGTH_SHORT).show();
                    }
                });

        dialog.show();
    }

    private View renderBookingDocCard(com.hafiztraveltours.app.models.DocumentDto doc) {
        androidx.cardview.widget.CardView card = new androidx.cardview.widget.CardView(this);
        android.widget.LinearLayout.LayoutParams cardParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = Math.round(12 * getResources().getDisplayMetrics().density);
        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(getResources().getColor(R.color.card_surface));
        card.setRadius(Math.round(16 * getResources().getDisplayMetrics().density));
        card.setCardElevation(Math.round(1 * getResources().getDisplayMetrics().density));

        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        android.widget.LinearLayout header = new android.widget.LinearLayout(this);
        header.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);

        android.widget.ImageView icon = new android.widget.ImageView(this);
        int iconSize = Math.round(38 * getResources().getDisplayMetrics().density);
        android.widget.LinearLayout.LayoutParams iconParams = new android.widget.LinearLayout.LayoutParams(iconSize, iconSize);
        icon.setLayoutParams(iconParams);
        icon.setBackgroundResource(R.drawable.circle_bg_light);
        icon.setPadding(Math.round(9 * getResources().getDisplayMetrics().density), Math.round(9 * getResources().getDisplayMetrics().density), Math.round(9 * getResources().getDisplayMetrics().density), Math.round(9 * getResources().getDisplayMetrics().density));
        icon.setImageResource(R.drawable.ic_doc_passport);
        icon.setColorFilter(getResources().getColor(R.color.pink_dark));
        header.addView(icon);

        android.widget.LinearLayout info = new android.widget.LinearLayout(this);
        info.setOrientation(android.widget.LinearLayout.VERTICAL);
        android.widget.LinearLayout.LayoutParams infoParams = new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        infoParams.setMarginStart(Math.round(12 * getResources().getDisplayMetrics().density));
        info.setLayoutParams(infoParams);

        TextView title = new TextView(this);
        title.setText(doc.title != null ? doc.title : "");
        title.setTextColor(getResources().getColor(R.color.text_dark));
        title.setTextSize(14);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        info.addView(title);

        TextView sourceTag = new TextView(this);
        sourceTag.setTextSize(11);
        sourceTag.setTypeface(null, android.graphics.Typeface.BOLD);
        if (doc.isAutoReused) {
            sourceTag.setText("✓ " + getString(R.string.doc_auto_reused_tag));
            sourceTag.setTextColor(getResources().getColor(R.color.brand_magenta));
        } else {
            sourceTag.setText(getString(R.string.doc_trip_specific_tag));
            sourceTag.setTextColor(getResources().getColor(R.color.text_gray));
        }
        info.addView(sourceTag);

        if (doc.rejectionReason != null && !doc.rejectionReason.isEmpty()) {
            TextView reason = new TextView(this);
            reason.setText(getString(R.string.doc_rejection_reason_prefix, doc.rejectionReason));
            reason.setTextColor(android.graphics.Color.parseColor("#EF4444"));
            reason.setTextSize(11);
            reason.setTypeface(null, android.graphics.Typeface.BOLD);
            info.addView(reason);
        }

        header.addView(info);

        TextView statusBadge = new TextView(this);
        statusBadge.setPadding(Math.round(10 * getResources().getDisplayMetrics().density), Math.round(4 * getResources().getDisplayMetrics().density), Math.round(10 * getResources().getDisplayMetrics().density), Math.round(4 * getResources().getDisplayMetrics().density));
        statusBadge.setTextSize(11);
        statusBadge.setTypeface(null, android.graphics.Typeface.BOLD);

        com.hafiztraveltours.app.utils.DocumentStatus docStatus =
                com.hafiztraveltours.app.utils.DocumentStatus.from(doc != null ? doc.status : null);

        if (docStatus == com.hafiztraveltours.app.utils.DocumentStatus.VERIFIED) {
            statusBadge.setText(getString(R.string.doc_status_verified));
            statusBadge.setBackgroundResource(R.drawable.bg_status_verified);
            statusBadge.setTextColor(android.graphics.Color.parseColor("#047857"));
        } else if (docStatus == com.hafiztraveltours.app.utils.DocumentStatus.PENDING) {
            statusBadge.setText(getString(docStatus.labelRes()));
            statusBadge.setBackgroundResource(R.drawable.bg_status_pending);
            statusBadge.setTextColor(getResources().getColor(R.color.gold_accent));
        } else if (docStatus == com.hafiztraveltours.app.utils.DocumentStatus.REJECTED) {
            statusBadge.setText(getString(R.string.doc_status_rejected));
            statusBadge.setBackgroundResource(R.drawable.bg_status_rejected);
            statusBadge.setTextColor(android.graphics.Color.parseColor("#EF4444"));
        } else if (docStatus == com.hafiztraveltours.app.utils.DocumentStatus.EXPIRED) {
            statusBadge.setText(getString(R.string.doc_status_expired));
            statusBadge.setBackgroundResource(R.drawable.bg_status_rejected);
            statusBadge.setTextColor(android.graphics.Color.parseColor("#EF4444"));
        } else {
            statusBadge.setText(getString(R.string.doc_status_not_uploaded));
            statusBadge.setBackgroundResource(R.drawable.bg_status_not_uploaded);
            statusBadge.setTextColor(getResources().getColor(R.color.text_gray));
        }
        header.addView(statusBadge);

        root.addView(header);

        android.widget.LinearLayout actions = new android.widget.LinearLayout(this);
        actions.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        actions.setGravity(android.view.Gravity.END);
        android.widget.LinearLayout.LayoutParams actionsParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        actionsParams.topMargin = Math.round(12 * getResources().getDisplayMetrics().density);
        actions.setLayoutParams(actionsParams);

        TextView actionBtn = new TextView(this);
        actionBtn.setPadding(Math.round(14 * getResources().getDisplayMetrics().density), Math.round(7 * getResources().getDisplayMetrics().density), Math.round(14 * getResources().getDisplayMetrics().density), Math.round(7 * getResources().getDisplayMetrics().density));
        actionBtn.setTextSize(12);
        actionBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        actionBtn.setClickable(true);
        actionBtn.setFocusable(true);

        if (docStatus == com.hafiztraveltours.app.utils.DocumentStatus.VERIFIED
                || docStatus == com.hafiztraveltours.app.utils.DocumentStatus.PENDING) {
            actionBtn.setText(getString(R.string.doc_action_view));
            actionBtn.setBackgroundResource(R.drawable.bg_button_white_square);
            actionBtn.setTextColor(getResources().getColor(R.color.brand_magenta));
        } else if (docStatus == com.hafiztraveltours.app.utils.DocumentStatus.REJECTED) {
            actionBtn.setText(getString(R.string.doc_action_replace));
            actionBtn.setBackgroundResource(R.drawable.bg_button_pink);
            actionBtn.setTextColor(getResources().getColor(R.color.white));
        } else {
            actionBtn.setText(getString(R.string.doc_action_upload));
            actionBtn.setBackgroundResource(R.drawable.bg_button_pink);
            actionBtn.setTextColor(getResources().getColor(R.color.white));
        }

        actionBtn.setOnClickListener(v ->
                Toast.makeText(MyBookingsActivity.this, getString(R.string.doc_status_fallback_format, doc.title, getString(docStatus.labelRes())), Toast.LENGTH_SHORT).show());

        actions.addView(actionBtn);
        root.addView(actions);

        card.addView(root);
        return card;
    }
}
