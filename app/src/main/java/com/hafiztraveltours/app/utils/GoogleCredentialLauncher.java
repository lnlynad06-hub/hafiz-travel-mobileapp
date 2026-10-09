package com.hafiztraveltours.app.utils;

import android.app.Activity;

import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.hafiztraveltours.app.BuildConfig;

/** Retrieves a Google ID token only; the server remains the identity authority. */
public final class GoogleCredentialLauncher {
    public interface Callback {
        void onToken(String idToken);
        void onError(boolean cancelled);
        default void onError(boolean cancelled, String detail) {
            onError(cancelled);
        }
    }

    private GoogleCredentialLauncher() {}

    public static boolean launch(Activity activity, Callback callback) {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID == null || BuildConfig.GOOGLE_WEB_CLIENT_ID.isEmpty()) {
            return false;
        }

        GetSignInWithGoogleOption option = new GetSignInWithGoogleOption.Builder(
                BuildConfig.GOOGLE_WEB_CLIENT_ID).build();
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build();

        CredentialManager.create(activity).getCredentialAsync(activity, request, null,
                ContextCompat.getMainExecutor(activity),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        try {
                            Credential credential = result.getCredential();
                            if (!GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())
                                    && !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL.equals(credential.getType())) {
                                callback.onError(false, "Unexpected credential type: " + credential.getType());
                                return;
                            }
                            String token = GoogleIdTokenCredential.createFrom(credential.getData()).getIdToken();
                            if (token == null || token.isEmpty()) {
                                callback.onError(false, "Empty ID token");
                                return;
                            }
                            callback.onToken(token);
                        } catch (Exception e) {
                            android.util.Log.e("GoogleAuth", "Credential parsing error", e);
                            callback.onError(false, e.getMessage());
                        }
                    }

                    @Override
                    public void onError(GetCredentialException error) {
                        String errType = error.getClass().getSimpleName();
                        String errMsg = error.getMessage();
                        android.util.Log.e("GoogleAuth", "CredentialManager onError: type=" + errType + ", msg=" + errMsg, error);
                        boolean isCancelled = error.getClass().getSimpleName().contains("Cancellation");
                        callback.onError(isCancelled, errType != null ? errType : errMsg);
                    }
                });
        return true;
    }
}
