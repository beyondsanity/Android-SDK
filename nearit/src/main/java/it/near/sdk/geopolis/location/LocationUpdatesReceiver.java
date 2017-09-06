package it.near.sdk.geopolis.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.google.android.gms.location.LocationResult;

import java.util.List;

import it.near.sdk.logging.NearLog;

public class LocationUpdatesReceiver extends BroadcastReceiver {
    private static final String TAG = "LocationUpdatesReceiver";
    public static final String ACTION_PROCESS_UPDATES = "empty_action";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_PROCESS_UPDATES.equals(action)) {
                LocationResult result = LocationResult.extractResult(intent);
                if (result != null) {
                    List<Location> locations = result.getLocations();
                    if (!locations.isEmpty()) {
                        Location location = locations.get(0);
                        sendNewLocationUpdate(location);
                    }
                }
            }
        }
        NearLog.i(TAG, "got location update");
    }

    private void sendNewLocationUpdate(Location location) {
        // TODO send intent to geopolis manager to communicate a new location update
    }
}
