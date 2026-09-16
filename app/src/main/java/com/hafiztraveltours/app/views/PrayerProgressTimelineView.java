package com.hafiztraveltours.app.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.utils.PrayerProgressCalculator;

/**
 * Editorial-style Prayer Progression Timeline.
 * Connects Subuh -> Zohor -> Asar -> Maghrib -> Isyak with a minimal horizontal line,
 * 5 subtle node dots, and a dynamic current position indicator calculated from real time.
 */
public class PrayerProgressTimelineView extends View {

    private final Paint lineInactivePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lineActivePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotInactivePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotActivePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint currentIndicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint currentOuterRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int activePrayerIndex = 0; // 0..4 (Subuh..Isyak)
    private float progressBetweenSegment = 0f; // 0f..1f between active and next prayer

    public PrayerProgressTimelineView(Context context) {
        super(context);
        init(context);
    }

    public PrayerProgressTimelineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PrayerProgressTimelineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        int magentaColor = ContextCompat.getColor(context, R.color.brand_magenta);
        int inactiveColor = Color.parseColor("#CBD5E1");

        lineInactivePaint.setStyle(Paint.Style.STROKE);
        lineInactivePaint.setStrokeWidth(dp(2));
        lineInactivePaint.setColor(inactiveColor);
        lineInactivePaint.setStrokeCap(Paint.Cap.ROUND);

        lineActivePaint.setStyle(Paint.Style.STROKE);
        lineActivePaint.setStrokeWidth(dp(2.5f));
        lineActivePaint.setColor(magentaColor);
        lineActivePaint.setStrokeCap(Paint.Cap.ROUND);

        dotInactivePaint.setStyle(Paint.Style.FILL);
        dotInactivePaint.setColor(inactiveColor);

        dotActivePaint.setStyle(Paint.Style.FILL);
        dotActivePaint.setColor(magentaColor);

        currentIndicatorPaint.setStyle(Paint.Style.FILL);
        currentIndicatorPaint.setColor(magentaColor);

        currentOuterRingPaint.setStyle(Paint.Style.STROKE);
        currentOuterRingPaint.setStrokeWidth(dp(2.5f));
        currentOuterRingPaint.setColor(Color.WHITE);
    }

    /**
     * Updates the timeline visualization based on calculated prayer progress.
     */
    public void setPrayerData(String[] names, long[] epochSeconds, long nowEpochSeconds) {
        if (names == null || epochSeconds == null || epochSeconds.length < 5) return;

        PrayerProgressCalculator.Result result =
                PrayerProgressCalculator.calculate(names, epochSeconds, nowEpochSeconds);

        int currentIdx = 0;
        for (int i = 0; i < names.length; i++) {
            if (names[i].equalsIgnoreCase(result.currentName)) {
                currentIdx = i;
                break;
            }
        }

        this.activePrayerIndex = currentIdx;
        this.progressBetweenSegment = Math.max(0f, Math.min(1f, result.progress));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        float centerY = height / 2f;
        int nodeCount = 5;
        float paddingX = width / (nodeCount * 2f); // Centers 5 equal columns
        float stepX = (width - 2 * paddingX) / (nodeCount - 1);

        float[] nodeX = new float[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            nodeX[i] = paddingX + i * stepX;
        }

        // 1. Draw base inactive horizontal line connecting first to last node
        canvas.drawLine(nodeX[0], centerY, nodeX[nodeCount - 1], centerY, lineInactivePaint);

        // 2. Calculate dynamic indicator X coordinate
        float indicatorX;
        if (activePrayerIndex < nodeCount - 1) {
            indicatorX = nodeX[activePrayerIndex] + progressBetweenSegment * (nodeX[activePrayerIndex + 1] - nodeX[activePrayerIndex]);
        } else {
            // After Isyak (segment wrap to Subuh)
            indicatorX = nodeX[4] + progressBetweenSegment * (width - nodeX[4]);
            if (indicatorX > width - dp(4)) indicatorX = nodeX[0] + progressBetweenSegment * nodeX[0];
        }

        // 3. Draw active progress line up to current position
        float activeLineEndX = Math.min(indicatorX, nodeX[nodeCount - 1]);
        canvas.drawLine(nodeX[0], centerY, activeLineEndX, centerY, lineActivePaint);

        // 4. Draw 5 static prayer node markers
        float nodeRadius = dp(4f);
        for (int i = 0; i < nodeCount; i++) {
            if (nodeX[i] <= activeLineEndX) {
                canvas.drawCircle(nodeX[i], centerY, nodeRadius, dotActivePaint);
            } else {
                canvas.drawCircle(nodeX[i], centerY, nodeRadius, dotInactivePaint);
            }
        }

        // 5. Draw prominent current position indicator
        float currentRadius = dp(6.5f);
        canvas.drawCircle(indicatorX, centerY, currentRadius + dp(1.5f), currentOuterRingPaint);
        canvas.drawCircle(indicatorX, centerY, currentRadius, currentIndicatorPaint);
    }

    private float dp(float val) {
        return val * getResources().getDisplayMetrics().density;
    }
}
