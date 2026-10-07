package com.hafiztraveltours.app.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.ui.ProfileActivity;

import java.util.Calendar;
import java.util.Locale;

public class DatePickerBottomSheetHelper {

    public interface OnDateSelectedListener {
        void onDateSelected(String isoDate, String formattedDisplayDate, int age);
    }

    public static BottomSheetDialog show(
            Context context,
            String title,
            String subtitle,
            boolean isExpiryPicker,
            boolean isChildPicker,
            TextView tvTargetValue,
            TextView tvTargetAge,
            String[] selectedDateHolder,
            OnDateSelectedListener listener) {

        BottomSheetDialog sheetDialog = new BottomSheetDialog(context);
        View sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_dob_picker, null);
        sheetDialog.setContentView(sheetView);

        if (sheetDialog.getWindow() != null) {
            sheetDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            sheetDialog.getWindow().setDimAmount(0.55f);
        }

        View btnClose = sheetView.findViewById(R.id.btnCloseDobSheet);
        if (btnClose != null) btnClose.setOnClickListener(v -> sheetDialog.dismiss());

        TextView tvSheetTitle = sheetView.findViewById(R.id.tvDobSheetTitle);
        if (tvSheetTitle != null && title != null && !title.isEmpty()) {
            tvSheetTitle.setText(title);
        }

        TextView tvSheetSub = sheetView.findViewById(R.id.tvDobSheetSub);
        if (tvSheetSub != null && subtitle != null && !subtitle.isEmpty()) {
            tvSheetSub.setText(subtitle);
        }

        TextView tvPreview = sheetView.findViewById(R.id.tvDobPreviewText);
        TextView tvPreviewAge = sheetView.findViewById(R.id.tvDobPreviewAge);
        NumberPicker npDay = sheetView.findViewById(R.id.npDobDay);
        NumberPicker npMonth = sheetView.findViewById(R.id.npDobMonth);
        NumberPicker npYear = sheetView.findViewById(R.id.npDobYear);
        View btnDone = sheetView.findViewById(R.id.btnDoneDob);

        Calendar today = Calendar.getInstance();
        int curYear = today.get(Calendar.YEAR);

        int minYear;
        int maxYear;
        int initYear;
        int initMonth;
        int initDay;

        if (isExpiryPicker) {
            minYear = curYear - 10;
            maxYear = curYear + 25;
            initYear = curYear + 5;
            initMonth = today.get(Calendar.MONTH);
            initDay = today.get(Calendar.DAY_OF_MONTH);
        } else if (isChildPicker) {
            minYear = curYear - 18;
            maxYear = curYear;
            initYear = curYear - 5;
            initMonth = 0;
            initDay = 1;
        } else {
            minYear = 1900;
            maxYear = curYear;
            initYear = curYear - 26;
            initMonth = 0;
            initDay = 1;
        }

        String currentDateStr = selectedDateHolder != null && selectedDateHolder.length > 0 ? selectedDateHolder[0] : "";
        if (currentDateStr != null && !currentDateStr.trim().isEmpty()) {
            try {
                String[] parts = currentDateStr.trim().split("-");
                if (parts.length == 3) {
                    initYear = Integer.parseInt(parts[0]);
                    initMonth = Integer.parseInt(parts[1]) - 1;
                    initDay = Integer.parseInt(parts[2]);
                }
            } catch (Exception ignored) {}
        }

        boolean isMalay = "ms".equalsIgnoreCase(LocaleHelper.getSavedLanguage(context));
        String[] monthNames = isMalay
                ? new String[]{"Jan", "Feb", "Mac", "Apr", "Mei", "Jun", "Jul", "Ogo", "Sep", "Okt", "Nov", "Dis"}
                : new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        if (npMonth != null) {
            npMonth.setMinValue(0);
            npMonth.setMaxValue(11);
            npMonth.setDisplayedValues(monthNames);
            npMonth.setValue(Math.max(0, Math.min(11, initMonth)));
            npMonth.setWrapSelectorWheel(true);
        }

