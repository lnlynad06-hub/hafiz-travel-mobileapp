package com.hafiztraveltours.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hafiztraveltours.app.BuildConfig;
import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.utils.HapticUtil;

public class AboutActivity extends BaseActivity {

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
            tvAppVersion.setText(getString(R.string.version_short_format, BuildConfig.VERSION_NAME));
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
            startActivitySafely(
                    new Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                    getString(R.string.err_open_link));
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
                startActivitySafely(intent, getString(R.string.err_open_call));
            });
        }

        // Email Client Intent
        View btnEmail = findViewById(R.id.btnContactEmail);
        if (btnEmail != null) {
            btnEmail.setOnClickListener(v -> {
                HapticUtil.click(v);
                String email = getString(R.string.about_email_value);
                Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email));
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.about_email_subject));
                startActivitySafely(
                        Intent.createChooser(intent, getString(R.string.send_email_chooser)),
                        getString(R.string.err_open_email));
            });
        }

        // Website Browser Intent
        View btnWebsite = findViewById(R.id.btnContactWebsite);
        if (btnWebsite != null) {
            btnWebsite.setOnClickListener(v -> {
                HapticUtil.click(v);
                String webUrl = getString(R.string.about_website_url);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
                startActivitySafely(intent, getString(R.string.err_open_link));
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
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    startActivitySafely(
                            new Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=" + Uri.encode(address))),
                            getString(R.string.err_open_map));
                }
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

}
