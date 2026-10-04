package com.hafiztraveltours.app.utils;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hafiztraveltours.app.R;

/**
 * Universal utility for managing button loading states across the application.
 * Prevents text overlapping and stacked animations while preserving exact button dimensions,
 * shape, styling, and preventing duplicate taps during loading.
 */
public class LoadingButtonUtil {

    public static void showLoading(View buttonView, ProgressBar progressBar) {
        if (buttonView == null) return;
        buttonView.setEnabled(false);

        if (buttonView instanceof TextView) {
            TextView tv = (TextView) buttonView;
            if (tv.getTag(R.id.tag_original_text) == null) {
                tv.setTag(R.id.tag_original_text, tv.getText());
            }
            if (tv.getTag(R.id.tag_original_text_color) == null) {
                tv.setTag(R.id.tag_original_text_color, tv.getTextColors());
            }
            tv.setTextColor(Color.TRANSPARENT);
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.bringToFront();
        }
    }

    public static void hideLoading(View buttonView, ProgressBar progressBar) {
        if (buttonView == null) return;
        buttonView.setEnabled(true);

        if (buttonView instanceof TextView) {
            TextView tv = (TextView) buttonView;
            ColorStateList originalColor = (ColorStateList) tv.getTag(R.id.tag_original_text_color);
            if (originalColor != null) {
                tv.setTextColor(originalColor);
            } else {
                tv.setTextColor(Color.WHITE);
            }
            CharSequence originalText = (CharSequence) tv.getTag(R.id.tag_original_text);
            if (originalText != null) {
                tv.setText(originalText);
            }
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }
}
