package com.hafiztraveltours.app.views;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.adapters.*;
import com.hafiztraveltours.app.network.*;
import com.hafiztraveltours.app.services.*;
import com.hafiztraveltours.app.utils.*;
import com.hafiztraveltours.app.views.*;
import com.hafiztraveltours.app.ui.*;


import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;

import com.bumptech.glide.Glide;

public class RemoveFavoriteDialog {

    public interface OnRemoveConfirmedListener {
        void onConfirmed();
    }

    public static void show(Context context, UmrahPackage pkg, OnRemoveConfirmedListener onConfirm) {
        if (context == null) return;
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
        }

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_remove_favorite, null);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(true)
                .create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setWindowAnimations(R.style.LuxuryDialogAnimation);
            int dialogWidth = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.88);
            window.setLayout(
                    Math.min(dialogWidth, (int) (380 * context.getResources().getDisplayMetrics().density)),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
            window.setDimAmount(0.55f);
        }

        // Bind Views
        ImageView ivImage = view.findViewById(R.id.dialogPackageImage);
        TextView tvName = view.findViewById(R.id.dialogPackageName);
        TextView tvPriceDuration = view.findViewById(R.id.dialogPackagePriceDuration);
        AppCompatButton btnCancel = view.findViewById(R.id.btnCancelRemove);
        AppCompatButton btnConfirm = view.findViewById(R.id.btnConfirmRemove);

        if (pkg != null) {
            tvName.setText(pkg.getDisplayName());
            String cleanPrice = com.hafiztraveltours.app.utils.MoneyFormat.numericString(pkg.price);
            tvPriceDuration.setText(context.getString(
                    R.string.package_duration_price, pkg.durationDays, pkg.nightsCount, cleanPrice));

            if (context instanceof android.app.Activity) {
                android.app.Activity act = (android.app.Activity) context;
                if (act.isFinishing() || act.isDestroyed()) return;
            }

            try {
                Glide.with(view)
                        .load(pkg.imageUrl)
                        .placeholder(R.drawable.bg_image_placeholder)
                        .centerCrop()
                        .into(ivImage);
            } catch (Exception ignored) {}
        }

        btnCancel.setOnClickListener(v -> {
            com.hafiztraveltours.app.utils.HapticUtil.tap(v);
            dialog.dismiss();
        });

        btnConfirm.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            dialog.dismiss();
            if (onConfirm != null) {
                onConfirm.onConfirmed();
            }
        });

        dialog.show();
    }
}
