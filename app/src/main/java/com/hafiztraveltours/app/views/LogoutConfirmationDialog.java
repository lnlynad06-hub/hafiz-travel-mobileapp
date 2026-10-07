package com.hafiztraveltours.app.views;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.utils.HapticUtil;

public class LogoutConfirmationDialog {

    public interface OnLogoutConfirmedListener {
        void onConfirmed();
    }

    public static void show(Context context, OnLogoutConfirmedListener onConfirm) {
        if (context == null) return;
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
        }

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_logout, null);

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
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            window.setDimAmount(0.55f);
        }

        AppCompatButton btnCancel = view.findViewById(R.id.btnCancelLogout);
        AppCompatButton btnConfirm = view.findViewById(R.id.btnConfirmLogout);

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                HapticUtil.tap(v);
                dialog.dismiss();
            });
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                HapticUtil.click(v);
                dialog.dismiss();
                if (onConfirm != null) {
                    onConfirm.onConfirmed();
                }
            });
        }

        dialog.show();
    }
}
