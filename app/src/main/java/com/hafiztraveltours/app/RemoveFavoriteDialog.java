package com.hafiztraveltours.app;

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
            String cleanPrice = (pkg.price != null) ? pkg.price.replace("RM", "").replace("rm", "").trim() : "";
            tvPriceDuration.setText(context.getString(
                    R.string.package_duration_price, pkg.durationDays, pkg.nightsCount, cleanPrice));

            Glide.with(context)
                    .load(pkg.imageUrl)
                    .placeholder(R.drawable.bg_image_placeholder)
                    .centerCrop()
                    .into(ivImage);
        }

        btnCancel.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
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
