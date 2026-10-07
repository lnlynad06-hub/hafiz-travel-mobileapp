package com.hafiztraveltours.app.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.AppNotification;
import com.hafiztraveltours.app.utils.AppNotificationManager;
import com.hafiztraveltours.app.utils.LocaleHelper;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(AppNotification notif);
    }

    private final Context context;
    private final List<AppNotification> list;
    private final OnNotificationClickListener listener;

    public NotificationAdapter(Context context, List<AppNotification> list, OnNotificationClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppNotification item = list.get(position);
        if (item == null) return;

        boolean isMalay = LocaleHelper.LANGUAGE_MALAY.equalsIgnoreCase(LocaleHelper.getSavedLanguage(context));
        String title = isMalay ? item.titleBm : item.titleEn;
        String message = isMalay ? item.messageBm : item.messageEn;

        holder.tvTitle.setText(title != null && !title.isEmpty() ? title : "");
        holder.tvMessage.setText(message != null && !message.isEmpty() ? message : "");

        if (item.timestamp > 0) {
            CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                    item.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            holder.tvTime.setText(relativeTime);
        } else {
            holder.tvTime.setText("");
        }

        holder.unreadDot.setVisibility(item.isRead ? View.GONE : View.VISIBLE);
        holder.itemView.setAlpha(item.isRead ? 0.7f : 1.0f);

        holder.itemView.setOnClickListener(v -> {
            if (!item.isRead) {
                item.isRead = true;
                AppNotificationManager.markRead(context, item.id);
                notifyItemChanged(holder.getAdapterPosition());
            }
            if (listener != null) {
                listener.onNotificationClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View unreadDot;
        final TextView tvTitle;
        final TextView tvMessage;
        final TextView tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            unreadDot = itemView.findViewById(R.id.unreadDot);
            tvTitle = itemView.findViewById(R.id.tvNotifTitle);
            tvMessage = itemView.findViewById(R.id.tvNotifMessage);
            tvTime = itemView.findViewById(R.id.tvNotifTime);
        }
    }
}
