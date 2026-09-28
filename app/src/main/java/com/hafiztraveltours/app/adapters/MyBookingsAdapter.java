package com.hafiztraveltours.app.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.BookingDto;
import com.hafiztraveltours.app.models.BookingRequest;
import com.hafiztraveltours.app.ui.InvoiceViewerActivity;
import com.hafiztraveltours.app.ui.MyBookingsActivity;
import com.hafiztraveltours.app.ui.ReceiptViewerActivity;
import com.hafiztraveltours.app.utils.HapticUtil;

import java.util.List;
import java.util.Locale;

public class MyBookingsAdapter extends RecyclerView.Adapter<MyBookingsAdapter.ViewHolder> {

    public interface OnDocumentClickListener {
        void onDocumentClick(BookingDto booking);
    }

    public interface OnPayClickListener {
        void onPayClick(BookingDto booking);
    }

    private List<BookingDto> items;
    private OnDocumentClickListener documentClickListener;
    private OnPayClickListener payClickListener;

    public MyBookingsAdapter(List<BookingDto> items) {
        this.items = items != null ? items : new java.util.ArrayList<>();
    }

    public void setOnDocumentClickListener(OnDocumentClickListener listener) {
        this.documentClickListener = listener;
    }

    public void setOnPayClickListener(OnPayClickListener listener) {
        this.payClickListener = listener;
    }

    public void setItems(List<BookingDto> newItems) {
        this.items = newItems != null ? newItems : new java.util.ArrayList<>();
        notifyDataSetChanged();
    }

