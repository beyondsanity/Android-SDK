package it.near.sdk;

import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import it.near.sdk.logging.NearLog;

public class NearItProvider extends ContentProvider {

    private static final String TAG = "NearItProvider";

    @Override
    public boolean onCreate() {
        Context context = getContext();
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(),
                    PackageManager.GET_META_DATA
            );
            Bundle bundle = ai.metaData;
            String apiKey = bundle.getString("near_api_key");
            if (apiKey == null) {
                NearLog.e(TAG, "Missing near api key from manifest");
                return true;
            }
            NearItManager.init((Application) getContext().getApplicationContext(), apiKey);

        } catch (PackageManager.NameNotFoundException | NullPointerException e) {
            NearLog.e(TAG, "The NearIT SDK was not instantiated correctly");
        }
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
