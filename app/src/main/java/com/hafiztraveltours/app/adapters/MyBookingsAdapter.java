package com.hafiztraveltours.app.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
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

    public interface OnCancelClickListener {
        void onCancelClick(BookingDto booking);
    }

    private List<BookingDto> items;
    private OnDocumentClickListener documentClickListener;
    private OnPayClickListener payClickListener;
    private OnCancelClickListener cancelClickListener;

    public MyBookingsAdapter(List<BookingDto> items) {
        this.items = items != null ? items : new java.util.ArrayList<>();
    }

    public void setOnDocumentClickListener(OnDocumentClickListener listener) {
        this.documentClickListener = listener;
    }

    public void setOnPayClickListener(OnPayClickListener listener) {
        this.payClickListener = listener;
    }

    public void setOnCancelClickListener(OnCancelClickListener listener) {
        this.cancelClickListener = listener;
    }

    public void setItems(List<BookingDto> newItems) {
        this.items = newItems != null ? newItems : new java.util.ArrayList<>();
        notifyDataSetChanged();
    }

    public BookingDto getItem(int position) {
        if (items == null || position < 0 || position >= items.size()) return null;
        return items.get(position);
    }

    public void removeBooking(BookingDto booking) {
        if (items != null && booking != null) {
            int index = items.indexOf(booking);
            if (index >= 0) {
                items.remove(index);
                notifyItemRemoved(index);
                notifyItemRangeChanged(index, items.size());
            } else {
                notifyDataSetChanged();
            }
        }
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

        // 1. Booking Status Badge (Header Row Left)
        String rawStatus = booking.status != null ? booking.status.toLowerCase(Locale.ROOT) : "";
        if (rawStatus.contains("cancel_req") || rawStatus.contains("cancellation_requested") || rawStatus.contains("mohon_batal")) {
            holder.txtStatus.setText(ctx.getString(R.string.status_cancellation_requested));
            holder.txtStatus.setTextColor(Color.parseColor("#D97706"));
            holder.txtStatus.setBackgroundResource(R.drawable.bg_pill_accent);
        } else if (rawStatus.contains("cancel") || rawStatus.contains("batal")
                || rawStatus.contains("delet") || rawStatus.contains("hapus")) {
            holder.txtStatus.setText(ctx.getString(R.string.status_cancelled));
            holder.txtStatus.setTextColor(Color.parseColor("#DC2626"));
            holder.txtStatus.setBackgroundResource(R.drawable.bg_pill_inactive);
        } else if (rawStatus.contains("confirm") || rawStatus.contains("active") || rawStatus.contains("sah")) {
            holder.txtStatus.setText(ctx.getString(R.string.status_confirmed));
            holder.txtStatus.setTextColor(Color.parseColor("#059669"));
            holder.txtStatus.setBackgroundResource(R.drawable.bg_pill_success);
        } else if (rawStatus.contains("complet") || rawStatus.contains("selesai")) {
            holder.txtStatus.setText(ctx.getString(R.string.status_completed));
            holder.txtStatus.setTextColor(Color.parseColor("#059669"));
            holder.txtStatus.setBackgroundResource(R.drawable.bg_pill_success);
        } else {
            holder.txtStatus.setText(ctx.getString(R.string.status_pending_confirmation));
            holder.txtStatus.setTextColor(Color.parseColor("#D97706"));
            holder.txtStatus.setBackgroundResource(R.drawable.bg_pill_accent);
        }

        // 2. Custom Luxury Kebab Popup Menu (Header Row Right)
        if (holder.btnKebabMenu != null) {
            holder.btnKebabMenu.setOnClickListener(v -> {
                HapticUtil.click(v);
                showCustomKebabMenu(ctx, v, booking);
            });
        }

        // 3. Package Title
        holder.txtPackageName.setText(booking.packageName != null ? booking.packageName : ctx.getString(R.string.booking_category_umrah));

        // 4. Trip Information (Travel date • Duration • Pax)
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

        // 5. Payment Amounts (3-column summary with single-line baseline alignment)
        double totalAmt = booking.totalAmount;
        double paidAmt = booking.paidAmount;
        double balAmt = booking.balanceAmount;

        if (holder.txtPaidAmount != null) {
            holder.txtPaidAmount.setText(BookingRequest.formatPrice(paidAmt));
        }
        if (holder.txtOutstandingAmount != null) {
            holder.txtOutstandingAmount.setText(BookingRequest.formatPrice(balAmt));
        }
        if (holder.txtGrandTotal != null) {
            holder.txtGrandTotal.setText(BookingRequest.formatPrice(totalAmt));
        }

        boolean isCancelled = rawStatus.contains("cancel") || rawStatus.contains("batal")
                || rawStatus.contains("delet") || rawStatus.contains("hapus")
                || (booking.paymentStatus != null && (booking.paymentStatus.toLowerCase(Locale.ROOT).contains("cancel")
                        || booking.paymentStatus.toLowerCase(Locale.ROOT).contains("delet")));

        // Progress bar
        if (holder.paymentProgressBar != null) {
            if (isCancelled) {
                holder.paymentProgressBar.setVisibility(View.GONE);
            } else {
                holder.paymentProgressBar.setVisibility(View.VISIBLE);
                int progress = totalAmt > 0 ? (int) Math.round((paidAmt / totalAmt) * 100) : 0;
                holder.paymentProgressBar.setProgress(Math.max(0, Math.min(100, progress)));
            }
        }

        // Online payment is intentionally not released in this build.
        if (holder.btnPayNow != null) {
            holder.btnPayNow.setVisibility(View.GONE);
            holder.btnPayNow.setOnClickListener(null);
        }

        // 8. Action 2: Manage Documents (Secondary full width)
        if (holder.btnManageDocs != null) {
            if (isCancelled || "completed".equalsIgnoreCase(rawStatus)) {
                holder.btnManageDocs.setVisibility(View.GONE);
            } else {
                holder.btnManageDocs.setVisibility(View.VISIBLE);
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
        }
    }

    private void showCustomKebabMenu(Context ctx, View anchorView, BookingDto booking) {
        boolean hasConfirmedPayment = (booking.paidAmount > 0)
                || booking.isDepositPaid
                || booking.hasReceipts
                || (booking.receiptsCount > 0);

        String rawStatus = booking.status != null ? booking.status.toLowerCase(Locale.ROOT) : "";
        boolean isCancelled = rawStatus.contains("cancel") || rawStatus.contains("batal")
                || rawStatus.contains("delet") || rawStatus.contains("hapus")
                || (booking.paymentStatus != null && (booking.paymentStatus.toLowerCase(Locale.ROOT).contains("cancel")
                        || booking.paymentStatus.toLowerCase(Locale.ROOT).contains("delet")));
        boolean isCancellable = (booking.isCancellable != null ? booking.isCancellable.booleanValue() : true)
                && !isCancelled && !"completed".equalsIgnoreCase(rawStatus);

        try {
            View popupView = LayoutInflater.from(ctx).inflate(R.layout.popup_kebab_menu, null);
            PopupWindow popupWindow = new PopupWindow(
                    popupView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
            );

            popupWindow.setElevation(12f);
            popupWindow.setOutsideTouchable(true);

            View btnInvoice = popupView.findViewById(R.id.menuItemInvoice);
            View btnReceipt = popupView.findViewById(R.id.menuItemReceipt);
            View menuDivider = popupView.findViewById(R.id.menuDivider);
            View btnCancel = popupView.findViewById(R.id.menuItemCancel);
            View menuDividerCancel = popupView.findViewById(R.id.menuDividerCancel);

            if (btnInvoice != null) {
                btnInvoice.setOnClickListener(vItem -> {
                    HapticUtil.click(vItem);
                    popupWindow.dismiss();
                    Intent intent = new Intent(ctx, InvoiceViewerActivity.class);
                    intent.putExtra(InvoiceViewerActivity.EXTRA_BOOKING_ID, booking.id);
                    intent.putExtra(InvoiceViewerActivity.EXTRA_BOOKING_NO, booking.bookingNo);
                    ctx.startActivity(intent);
                });
            }

            if (hasConfirmedPayment) {
                if (btnReceipt != null) {
                    btnReceipt.setVisibility(View.VISIBLE);
                    btnReceipt.setOnClickListener(vItem -> {
                        HapticUtil.click(vItem);
                        popupWindow.dismiss();
                        Intent intent = new Intent(ctx, ReceiptViewerActivity.class);
                        intent.putExtra(ReceiptViewerActivity.EXTRA_BOOKING_ID, booking.id);
                        intent.putExtra(ReceiptViewerActivity.EXTRA_BOOKING_NO, booking.bookingNo);
                        ctx.startActivity(intent);
                    });
                }
                if (menuDivider != null) menuDivider.setVisibility(View.VISIBLE);
            } else {
                if (btnReceipt != null) btnReceipt.setVisibility(View.GONE);
                if (menuDivider != null) menuDivider.setVisibility(View.GONE);
            }

            if (isCancellable) {
                if (btnCancel != null) {
                    btnCancel.setVisibility(View.VISIBLE);
                    btnCancel.setOnClickListener(vItem -> {
                        HapticUtil.click(vItem);
                        popupWindow.dismiss();
                        if (cancelClickListener != null) {
                            cancelClickListener.onCancelClick(booking);
                        } else {
                            android.app.Activity act = getActivityFromContext(ctx);
                            if (act instanceof MyBookingsActivity) {
                                ((MyBookingsActivity) act).showCancelBookingConfirmation(booking);
                            }
                        }
                    });
                }
                if (menuDividerCancel != null) menuDividerCancel.setVisibility(View.VISIBLE);
            } else {
                if (btnCancel != null) btnCancel.setVisibility(View.GONE);
                if (menuDividerCancel != null) menuDividerCancel.setVisibility(View.GONE);
            }

            int xOffset = -Math.round(110 * ctx.getResources().getDisplayMetrics().density);
            popupWindow.showAsDropDown(anchorView, xOffset, 0);
        } catch (Exception e) {
            // Fallback to PopupMenu if custom PopupWindow fails
            androidx.appcompat.widget.PopupMenu fallbackMenu = new androidx.appcompat.widget.PopupMenu(ctx, anchorView);
            fallbackMenu.getMenu().add(0, 1, 0, ctx.getString(R.string.menu_invoice));
            if (hasConfirmedPayment) {
                fallbackMenu.getMenu().add(0, 2, 1, ctx.getString(R.string.menu_receipt));
            }
            if (isCancellable) {
                fallbackMenu.getMenu().add(0, 3, 2, ctx.getString(R.string.booking_btn_cancel));
            }
            fallbackMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    Intent intent = new Intent(ctx, InvoiceViewerActivity.class);
                    intent.putExtra(InvoiceViewerActivity.EXTRA_BOOKING_ID, booking.id);
                    intent.putExtra(InvoiceViewerActivity.EXTRA_BOOKING_NO, booking.bookingNo);
                    ctx.startActivity(intent);
                    return true;
                } else if (item.getItemId() == 2 && hasConfirmedPayment) {
                    Intent intent = new Intent(ctx, ReceiptViewerActivity.class);
                    intent.putExtra(ReceiptViewerActivity.EXTRA_BOOKING_ID, booking.id);
                    intent.putExtra(ReceiptViewerActivity.EXTRA_BOOKING_NO, booking.bookingNo);
                    ctx.startActivity(intent);
                    return true;
                } else if (item.getItemId() == 3 && isCancellable) {
                    if (cancelClickListener != null) {
                        cancelClickListener.onCancelClick(booking);
                    } else {
                        android.app.Activity act = getActivityFromContext(ctx);
                        if (act instanceof MyBookingsActivity) {
                            ((MyBookingsActivity) act).showCancelBookingConfirmation(booking);
                        }
                    }
                    return true;
                }
                return false;
            });
            fallbackMenu.show();
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtStatus;
        ImageView btnKebabMenu;

        TextView txtPackageName;
        TextView txtTripMeta;

        ProgressBar paymentProgressBar;

        TextView txtPaidAmount;
        TextView txtOutstandingAmount;
        TextView txtGrandTotal;

        MaterialButton btnPayNow;
        MaterialButton btnManageDocs;

        ViewHolder(View itemView) {
            super(itemView);
            txtStatus = itemView.findViewById(R.id.itemBookingStatus);
            btnKebabMenu = itemView.findViewById(R.id.btnBookingKebabMenu);

            txtPackageName = itemView.findViewById(R.id.itemBookingPackageName);
            txtTripMeta = itemView.findViewById(R.id.itemBookingTripMeta);

            paymentProgressBar = itemView.findViewById(R.id.itemPaymentProgressBar);

            txtPaidAmount = itemView.findViewById(R.id.itemBookingPaidAmount);
            txtOutstandingAmount = itemView.findViewById(R.id.itemBookingOutstandingAmount);
            txtGrandTotal = itemView.findViewById(R.id.itemBookingGrandTotal);

            btnPayNow = itemView.findViewById(R.id.btnItemPayNow);
            btnManageDocs = itemView.findViewById(R.id.btnItemManageDocs);
        }
    }
}
