package com.hafiztraveltours.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.CustomTravelRequestDto;
import com.hafiztraveltours.app.utils.RoomLabels;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomTravelRequestAdapter extends RecyclerView.Adapter<CustomTravelRequestAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(CustomTravelRequestDto item);
    }

    private final Context context;
    private final List<CustomTravelRequestDto> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public CustomTravelRequestAdapter(Context context, OnItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<CustomTravelRequestDto> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_custom_travel_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CustomTravelRequestDto item = items.get(position);
        holder.bind(item, context, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvRequestNo;
        private final TextView tvStatusBadge;
        private final TextView tvTitle;
        private final TextView tvDates;
        private final TextView tvTravellersInfo;
        private final TextView tvQuotationHighlight;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRequestNo = itemView.findViewById(R.id.tvItemRequestNo);
            tvStatusBadge = itemView.findViewById(R.id.tvItemStatusBadge);
            tvTitle = itemView.findViewById(R.id.tvItemTitle);
            tvDates = itemView.findViewById(R.id.tvItemDates);
            tvTravellersInfo = itemView.findViewById(R.id.tvItemTravellersInfo);
            tvQuotationHighlight = itemView.findViewById(R.id.tvItemQuotationHighlight);
        }

        public void bind(CustomTravelRequestDto item, Context context, OnItemClickListener listener) {
            tvRequestNo.setText(item.requestNo != null ? item.requestNo : "CRQ-" + item.id);
            tvStatusBadge.setText(item.getStatusLocalized(context));
            tvTitle.setText(item.getDisplayTitle());

            // Dates & Duration
            String dep = item.preferredDepartureDate != null ? item.preferredDepartureDate : "-";
            String ret = item.preferredReturnDate != null ? item.preferredReturnDate : "-";
            int duration = item.durationDays != null ? item.durationDays : 0;
            if (duration > 0) {
                tvDates.setText(context.getString(R.string.request_dates_with_duration, dep, ret, duration));
            } else {
                tvDates.setText(context.getString(R.string.request_dates, dep, ret));
            }

            // Travellers & Rooms
            int pax = item.getTotalPax();
            String roomCat = item.roomCategory != null ? item.roomCategory.toUpperCase() : "QUAD";
            String roomLabel = RoomLabels.resolve(context, item.roomCategory, roomCat);
            String familyText = context.getString(item.isFamilyRoom ? R.string.family_together : R.string.genders_separate);
            tvTravellersInfo.setText(context.getString(R.string.request_travellers_room, pax, roomLabel, familyText));

            // Official quotation highlight if customer visible
            if (item.hasCustomerVisibleQuotation()) {
                tvQuotationHighlight.setVisibility(View.VISIBLE);
                tvQuotationHighlight.setText(context.getString(R.string.official_quotation_amount,
                        String.format(Locale.US, "%,.2f", item.latestQuotation.totalAmount)));
            } else {
                tvQuotationHighlight.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });
        }
    }
}
