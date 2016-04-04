package it.near.sdk.Beacons;

import android.app.Application;
import android.content.Context;

import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.startup.BootstrapNotifier;

import it.near.sdk.Utils.ULog;

/**
 * Created by cattaneostefano on 04/04/16.
 */
public class NearMonitorNotifier implements MonitorNotifier, BootstrapNotifier {
    private static final String TAG = "NearMonitorNotifier";
    private final Context mContext;
    private RegionListener regionListener;

    public NearMonitorNotifier(Context context, RegionListener regionListener) {
        this.mContext = context;
        this.regionListener = regionListener;
    }

    @Override
    public void didEnterRegion(Region region) {
        ULog.d(TAG , "didEnterRegion: " + region.toString());

    }

    @Override
    public void didExitRegion(Region region) {
        ULog.d(TAG , "didExitRegion: " + region.toString());
    }

    @Override
    public void didDetermineStateForRegion(int i, Region region) {
        ULog.d(TAG , "didDetermineStateForRegion: " + region.toString());
    }

    @Override
    public Context getApplicationContext() {
        return mContext;
    }
}