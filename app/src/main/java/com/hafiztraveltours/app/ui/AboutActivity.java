package com.hafiztraveltours.app.ui;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import android.widget.Toast;

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

        // Header back button
        findViewById(R.id.aboutBackButton).setOnClickListener(v -> {
            HapticUtil.click(v);
            finish();
        });

        // App Version dynamically from build configuration
        TextView tvAppVersion = findViewById(R.id.tvAppVersion);
        if (tvAppVersion != null) {
            tvAppVersion.setText("v" + BuildConfig.VERSION_NAME);
        }

        // Contact Intent Listeners
        setupContactListeners();

        // Social Media Intent Listeners
        setupSocialListeners();

        // Privacy Policy
        View btnPrivacyPolicy = findViewById(R.id.btnAboutPrivacyPolicy);
        if (btnPrivacyPolicy != null) {
            btnPrivacyPolicy.setOnClickListener(v -> {
                HapticUtil.click(v);
                Intent intent = new Intent(AboutActivity.this, PrivacyPolicyActivity.class);
                intent.putExtra(PrivacyPolicyActivity.EXTRA_VIEW_ONLY, true);
                startActivity(intent);
            });
        }

        // Developer Shimmer & Link
        setupDanialShimmer();
    }

    private void setupSocialListeners() {
        // TikTok
        View btnTikTok = findViewById(R.id.btnSocialTikTok);
        if (btnTikTok != null) {
            btnTikTok.setOnClickListener(v -> {
                HapticUtil.click(v);
                openSocialLink("https://www.tiktok.com/@hafiztravelofficial", "com.zhiliaoapp.musically");
            });
        }

        // Threads
        View btnThreads = findViewById(R.id.btnSocialThreads);
        if (btnThreads != null) {
            btnThreads.setOnClickListener(v -> {
                HapticUtil.click(v);
                openSocialLink("https://www.threads.net/@hafiztravelofficial", "com.instagram.barcelona");
            });
        }

        // Instagram
        View btnInstagram = findViewById(R.id.btnSocialInstagram);
        if (btnInstagram != null) {
            btnInstagram.setOnClickListener(v -> {
                HapticUtil.click(v);
                openSocialLink("https://www.instagram.com/hafiztravelofficial/", "com.instagram.android");
            });
        }

        // Facebook
        View btnFacebook = findViewById(R.id.btnSocialFacebook);
        if (btnFacebook != null) {
            btnFacebook.setOnClickListener(v -> {
                HapticUtil.click(v);
                openSocialLink("https://www.facebook.com/hafiztravelntours", "com.facebook.katana");
            });
        }

        // YouTube
        View btnYouTube = findViewById(R.id.btnSocialYouTube);
        if (btnYouTube != null) {
            btnYouTube.setOnClickListener(v -> {
                HapticUtil.click(v);
                openSocialLink("https://www.youtube.com/@hafiztravelandtours", "com.google.android.youtube");
            });
        }
    }

    private void openSocialLink(String url, String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            if (packageName != null) {
                intent.setPackage(packageName);
            }
            startActivity(intent);
        } catch (Exception e) {
            startActivitySafely(new Intent(Intent.ACTION_VIEW, Uri.parse(url)), "No app found to open link");
        }
    }

    private void setupContactListeners() {
        // Phone Dialer Intent
        View btnPhone = findViewById(R.id.btnContactPhone);
        if (btnPhone != null) {
            btnPhone.setOnClickListener(v -> {
                HapticUtil.click(v);
                String phoneNumber = getString(R.string.about_phone_value);
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber.replace(" ", "").replace("-", "")));
                startActivitySafely(intent, "No dialer app found");
            });
        }

        // Email Client Intent
        View btnEmail = findViewById(R.id.btnContactEmail);
        if (btnEmail != null) {
            btnEmail.setOnClickListener(v -> {
                HapticUtil.click(v);
                String email = getString(R.string.about_email_value);
                Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email));
                intent.putExtra(Intent.EXTRA_SUBJECT, "Inquiry via Hafiz Travel App");
                startActivitySafely(Intent.createChooser(intent, "Send Email"), "No email app found");
            });
        }

        // Website Browser Intent
        View btnWebsite = findViewById(R.id.btnContactWebsite);
        if (btnWebsite != null) {
            btnWebsite.setOnClickListener(v -> {
                HapticUtil.click(v);
                String webUrl = getString(R.string.about_website_url);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
                startActivitySafely(intent, "No browser app found");
            });
        }

        // Address Maps Intent
        View btnAddress = findViewById(R.id.btnContactAddress);
        if (btnAddress != null) {
            btnAddress.setOnClickListener(v -> {
                HapticUtil.click(v);
                String address = getString(R.string.about_address_value);
                Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(address));
                Intent intent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                intent.setPackage("com.google.android.apps.maps");
                if (intent.resolveActivity(getPackageManager()) == null) {
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=" + Uri.encode(address)));
                }
                startActivitySafely(intent, "No maps app found");
            });
        }
    }



    private void startActivitySafely(Intent intent, String errorMessage) {
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private void setupDanialShimmer() {
        TextView tvDanial = findViewById(R.id.tvAuthorDanial);
        if (tvDanial == null) return;

        // Production: author credit is display-only (no external personal links).
        tvDanial.setOnClickListener(null);
        tvDanial.setClickable(false);
        tvDanial.setFocusable(false);

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
