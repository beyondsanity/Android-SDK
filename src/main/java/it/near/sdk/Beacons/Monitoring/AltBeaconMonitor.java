package it.near.sdk.Beacons.Monitoring;

import android.content.Context;
import android.os.RemoteException;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import java.util.ArrayList;
import java.util.List;

import it.near.sdk.Beacons.BeaconForest.Beacon;
import it.near.sdk.Beacons.TestRegionCrafter;
import it.near.sdk.GlobalState;
import it.near.sdk.Utils.ULog;

/**
 * Created by cattaneostefano on 05/04/16.
 */
public class AltBeaconMonitor {

    private static final String TAG = "AltBeaconMonitor";
    private final BeaconManager beaconManager;
    Context mContext;
    private RegionBootstrap regionBootstrap;
    private NearRegionLogger nearRegionLogger;
    private ArrayList<Region> insideRegions;

    public AltBeaconMonitor(Context context) {
        this.mContext = context;

        ULog.d(TAG, "Constructor called");
        beaconManager = BeaconManager.getInstanceForApplication(context.getApplicationContext());
        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        BeaconManager.setDebug(true);

        insideRegions = new ArrayList<>();

        ArrayList<Region> testRegions = TestRegionCrafter.getTestRegions(mContext);

        Region emitterRegion = new Region("Region", Identifier.parse("ACFD065E-C3C0-11E3-9BBE-1A514932AC01"), Identifier.fromInt(2009), null);
        testRegions.add(emitterRegion);
//        Region kontaktRegion = new Region("Kontakt", Identifier.parse("B9407F30-F5F8-466E-AFF9-25556B57FE6D"), Identifier.fromInt(452), null);
//        testRegions.add(kontaktRegion);
        beaconManager.setBackgroundBetweenScanPeriod(10000l);
        beaconManager.setBackgroundScanPeriod(2000l);
        // regionBootstrap = new RegionBootstrap( this, testRegions);
    }

    /**
     * Start monitoring on the given regions and sets the notifier object to be notified on region enter and exit.
     * When doing this, we stop monitoring on the region we were previously monitoring and we set the given notifier
     * as the only notifier.
     * @param regions
     * @param notifier
     */
    public void startRadar(long backBetweenPeriod, long backScanPeriod ,List<Region> regions, BootstrapNotifier notifier){
        resetMonitoring();

        beaconManager.setBackgroundBetweenScanPeriod(backBetweenPeriod);
        beaconManager.setBackgroundScanPeriod(backScanPeriod);
        regionBootstrap = new RegionBootstrap(notifier, regions);
    }

    public void setLogger(NearRegionLogger logger){
        this.nearRegionLogger = logger;
    }

    private void resetMonitoring() {
        ArrayList<Region> monitoredRegions = (ArrayList<Region>) beaconManager.getMonitoredRegions();
        for (Region region : monitoredRegions){
            try {
                beaconManager.stopMonitoringBeaconsInRegion(region);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void safeLog(String msg, ArrayList<Region> insideRegions) {
        if (nearRegionLogger != null){
            nearRegionLogger.log(msg, insideRegions);
        }
    }

}
