package com.hafiztraveltours.app.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.hafiztraveltours.app.ui.PackageDetailActivity;

/**
 * Repeated navigation patterns only (M3). thin wrappers over explicit Intents —
 * destinations and extras are identical to the previously inlined code.
 */
public final class Navigator {

    private Navigator() {}

    /** Opens package detail. Extras: EXTRA_COLLECTION + EXTRA_PACKAGE_ID. */
    public static void openPackage(Context context, String collection, String packageId) {
        if (context == null) return;
        Intent intent = new Intent(context, PackageDetailActivity.class);
        intent.putExtra(PackageDetailActivity.EXTRA_COLLECTION, collection);
        intent.putExtra(PackageDetailActivity.EXTRA_PACKAGE_ID, packageId);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }
}
