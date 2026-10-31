package com.hafiztraveltours.app.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.hafiztraveltours.app.R;

public class BookingFunnelHelper {

    public static final int STAGE_PACKAGE = 1;
    public static final int STAGE_ROOM = 2;
    public static final int STAGE_CUSTOMIZE = 3;
    public static final int STAGE_TRAVELLERS = 4;
    public static final int STAGE_PAYMENT = 5;

    public static void setup(View headerRoot, boolean hasCustomization, int activeStage) {
        if (headerRoot == null) return;
        Context ctx = headerRoot.getContext();

        TextView tv1 = headerRoot.findViewById(R.id.txtFunnelStep1);
        TextView tv2 = headerRoot.findViewById(R.id.txtFunnelStep2);
        TextView tv3 = headerRoot.findViewById(R.id.txtFunnelStep3);
        TextView tv4 = headerRoot.findViewById(R.id.txtFunnelStep4);
        TextView tv5 = headerRoot.findViewById(R.id.txtFunnelStep5);

        View sep4 = headerRoot.findViewById(R.id.sepFunnel4);

        int activeColor = ContextCompat.getColor(ctx, R.color.brand_magenta);
        int inactiveColor = ContextCompat.getColor(ctx, R.color.text_gray);

        if (hasCustomization) {
            // 5 stages: 1. Package, 2. Room, 3. Customize, 4. Travellers, 5. Payment
            if (tv1 != null) tv1.setText(R.string.step_funnel_5_package);
            if (tv2 != null) tv2.setText(R.string.step_funnel_5_room);
            if (tv3 != null) tv3.setText(R.string.step_funnel_5_customize);
            if (tv4 != null) tv4.setText(R.string.step_funnel_5_travellers);
            if (tv5 != null) {
                tv5.setText(R.string.step_funnel_5_payment);
                tv5.setVisibility(View.VISIBLE);
            }
            if (sep4 != null) sep4.setVisibility(View.VISIBLE);

            boolean isStep1 = (activeStage == STAGE_PACKAGE || activeStage == 1);
            boolean isStep2 = (activeStage == STAGE_ROOM || activeStage == 2);
            boolean isStep3 = (activeStage == STAGE_CUSTOMIZE || activeStage == 3);
            boolean isStep4 = (activeStage == STAGE_TRAVELLERS || activeStage == 4);
            boolean isStep5 = (activeStage == STAGE_PAYMENT || activeStage == 5);

            setStageStyle(tv1, isStep1, activeColor, inactiveColor);
            setStageStyle(tv2, isStep2, activeColor, inactiveColor);
            setStageStyle(tv3, isStep3, activeColor, inactiveColor);
            setStageStyle(tv4, isStep4, activeColor, inactiveColor);
            setStageStyle(tv5, isStep5, activeColor, inactiveColor);
        } else {
            // 4 stages: 1. Package, 2. Room, 3. Travellers, 4. Payment
            if (tv1 != null) tv1.setText(R.string.step_funnel_package);
            if (tv2 != null) tv2.setText(R.string.step_funnel_room);
            if (tv3 != null) tv3.setText(R.string.step_funnel_travellers);
            if (tv4 != null) tv4.setText(R.string.step_funnel_payment);
            if (tv5 != null) tv5.setVisibility(View.GONE);
            if (sep4 != null) sep4.setVisibility(View.GONE);

            boolean isStep1 = (activeStage == STAGE_PACKAGE || activeStage == 1);
            boolean isStep2 = (activeStage == STAGE_ROOM || activeStage == 2);
            boolean isStep3 = (activeStage == STAGE_TRAVELLERS || activeStage == 3);
            boolean isStep4 = (activeStage == STAGE_PAYMENT || activeStage == 4);

            setStageStyle(tv1, isStep1, activeColor, inactiveColor);
            setStageStyle(tv2, isStep2, activeColor, inactiveColor);
            setStageStyle(tv3, isStep3, activeColor, inactiveColor);
            setStageStyle(tv4, isStep4, activeColor, inactiveColor);
        }
    }

    private static void setStageStyle(TextView tv, boolean isActive, int activeColor, int inactiveColor) {
        if (tv == null) return;
        tv.setTextColor(isActive ? activeColor : inactiveColor);
        tv.setTypeface(null, isActive ? Typeface.BOLD : Typeface.NORMAL);
    }
}