    public static android.app.Activity getActivityFromContext(Context context) {
        while (context instanceof android.content.ContextWrapper) {
            if (context instanceof android.app.Activity) {
                return (android.app.Activity) context;
            }
            context = ((android.content.ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (items == null || position < 0 || position >= items.size()) return;
        BookingDto booking = items.get(position);
        if (booking == null) return;

        Context ctx = holder.itemView.getContext();

        // 1. Category Tag
        if (holder.tvCategoryTag != null) {
            String pkgName = booking.packageName != null ? booking.packageName.toLowerCase(Locale.ROOT) : "";
            if (pkgName.contains("tour") || pkgName.contains("pelancongan") || pkgName.contains("vietnam")
                    || pkgName.contains("balkan") || pkgName.contains("turki") || pkgName.contains("turkey")
                    || pkgName.contains("japan") || pkgName.contains("korea") || pkgName.contains("switzerland")) {
                holder.tvCategoryTag.setText(ctx.getString(R.string.booking_category_tour));
            } else {
                holder.tvCategoryTag.setText(ctx.getString(R.string.booking_category_umrah));
            }
        }

        // 2. Booking Reference No & Copy
        holder.txtBookingNo.setText(booking.bookingNo != null ? booking.bookingNo : "BKG-" + booking.id);
        if (holder.btnCopyBookingNo != null) {
            holder.btnCopyBookingNo.setOnClickListener(v -> {
                HapticUtil.click(v);
                ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null && booking.bookingNo != null) {
                    ClipData clip = ClipData.newPlainText("Booking No", booking.bookingNo);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(ctx, ctx.getString(R.string.booking_success_copied_toast, booking.bookingNo), Toast.LENGTH_SHORT).show();
                }
            });
        }

        // 3. Booking Status Badge
        String rawStatus = booking.status != null ? booking.status.toLowerCase(Locale.ROOT) : "";
        if (rawStatus.contains("confirm") || rawStatus.contains("active") || rawStatus.contains("sah")) {
            holder.txtStatus.setText(ctx.getString(R.string.status_confirmed));
            holder.txtStatus.setTextColor(Color.parseColor("#059669"));
            holder.txtStatus.setBackgroundResource(R.drawable.bg_pill_success);
        } else if (rawStatus.contains("cancel") || rawStatus.contains("batal")) {
            holder.txtStatus.setText(ctx.getString(R.string.status_cancelled));
            holder.txtStatus.setTextColor(Color.parseColor("#DC2626"));
            holder.txtStatus.setBackgroundResource(R.drawable.bg_pill_inactive);
        } else {
            holder.txtStatus.setText(ctx.getString(R.string.status_pending_confirmation));
            holder.txtStatus.setTextColor(Color.parseColor("#D97706"));
            holder.txtStatus.setBackgroundResource(R.drawable.bg_pill_accent);
        }

        // 4. Package Hero Name
        holder.txtPackageName.setText(booking.packageName != null ? booking.packageName : ctx.getString(R.string.booking_category_umrah));

        // 5. Trip Meta: Departure Date, Duration & Pax
        if (holder.txtTripMeta != null) {
            StringBuilder meta = new StringBuilder();
            if (booking.departureDate != null && !booking.departureDate.isEmpty()) {
                meta.append(booking.departureDate);
            } else {
                meta.append("—");
            }

            if (booking.durationDays > 0) {
                int nights = Math.max(0, booking.durationDays - 1);
                meta.append(" • ").append(ctx.getString(R.string.booking_meta_duration_format, booking.durationDays, nights));
            }

            int pax = booking.totalPax > 0 ? booking.totalPax : 1;
            meta.append(" • ").append(ctx.getString(R.string.booking_meta_pax_format, pax));

            holder.txtTripMeta.setText(meta.toString());
        }

        // 6. Two-Stage Payment Calculation & Presentation
        double totalAmt = booking.totalAmount;
        double paidAmt = booking.paidAmount;
        double balAmt = booking.balanceAmount;

        double reqDeposit = booking.requiredDeposit > 0 ? booking.requiredDeposit : totalAmt;
        double depPaid = booking.depositPaid > 0 ? booking.depositPaid : Math.min(paidAmt, reqDeposit);
        double depRem = booking.depositRemaining > 0 ? booking.depositRemaining : Math.max(0, reqDeposit - depPaid);

        double pkgBalTotal = booking.balanceTotal > 0 ? booking.balanceTotal : Math.max(0, totalAmt - reqDeposit);
        double pkgBalPaid = booking.balancePaid > 0 ? booking.balancePaid : Math.max(0, paidAmt - reqDeposit);
        double pkgBalRem = booking.balanceRemaining > 0 ? booking.balanceRemaining : Math.max(0, pkgBalTotal - pkgBalPaid);

        if (balAmt <= 0 && totalAmt > 0) {
            // FULLY PAID
            if (holder.tvPaymentStageLabel != null) {
                holder.tvPaymentStageLabel.setText(ctx.getString(R.string.booking_label_stage_completed));
                holder.tvPaymentStageLabel.setTextColor(Color.parseColor("#059669"));
            }
            if (holder.tvPaymentStatusBadge != null) {
                holder.tvPaymentStatusBadge.setText(ctx.getString(R.string.payment_status_fully_paid));
                holder.tvPaymentStatusBadge.setTextColor(Color.parseColor("#059669"));
                holder.tvPaymentStatusBadge.setBackgroundResource(R.drawable.bg_pill_success);
            }
            if (holder.paymentProgressBar != null) {
                holder.paymentProgressBar.setProgress(100);
            }
            if (holder.tvPaymentStageDetail != null) {
                holder.tvPaymentStageDetail.setText(ctx.getString(R.string.payment_detail_fully_paid_format,
                        BookingRequest.formatPrice(totalAmt), BookingRequest.formatPrice(totalAmt)));
            }
        } else if (depRem > 0) {
            // STAGE 1: DEPOSIT PENDING / PARTIAL
            if (holder.tvPaymentStageLabel != null) {
                holder.tvPaymentStageLabel.setText(ctx.getString(R.string.booking_label_stage_deposit));
                holder.tvPaymentStageLabel.setTextColor(ctx.getResources().getColor(R.color.brand_magenta));
            }
            if (holder.tvPaymentStatusBadge != null) {
                if (depPaid > 0) {
                    holder.tvPaymentStatusBadge.setText(ctx.getString(R.string.payment_status_deposit_partial));
                    holder.tvPaymentStatusBadge.setTextColor(Color.parseColor("#D97706"));
                    holder.tvPaymentStatusBadge.setBackgroundResource(R.drawable.bg_pill_accent);
                } else {
                    holder.tvPaymentStatusBadge.setText(ctx.getString(R.string.payment_status_deposit_pending));
                    holder.tvPaymentStatusBadge.setTextColor(Color.parseColor("#DC2626"));
                    holder.tvPaymentStatusBadge.setBackgroundResource(R.drawable.bg_pill_inactive);
                }
            }
            if (holder.paymentProgressBar != null) {
                int progress = reqDeposit > 0 ? (int) Math.round((depPaid / reqDeposit) * 100) : 0;
                holder.paymentProgressBar.setProgress(Math.max(0, Math.min(100, progress)));
            }
            if (holder.tvPaymentStageDetail != null) {
                holder.tvPaymentStageDetail.setText(ctx.getString(R.string.payment_detail_deposit_format,
                        BookingRequest.formatPrice(depPaid), BookingRequest.formatPrice(reqDeposit), BookingRequest.formatPrice(depRem)));
            }
        } else {
            // STAGE 2: PACKAGE BALANCE INSTALLMENT
            if (holder.tvPaymentStageLabel != null) {
                holder.tvPaymentStageLabel.setText(ctx.getString(R.string.booking_label_stage_balance));
                holder.tvPaymentStageLabel.setTextColor(ctx.getResources().getColor(R.color.brand_magenta));
            }
            if (holder.tvPaymentStatusBadge != null) {
                holder.tvPaymentStatusBadge.setText(ctx.getString(R.string.payment_status_balance_partial));
                holder.tvPaymentStatusBadge.setTextColor(Color.parseColor("#2563EB"));
                holder.tvPaymentStatusBadge.setBackgroundResource(R.drawable.bg_pill_accent);
            }
            if (holder.paymentProgressBar != null) {
                int progress = totalAmt > 0 ? (int) Math.round((paidAmt / totalAmt) * 100) : 0;
                holder.paymentProgressBar.setProgress(Math.max(0, Math.min(100, progress)));
            }
            if (holder.tvPaymentStageDetail != null) {
                holder.tvPaymentStageDetail.setText(ctx.getString(R.string.payment_detail_balance_format,
                        BookingRequest.formatPrice(pkgBalPaid), BookingRequest.formatPrice(pkgBalTotal), BookingRequest.formatPrice(pkgBalRem)));
            }
        }

        // 7. Paid & Outstanding Amounts
        if (holder.txtPaidAmount != null) {
            holder.txtPaidAmount.setText(BookingRequest.formatPrice(paidAmt));
        }
        if (holder.txtOutstandingAmount != null) {
            holder.txtOutstandingAmount.setText(BookingRequest.formatPrice(balAmt));
        }

        // 8. Pay Now Action (State-Aware: only when outstanding balance exists and not cancelled)
        boolean isCancelled = rawStatus.contains("cancel") || rawStatus.contains("batal");
        if (holder.btnPayNow != null) {
            if (balAmt > 0 && !isCancelled) {
                holder.btnPayNow.setVisibility(View.VISIBLE);
                holder.btnPayNow.setText(R.string.booking_btn_pay_now);
                holder.btnPayNow.setOnClickListener(v -> {
                    HapticUtil.click(v);
                    if (payClickListener != null) {
                        payClickListener.onPayClick(booking);
                    } else {
                        android.app.Activity act = getActivityFromContext(v.getContext());
                        if (act instanceof MyBookingsActivity) {
                            ((MyBookingsActivity) act).showQuickPaySheet(booking);
                        }
                    }
                });
            } else {
                holder.btnPayNow.setVisibility(View.GONE);
            }
        }

        // 9. In-App Invoice Action
        if (holder.btnViewInvoice != null) {
            holder.btnViewInvoice.setOnClickListener(v -> {
                HapticUtil.click(v);
                Intent intent = new Intent(ctx, InvoiceViewerActivity.class);
                intent.putExtra(InvoiceViewerActivity.EXTRA_BOOKING_ID, booking.id);
                intent.putExtra(InvoiceViewerActivity.EXTRA_BOOKING_NO, booking.bookingNo);
                ctx.startActivity(intent);
            });
        }

        // 10. In-App Receipt Action (State-Aware: ONLY when verified receipts exist)
        if (holder.btnViewReceipt != null) {
            if (booking.hasReceipts || booking.receiptsCount > 0) {
                holder.btnViewReceipt.setVisibility(View.VISIBLE);
                holder.btnViewReceipt.setOnClickListener(v -> {
                    HapticUtil.click(v);
                    Intent intent = new Intent(ctx, ReceiptViewerActivity.class);
                    intent.putExtra(ReceiptViewerActivity.EXTRA_BOOKING_ID, booking.id);
                    intent.putExtra(ReceiptViewerActivity.EXTRA_BOOKING_NO, booking.bookingNo);
                    ctx.startActivity(intent);
                });
            } else {
                holder.btnViewReceipt.setVisibility(View.GONE);
            }
        }

        // 11. Manage Documents Action (Direct click only)
        if (holder.btnManageDocs != null) {
            holder.btnManageDocs.setEnabled(true);
            holder.btnManageDocs.setClickable(true);
            holder.btnManageDocs.setOnClickListener(v -> {
                HapticUtil.click(v);
                if (documentClickListener != null) {
                    documentClickListener.onDocumentClick(booking);
                } else {
                    android.app.Activity act = getActivityFromContext(v.getContext());
                    if (act instanceof MyBookingsActivity) {
                        ((MyBookingsActivity) act).showBookingDocsSheet(booking);
                    }
                }
            });
        }

        // Intentionally NO onClick on holder.itemView to prevent accidental document sheet opening!
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryTag;
        TextView txtBookingNo;
        ImageView btnCopyBookingNo;
        TextView txtStatus;

        TextView txtPackageName;
        TextView txtTripMeta;

        TextView tvPaymentStageLabel;
        TextView tvPaymentStatusBadge;
        ProgressBar paymentProgressBar;
        TextView tvPaymentStageDetail;

        TextView txtPaidAmount;
        TextView txtOutstandingAmount;

        MaterialButton btnPayNow;
        MaterialButton btnViewInvoice;
        MaterialButton btnViewReceipt;
        MaterialButton btnManageDocs;

        ViewHolder(View itemView) {
            super(itemView);
            tvCategoryTag = itemView.findViewById(R.id.tvItemCategoryTag);
            txtBookingNo = itemView.findViewById(R.id.itemBookingNo);
            btnCopyBookingNo = itemView.findViewById(R.id.btnItemCopyBookingNo);
            txtStatus = itemView.findViewById(R.id.itemBookingStatus);

            txtPackageName = itemView.findViewById(R.id.itemBookingPackageName);
            txtTripMeta = itemView.findViewById(R.id.itemBookingTripMeta);

            tvPaymentStageLabel = itemView.findViewById(R.id.itemPaymentStageLabel);
            tvPaymentStatusBadge = itemView.findViewById(R.id.itemPaymentStatusBadge);
            paymentProgressBar = itemView.findViewById(R.id.itemPaymentProgressBar);
            tvPaymentStageDetail = itemView.findViewById(R.id.itemPaymentStageDetail);

            txtPaidAmount = itemView.findViewById(R.id.itemBookingPaidAmount);
            txtOutstandingAmount = itemView.findViewById(R.id.itemBookingOutstandingAmount);

            btnPayNow = itemView.findViewById(R.id.btnItemPayNow);
            btnViewInvoice = itemView.findViewById(R.id.btnItemViewInvoice);
            btnViewReceipt = itemView.findViewById(R.id.btnItemViewReceipt);
            btnManageDocs = itemView.findViewById(R.id.btnItemManageDocs);
        }
    }
}
