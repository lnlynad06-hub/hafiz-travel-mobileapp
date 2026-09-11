// PrayerArcView.java — FAIL LENGKAP
package com.hafiztraveltours.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * Decorative arc for the homepage "Waktu Solat" widget: a gently curved
 * line from the left edge to the right edge, coloured with a horizontal
 * gradient (soft colours in light theme, existing pink scheme in dark
 * theme - see colors.xml / values-night/colors.xml for
 * prayer_arc_start/prayer_arc_end), plus a dot marker whose position
 * along the arc is driven by setProgress(0f..1f) - how far along we are
 * between the current prayer and the next one.
 */
public class PrayerArcView extends View {

    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path arcPath = new Path();
    private final PathMeasure pathMeasure = new PathMeasure();

    // Dot indicator radius — dibesarkan dari 7dp -> 13dp
    private static final float DOT_RADIUS_DP = 13f;
    private static final float DOT_BORDER_WIDTH_DP = 3f;

    private float progress = 0f; // 0f = at current prayer, 1f = at next prayer

    public PrayerArcView(Context context) {
        super(context);
        init();
    }

    public PrayerArcView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PrayerArcView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(dp(5));
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(getResources().getColor(R.color.prayer_arc_dot));

        dotBorderPaint.setStyle(Paint.Style.STROKE);
        dotBorderPaint.setStrokeWidth(dp(DOT_BORDER_WIDTH_DP));
        dotBorderPaint.setColor(Color.WHITE);
    }

    /** 0f..1f - how far along between the current and next prayer time. */
    public void setProgress(float progressFraction) {
        this.progress = Math.max(0f, Math.min(1f, progressFraction));
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        buildPath(w, h);
    }

    // PrayerArcView.java — buildPath dikemaskini: lengkung lagi flat, sebab arc kini height 56dp sahaja (bukan tinggi penuh card)
    private void buildPath(int w, int h) {
        arcPath.reset();
        if (w <= 0 || h <= 0) return;

        float horizontalInset = dp(DOT_RADIUS_DP + DOT_BORDER_WIDTH_DP);
        float verticalInset = dp(DOT_RADIUS_DP + DOT_BORDER_WIDTH_DP);

        float startX = horizontalInset;
        float endX = w - horizontalInset;
        float startY = h - verticalInset;
        float endY = h - verticalInset;
        float controlX = w / 2f;
        // Lengkung lebih flat (dulu h*0.05f = terlalu curam) - sesuai dengan
        // height arc yang kini kecil (56dp) supaya tak nampak "terjunam"
        float controlY = h * 0.15f;

        arcPath.moveTo(startX, startY);
        arcPath.quadTo(controlX, controlY, endX, endY);

        pathMeasure.setPath(arcPath, false);

        int colorStart = getResources().getColor(R.color.prayer_arc_start);
        int colorEnd = getResources().getColor(R.color.prayer_arc_end);
        arcPaint.setShader(new LinearGradient(
                startX, 0, endX, 0,
                colorStart, colorEnd,
                Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (arcPath.isEmpty()) return;

        canvas.drawPath(arcPath, arcPaint);

        float[] pos = new float[2];
        float[] tan = new float[2];
        float distance = pathMeasure.getLength() * progress;
        pathMeasure.getPosTan(distance, pos, tan);

        float dotRadius = dp(DOT_RADIUS_DP);
        canvas.drawCircle(pos[0], pos[1], dotRadius, dotPaint);
        canvas.drawCircle(pos[0], pos[1], dotRadius, dotBorderPaint);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}