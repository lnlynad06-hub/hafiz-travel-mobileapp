package com.hafiztraveltours.app.views;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.DocumentDto;
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.utils.ZoomableTouchListener;

import java.io.InputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Native in-app document viewer dialog.
 * Never redirects outside the app or uses ACTION_VIEW intents.
 * Displays retrieved image with pinch-to-zoom, pan, loading indicator,
 * meaningful error states, and retry.
 */
public class DocumentViewerDialog extends Dialog {

    private final String documentTitle;
    private final String fileName;
    private final String rawUrl;

    private ImageView btnClose;
    private TextView tvTitle;
    private TextView tvSubtitle;
    private ImageView ivImage;
    private LinearLayout llLoading;
    private LinearLayout llError;
    private TextView tvErrorMsg;
    private MaterialButton btnRetry;
    private TextView tvZoomHint;

    private Call<ResponseBody> activeDownloadCall;
    private ZoomableTouchListener zoomTouchListener;
    private Bitmap loadedBitmap;

    public DocumentViewerDialog(@NonNull Context context, String documentTitle, DocumentDto doc) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.documentTitle = (documentTitle != null && !documentTitle.isEmpty())
                ? documentTitle
                : (doc != null && doc.title != null ? doc.title : context.getString(R.string.doc_passport_copy));
        this.fileName = (doc != null && doc.fileName != null) ? doc.fileName : "";
        this.rawUrl = (doc != null) ? doc.getEffectiveUrl() : null;
    }

    public DocumentViewerDialog(@NonNull Context context, String documentTitle, String fileName, String rawUrl) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.documentTitle = documentTitle != null ? documentTitle : context.getString(R.string.doc_passport_copy);
        this.fileName = fileName != null ? fileName : "";
        this.rawUrl = rawUrl;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_document_viewer);

        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            getWindow().setBackgroundDrawableResource(android.R.color.black);
        }

        initViews();
        bindData();
        loadDocument();
    }

    private void initViews() {
        btnClose = findViewById(R.id.btnDocViewerClose);
        tvTitle = findViewById(R.id.tvDocViewerTitle);
        tvSubtitle = findViewById(R.id.tvDocViewerSubtitle);
        ivImage = findViewById(R.id.ivDocViewerImage);
        llLoading = findViewById(R.id.llDocViewerLoading);
        llError = findViewById(R.id.llDocViewerError);
        tvErrorMsg = findViewById(R.id.tvDocViewerErrorMsg);
        btnRetry = findViewById(R.id.btnDocViewerRetry);
        tvZoomHint = findViewById(R.id.tvDocViewerZoomHint);

        zoomTouchListener = new ZoomableTouchListener(ivImage);
        ivImage.setOnTouchListener(zoomTouchListener);

        btnClose.setOnClickListener(v -> dismiss());
        btnRetry.setOnClickListener(v -> loadDocument());
    }

    private void bindData() {
        tvTitle.setText(documentTitle);
        if (fileName != null && !fileName.isEmpty()) {
            tvSubtitle.setText(fileName);
            tvSubtitle.setVisibility(View.VISIBLE);
        } else {
            tvSubtitle.setVisibility(View.GONE);
        }
    }

    private void loadDocument() {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            showError(getContext().getString(R.string.doc_file_not_found));
            return;
        }

        String targetUrl = normalizeDocumentUrl(rawUrl);

        showLoading();

        if (activeDownloadCall != null && !activeDownloadCall.isCanceled()) {
            activeDownloadCall.cancel();
        }

        activeDownloadCall = ApiClient.getApiService().downloadFile(targetUrl);
        activeDownloadCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful()) {
                    handleHttpError(response.code());
                    return;
                }

                ResponseBody body = response.body();
                if (body == null) {
                    showError(getContext().getString(R.string.doc_file_not_found));
                    return;
                }

                new Thread(() -> {
                    try (InputStream is = body.byteStream()) {
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        if (bitmap != null) {
                            if (ivImage != null) {
                                ivImage.post(() -> showImage(bitmap));
                            }
                        } else {
                            if (ivImage != null) {
                                ivImage.post(() -> showError(getContext().getString(R.string.doc_viewer_unsupported)));
                            }
                        }
                    } catch (Exception e) {
                        if (ivImage != null) {
                            ivImage.post(() -> showError(getContext().getString(R.string.doc_viewer_load_failed)));
                        }
                    }
                }).start();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (call.isCanceled()) return;
                handleNetworkFailure(t);
            }
        });
    }

    private void showLoading() {
        llLoading.setVisibility(View.VISIBLE);
        llError.setVisibility(View.GONE);
        ivImage.setVisibility(View.GONE);
        tvZoomHint.setVisibility(View.GONE);
    }

    private void showImage(Bitmap bitmap) {
        this.loadedBitmap = bitmap;
        llLoading.setVisibility(View.GONE);
        llError.setVisibility(View.GONE);
        ivImage.setVisibility(View.VISIBLE);
        tvZoomHint.setVisibility(View.VISIBLE);

        ivImage.setImageBitmap(bitmap);
        ivImage.post(() -> {
            if (zoomTouchListener != null) {
                zoomTouchListener.updateBaseMatrix();
            }
        });
    }

    private void showError(String msg) {
        llLoading.setVisibility(View.GONE);
        ivImage.setVisibility(View.GONE);
        tvZoomHint.setVisibility(View.GONE);
        llError.setVisibility(View.VISIBLE);
        tvErrorMsg.setText(msg != null ? msg : getContext().getString(R.string.doc_viewer_load_failed));
    }

    private void handleHttpError(int code) {
        Context ctx = getContext();
        switch (code) {
            case 401:
                showError(ctx.getString(R.string.err_session_expired));
                break;
            case 403:
                showError(ctx.getString(R.string.err_forbidden));
                break;
            case 404:
                showError(ctx.getString(R.string.doc_file_not_found));
                break;
            default:
                showError(ctx.getString(R.string.doc_viewer_load_failed));
                break;
        }
    }

    private void handleNetworkFailure(Throwable t) {
        Context ctx = getContext();
        if (t instanceof SocketTimeoutException) {
            showError(ctx.getString(R.string.err_timeout));
        } else if (t instanceof UnknownHostException || t instanceof ConnectException) {
            showError(ctx.getString(R.string.err_no_internet));
        } else {
            showError(ctx.getString(R.string.doc_viewer_load_failed));
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (activeDownloadCall != null && !activeDownloadCall.isCanceled()) {
            activeDownloadCall.cancel();
        }
    }

    /**
     * Resolves document URL against active ApiClient.BASE_URL.
     * Rewrites localhost/127.0.0.1 authority to match active API base URL for ADB reverse testing.
     */
    public static String normalizeDocumentUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) return "";
        String trimmed = rawUrl.trim();

        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            String baseUrl = ApiClient.BASE_URL;
            if (baseUrl.endsWith("/api/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 5);
            } else if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            if (!trimmed.startsWith("/")) {
                trimmed = "/" + trimmed;
            }
            return baseUrl + trimmed;
        }

        try {
            android.net.Uri docUri = android.net.Uri.parse(trimmed);
            android.net.Uri baseUri = android.net.Uri.parse(ApiClient.BASE_URL);
            String docHost = docUri.getHost();
            if (docHost != null && (docHost.equals("localhost") || docHost.equals("127.0.0.1") || docHost.equals("10.0.2.2"))) {
                return docUri.buildUpon()
                        .scheme(baseUri.getScheme())
                        .encodedAuthority(baseUri.getEncodedAuthority())
                        .build()
                        .toString();
            }
        } catch (Exception ignored) {}

        return trimmed;
    }
}
