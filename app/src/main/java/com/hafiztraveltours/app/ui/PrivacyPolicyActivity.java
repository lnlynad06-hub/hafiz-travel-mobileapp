package com.hafiztraveltours.app.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.utils.HapticUtil;
import com.hafiztraveltours.app.utils.OnboardingManager;
import com.hafiztraveltours.app.utils.PrivacyPolicyManager;

/**
 * Dedicated Privacy Policy screen displaying the official Hafiz Travel Privacy Policy
 * via an embedded WebView with error handling, retry option, and neumorphic Accept/Decline gates.
 */
public class PrivacyPolicyActivity extends BaseActivity {

    public static final String EXTRA_VIEW_ONLY = "extra_view_only";

    private WebView webView;
    private ProgressBar progressBar;
    private View errorLayout;
    private boolean hasLoadingError = false;
    private boolean isViewOnly = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        isViewOnly = getIntent().getBooleanExtra(EXTRA_VIEW_ONLY, false);

        initViews();
        setupWebView();
        setupNavigationAndActions();
    }

    private void initViews() {
        webView = findViewById(R.id.privacyWebView);
        progressBar = findViewById(R.id.privacyProgress);
        errorLayout = findViewById(R.id.privacyErrorLayout);

        TextView tvVersionBadge = findViewById(R.id.tvPrivacyVersionBadge);
        if (tvVersionBadge != null) {
            tvVersionBadge.setText("v" + PrivacyPolicyManager.CURRENT_POLICY_VERSION);
        }

        View layoutConsentActions = findViewById(R.id.layoutConsentActions);
        View layoutViewOnlyActions = findViewById(R.id.layoutViewOnlyActions);
        View btnOpenInBrowser = findViewById(R.id.btnOpenInBrowser);

        if (isViewOnly) {
            if (layoutConsentActions != null) layoutConsentActions.setVisibility(View.GONE);
            if (layoutViewOnlyActions != null) layoutViewOnlyActions.setVisibility(View.VISIBLE);
            if (btnOpenInBrowser != null) {
                btnOpenInBrowser.setVisibility(View.VISIBLE);
                btnOpenInBrowser.setOnClickListener(v -> {
                    HapticUtil.click(v);
                    openInExternalBrowser();
                });
            }
        } else {
            if (layoutConsentActions != null) layoutConsentActions.setVisibility(View.VISIBLE);
            if (layoutViewOnlyActions != null) layoutViewOnlyActions.setVisibility(View.GONE);
            if (btnOpenInBrowser != null) btnOpenInBrowser.setVisibility(View.GONE);
        }

        findViewById(R.id.btnPrivacyRetry).setOnClickListener(v -> {
            HapticUtil.click(v);
            reloadPolicy();
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setDatabaseEnabled(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (progressBar != null) {
                    progressBar.setProgress(newProgress);
                    if (newProgress >= 100 || hasLoadingError) {
                        progressBar.setVisibility(View.GONE);
                    } else {
                        progressBar.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                hasLoadingError = false;
                if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                if (errorLayout != null) errorLayout.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request != null && request.isForMainFrame()) {
                    hasLoadingError = true;
                    showErrorState();
                }
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                hasLoadingError = true;
                showErrorState();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!hasLoadingError) {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (errorLayout != null) errorLayout.setVisibility(View.GONE);
                    if (webView != null) webView.setVisibility(View.VISIBLE);
                }
            }
        });

        loadPolicyUrl();
    }

    private void loadPolicyUrl() {
        hasLoadingError = false;
        if (webView != null) {
            webView.setVisibility(View.VISIBLE);
            webView.loadUrl(PrivacyPolicyManager.OFFICIAL_POLICY_URL);
        }
    }

    private void reloadPolicy() {
        hasLoadingError = false;
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (errorLayout != null) errorLayout.setVisibility(View.GONE);
        if (webView != null) {
            webView.setVisibility(View.VISIBLE);
            webView.reload();
        }
    }

    private void showErrorState() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (webView != null) webView.setVisibility(View.GONE);
        if (errorLayout != null) errorLayout.setVisibility(View.VISIBLE);
    }

    private void setupNavigationAndActions() {
        View btnBack = findViewById(R.id.btnPrivacyBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                HapticUtil.click(v);
                if (isViewOnly) {
                    finish();
                } else {
                    showDeclineExitDialog();
                }
            });
        }

        View btnAccept = findViewById(R.id.btnPrivacyAccept);
        if (btnAccept != null) {
            btnAccept.setOnClickListener(v -> {
                HapticUtil.click(v);
                handleAcceptConsent();
            });
        }

        View btnDecline = findViewById(R.id.btnPrivacyDecline);
        if (btnDecline != null) {
            btnDecline.setOnClickListener(v -> {
                HapticUtil.click(v);
                showDeclineExitDialog();
            });
        }

        View btnClose = findViewById(R.id.btnPrivacyClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                HapticUtil.click(v);
                finish();
            });
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isViewOnly) {
                    finish();
                } else if (webView != null && webView.canGoBack()) {
                    webView.goBack();
                } else {
                    showDeclineExitDialog();
                }
            }
        });
    }

    private void handleAcceptConsent() {
        PrivacyPolicyManager.setPrivacyPolicyAccepted(this, true);

        Class<?> nextActivity = OnboardingManager.isOnboardingCompleted(this)
                ? MainActivity.class
                : WelcomeActivity.class;

        Intent intent = new Intent(PrivacyPolicyActivity.this, nextActivity);
        startActivity(intent);
        overridePendingTransition(R.anim.nav_seamless_fade_in, R.anim.nav_seamless_fade_out);
        finish();
    }

    private void showDeclineExitDialog() {
        new AlertDialog.Builder(this, R.style.LuxuryDialogTheme)
                .setTitle(R.string.privacy_policy_decline_dialog_title)
                .setMessage(R.string.privacy_policy_decline_dialog_msg)
                .setPositiveButton(R.string.privacy_policy_decline_dialog_review, (dialog, which) -> dialog.dismiss())
                .setNegativeButton(R.string.privacy_policy_decline_dialog_exit, (dialog, which) -> {
                    finishAffinity();
                })
                .setCancelable(false)
                .show();
    }

    private void openInExternalBrowser() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PrivacyPolicyManager.OFFICIAL_POLICY_URL));
            startActivity(intent);
        } catch (Exception ignored) {
            // Safe fallback if no browser is installed
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
