package com.hafiztraveltours.app.ui;

import android.animation.ValueAnimator;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.hafiztraveltours.app.BuildConfig;
import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.utils.HapticUtil;

public class AboutActivity extends BaseActivity {

    private ValueAnimator shimmerAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        findViewById(R.id.aboutBackButton).setOnClickListener(v -> {
            HapticUtil.click(v);
            finish();
        });

        TextView tvAppVersion = findViewById(R.id.tvAppVersion);
        if (tvAppVersion != null) {
            tvAppVersion.setText("v" + BuildConfig.VERSION_NAME);
        }

        setupDanialShimmer();
    }

    private void setupDanialShimmer() {
        TextView tvDanial = findViewById(R.id.tvAuthorDanial);
        if (tvDanial == null) return;

        tvDanial.post(() -> {
            if (isFinishing() || isDestroyed()) return;

            float width = tvDanial.getPaint().measureText(tvDanial.getText().toString());
            if (width <= 0) width = 120f;

            int baseColor = ContextCompat.getColor(this, R.color.pink_dark);
            int glowColor = 0xFFFFA0B8; // Soft, elegant rose glow

            LinearGradient shader = new LinearGradient(
                    -width, 0, width, 0,
                    new int[]{baseColor, glowColor, baseColor},
                    new float[]{0f, 0.5f, 1f},
                    Shader.TileMode.CLAMP
            );
            tvDanial.getPaint().setShader(shader);

            Matrix matrix = new Matrix();
            shimmerAnimator = ValueAnimator.ofFloat(-width * 1.5f, width * 2.5f);
            shimmerAnimator.setDuration(3200);
            shimmerAnimator.setRepeatCount(ValueAnimator.INFINITE);
            shimmerAnimator.setRepeatMode(ValueAnimator.RESTART);
            shimmerAnimator.setInterpolator(new LinearInterpolator());
            shimmerAnimator.addUpdateListener(animation -> {
                if (tvDanial.getPaint() != null) {
                    float translate = (float) animation.getAnimatedValue();
                    matrix.setTranslate(translate, 0);
                    shader.setLocalMatrix(matrix);
                    tvDanial.invalidate();
                }
            });
            shimmerAnimator.start();
        });
    }

    @Override
    protected void onDestroy() {
        if (shimmerAnimator != null) {
            shimmerAnimator.cancel();
            shimmerAnimator = null;
        }
        super.onDestroy();
    }
}
