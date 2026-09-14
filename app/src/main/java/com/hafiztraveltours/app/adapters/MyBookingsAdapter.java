package com.hafiztraveltours.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.BookingDto;
import com.hafiztraveltours.app.models.BookingRequest;

import java.util.List;

public class MyBookingsAdapter extends RecyclerView.Adapter<MyBookingsAdapter.ViewHolder> {

    private List<BookingDto> items;

    public MyBookingsAdapter(List<BookingDto> items) {
        this.items = items;
    }

    public void setItems(List<BookingDto> newItems) {
        this.items = newItems;
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
        BookingDto booking = items.get(position);
        holder.txtPackageName.setText(booking.packageName != null ? booking.packageName : "");
        holder.txtBookingNo.setText(booking.bookingNo != null ? booking.bookingNo : "");
        String status = booking.status != null ? booking.status.replace('_', ' ').toUpperCase() : "";
        String meta = status;
        if (booking.departureDate != null && !booking.departureDate.isEmpty()) {
            meta += " • " + booking.departureDate;
        }
        holder.txtStatus.setText(meta);
        holder.txtTotal.setText(BookingRequest.formatPrice(booking.totalAmount));

        holder.itemView.setOnClickListener(v -> {
            if (v.getContext() instanceof com.hafiztraveltours.app.ui.MyBookingsActivity) {
                ((com.hafiztraveltours.app.ui.MyBookingsActivity) v.getContext()).showBookingDocsSheet(booking);
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
        TextView txtStatus;
        TextView txtTotal;

        ViewHolder(View itemView) {
            super(itemView);
            txtPackageName = itemView.findViewById(R.id.itemBookingPackageName);
            txtBookingNo = itemView.findViewById(R.id.itemBookingNo);
            txtStatus = itemView.findViewById(R.id.itemBookingStatus);
            txtTotal = itemView.findViewById(R.id.itemBookingTotal);
        }
    }
}