        if (npYear != null) {
            npYear.setMinValue(minYear);
            npYear.setMaxValue(maxYear);
            npYear.setValue(Math.max(minYear, Math.min(maxYear, initYear)));
            npYear.setWrapSelectorWheel(false);
        }

        Runnable updateDayMax = () -> {
            int y = npYear != null ? npYear.getValue() : curYear;
            int m = npMonth != null ? npMonth.getValue() : 0;
            Calendar tempCal = Calendar.getInstance();
            tempCal.set(Calendar.YEAR, y);
            tempCal.set(Calendar.MONTH, m);
            tempCal.set(Calendar.DAY_OF_MONTH, 1);
            int maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);
            if (npDay != null) {
                int oldVal = npDay.getValue();
                npDay.setMinValue(1);
                npDay.setMaxValue(maxDays);
                npDay.setValue(Math.max(1, Math.min(maxDays, oldVal)));
                npDay.setWrapSelectorWheel(true);
            }
        };

        updateDayMax.run();
        if (npDay != null) {
            npDay.setValue(Math.max(1, Math.min(31, initDay)));
        }

        Runnable updatePreview = () -> {
            int d = npDay != null ? npDay.getValue() : 1;
            int m = npMonth != null ? npMonth.getValue() : 0;
            int y = npYear != null ? npYear.getValue() : curYear;
            String iso = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d);
            String displayStr = ProfileActivity.formatDateDisplay(context, iso);
            if (tvPreview != null) {
                tvPreview.setText(displayStr);
            }
            if (isExpiryPicker) {
                if (tvPreviewAge != null) {
                    tvPreviewAge.setVisibility(View.GONE);
                }
            } else {
                int age = curYear - y;
                if (today.get(Calendar.MONTH) < m ||
                        (today.get(Calendar.MONTH) == m && today.get(Calendar.DAY_OF_MONTH) < d)) {
                    age--;
                }
                if (tvPreviewAge != null) {
                    if (age >= 0) {
                        tvPreviewAge.setText(context.getString(R.string.profile_age_format, age));
                        tvPreviewAge.setVisibility(View.VISIBLE);
                    } else {
                        tvPreviewAge.setVisibility(View.GONE);
                    }
                }
            }
        };

        updatePreview.run();

        NumberPicker.OnValueChangeListener changeListener = (picker, oldVal, newVal) -> {
            if (picker == npMonth || picker == npYear) {
                updateDayMax.run();
            }
            updatePreview.run();
        };

        if (npDay != null) npDay.setOnValueChangedListener(changeListener);
        if (npMonth != null) npMonth.setOnValueChangedListener(changeListener);
        if (npYear != null) npYear.setOnValueChangedListener(changeListener);

        if (btnDone != null) {
            btnDone.setOnClickListener(v -> {
                HapticUtil.click(v);
                int d = npDay != null ? npDay.getValue() : 1;
                int m = npMonth != null ? npMonth.getValue() : 0;
                int y = npYear != null ? npYear.getValue() : curYear;
                String iso = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d);
                String formattedDisplay = ProfileActivity.formatDateDisplay(context, iso);

                int age = curYear - y;
                if (today.get(Calendar.MONTH) < m ||
                        (today.get(Calendar.MONTH) == m && today.get(Calendar.DAY_OF_MONTH) < d)) {
                    age--;
                }

                if (selectedDateHolder != null && selectedDateHolder.length > 0) {
                    selectedDateHolder[0] = iso;
                }
                if (tvTargetValue != null) {
                    tvTargetValue.setText(formattedDisplay);
                    tvTargetValue.setTextColor(ContextCompat.getColor(context, R.color.text_dark));
                }
                if (!isExpiryPicker && tvTargetAge != null) {
                    if (age >= 0) {
                        tvTargetAge.setText(context.getString(R.string.profile_age_format, age));
                        tvTargetAge.setVisibility(View.VISIBLE);
                    } else {
                        tvTargetAge.setVisibility(View.GONE);
                    }
                }
                if (listener != null) {
                    listener.onDateSelected(iso, formattedDisplay, age);
                }
                sheetDialog.dismiss();
            });
        }

        sheetDialog.show();
        return sheetDialog;
    }
}
