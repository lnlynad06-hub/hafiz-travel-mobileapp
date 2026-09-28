package com.hafiztraveltours.app.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.BookingDto;
import com.hafiztraveltours.app.models.BookingRequest;
import com.hafiztraveltours.app.ui.InvoiceViewerActivity;
import com.hafiztraveltours.app.ui.MyBookingsActivity;
import com.hafiztraveltours.app.utils.HapticUtil;

import java.util.List;
import java.util.Locale;

public class MyBookingsAdapter extends RecyclerView.Adapter<MyBookingsAdapter.ViewHolder> {

    private List<BookingDto> items;

    public MyBookingsAdapter(List<BookingDto> items) {
        this.items = items != null ? items : new java.util.ArrayList<>();
    }

    public void setItems(List<BookingDto> newItems) {
        this.items = newItems != null ? newItems : new java.util.ArrayList<>();
        notifyDataSetChanged();
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

        holder.txtPackageName.setText(booking.packageName != null ? booking.packageName : "Pakej Umrah / Pelancongan");
        holder.txtBookingNo.setText(booking.bookingNo != null ? booking.bookingNo : "BKG-" + booking.id);

        if (holder.btnCopyBookingNo != null) {
            holder.btnCopyBookingNo.setOnClickListener(v -> {
                HapticUtil.click(v);
                Context ctx = v.getContext();
                ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null && booking.bookingNo != null) {
                    ClipData clip = ClipData.newPlainText("Booking No", booking.bookingNo);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(ctx, ctx.getString(R.string.booking_success_copied_toast, booking.bookingNo), Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Status Badge
        String rawStatus = booking.status != null ? booking.status.toLowerCase(Locale.ROOT) : "";
        Context ctx = holder.itemView.getContext();
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

        // Departure Date
        if (holder.txtDepartureDate != null) {
            String dep = (booking.departureDate != null && !booking.departureDate.isEmpty())
                    ? booking.departureDate : "—";
            holder.txtDepartureDate.setText(dep);
        }

        // Total
        holder.txtTotal.setText(BookingRequest.formatPrice(booking.totalAmount));

        // Action: View In-App Invoice
        if (holder.btnViewInvoice != null) {
            holder.btnViewInvoice.setOnClickListener(v -> {
                HapticUtil.click(v);
                Intent intent = new Intent(ctx, InvoiceViewerActivity.class);
                intent.putExtra(InvoiceViewerActivity.EXTRA_BOOKING_ID, booking.id);
                intent.putExtra(InvoiceViewerActivity.EXTRA_BOOKING_NO, booking.bookingNo);
                ctx.startActivity(intent);
            });
        }

        // Action: View In-App Receipt
        if (holder.btnViewReceipt != null) {
            holder.btnViewReceipt.setOnClickListener(v -> {
                HapticUtil.click(v);
                Intent intent = new Intent(ctx, com.hafiztraveltours.app.ui.ReceiptViewerActivity.class);
                intent.putExtra(com.hafiztraveltours.app.ui.ReceiptViewerActivity.EXTRA_BOOKING_ID, booking.id);
                intent.putExtra(com.hafiztraveltours.app.ui.ReceiptViewerActivity.EXTRA_BOOKING_NO, booking.bookingNo);
                ctx.startActivity(intent);
            });
        }

        // Action: Manage Documents
        if (holder.btnManageDocs != null) {
            holder.btnManageDocs.setOnClickListener(v -> {
                HapticUtil.click(v);
                if (v.getContext() instanceof MyBookingsActivity) {
                    ((MyBookingsActivity) v.getContext()).showBookingDocsSheet(booking);
                }
            });
        }

        holder.itemView.setOnClickListener(v -> {
            if (v.getContext() instanceof MyBookingsActivity) {
                ((MyBookingsActivity) v.getContext()).showBookingDocsSheet(booking);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtPackageName;
        TextView txtBookingNo;
        ImageView btnCopyBookingNo;
        TextView txtStatus;
        TextView txtDepartureDate;
        TextView txtTotal;
        Button btnViewInvoice;
        Button btnViewReceipt;
        Button btnManageDocs;

        ViewHolder(View itemView) {
            super(itemView);
            txtPackageName = itemView.findViewById(R.id.itemBookingPackageName);
            txtBookingNo = itemView.findViewById(R.id.itemBookingNo);
            btnCopyBookingNo = itemView.findViewById(R.id.btnItemCopyBookingNo);
            txtStatus = itemView.findViewById(R.id.itemBookingStatus);
            txtDepartureDate = itemView.findViewById(R.id.itemBookingDepartureDate);
            txtTotal = itemView.findViewById(R.id.itemBookingTotal);
            btnViewInvoice = itemView.findViewById(R.id.btnItemViewInvoice);
            btnViewReceipt = itemView.findViewById(R.id.btnItemViewReceipt);
            btnManageDocs = itemView.findViewById(R.id.btnItemManageDocs);
        }
    }
}
