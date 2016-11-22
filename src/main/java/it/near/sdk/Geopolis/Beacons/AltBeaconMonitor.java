package it.near.sdk.Geopolis.Beacons;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.RemoteException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.service.RangedBeacon;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.near.sdk.Geopolis.GeopolisManager;
import it.near.sdk.Geopolis.Node;
import it.near.sdk.Geopolis.NodesManager;
import it.near.sdk.Geopolis.Beacons.Ranging.BeaconDynamicRadar;
import it.near.sdk.Utils.AppLifecycleMonitor;
import it.near.sdk.Utils.OnLifecycleEventListener;
import it.near.sdk.Utils.ULog;

/**
 * Monitor for AltBeacon regions. It sets the format of the bluetooth package and holds the background powersaver.
 *
 * @author cattaneostefano.
 */
public class AltBeaconMonitor extends OnLifecycleEventListener implements BeaconConsumer, BootstrapNotifier, RangeNotifier {

    private static final String TAG = "AltBeaconMonitor";
    private static final float DEFAULT_THRESHOLD = 0.5f;
    private static final long BACKGROUND_BETWEEN_SCAN_PERIODS = 30000;
    private static final long BACKGROUND_SCAN_PERIOD = 1500;
    private static final long FOREGROUND_SCAN_PERIOD = 1000;
    private static final long REGION_EXIT_PERIOD = 30000;

    private final BeaconManager beaconManager;
    private final NodesManager nodesManager;
    private Application mApplication;
    private RegionBootstrap regionBootstrap;
    private String prefsNameSuffix = "AltMonitor";
    private SharedPreferences sp;
    private List<Region> regions;
    private Map<Region, BeaconDynamicRadar> rangingRadars;

    public AltBeaconMonitor(Application application, NodesManager nodesManager) {
        ULog.wtf(TAG, "Altbeacon started");
        this.mApplication = application;
        this.nodesManager = nodesManager;
        this.rangingRadars = new HashMap<>();
        this.regions = new ArrayList<>();

        initAppLifecycleMonitor(application);

        beaconManager = BeaconManager.getInstanceForApplication(application.getApplicationContext());
        beaconManager.getBeaconParsers().clear();
        // set beacon layout for iBeacons
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        BeaconManager.setDebug(true);

        String PACK_NAME = application.getApplicationContext().getPackageName();
        String PREFS_NAME = PACK_NAME + prefsNameSuffix;
        sp = application.getSharedPreferences(PREFS_NAME, 0);

        addAltRegions(loadRegions());

    }


    private void startRadar() {
        beaconManager.setBackgroundBetweenScanPeriod(BACKGROUND_BETWEEN_SCAN_PERIODS);
        beaconManager.setBackgroundScanPeriod(BACKGROUND_SCAN_PERIOD);
        beaconManager.setForegroundScanPeriod(FOREGROUND_SCAN_PERIOD);
        BeaconManager.setRegionExitPeriod(REGION_EXIT_PERIOD);
        beaconManager.setBackgroundMode(true);
        if (regions != null && regions.size() > 0){
            regionBootstrap = new RegionBootstrap(this, regions);
        }
    }

    public void stopRadar(){
        if (regions != null) regions.clear();
        beaconManager.setBackgroundMode(true);
        beaconManager.removeAllMonitorNotifiers();
        beaconManager.removeAllRangeNotifiers();
        resetRanging();
        resetMonitoring();
        if (regionBootstrap != null){
            regionBootstrap.disable();
            regionBootstrap = null;
        }
    }

    /**
     * Compute the beacon region list to monitor from a list of geopolis nodes tree roots, walking down only the beacon branches.
     * @param nodes
     * @return
     */
    public static List<Region> filterBeaconRegions(List<Node> nodes) {
        List<Region> regionsToMonitor = new ArrayList<>();
        if (nodes == null) return regionsToMonitor;
        for (Node node : nodes) {
            addBranch(node, regionsToMonitor);
        }
        return regionsToMonitor;
    }

    /**
     * From a single node add all the direct beacons. This means that beacons under geofences we are not already in, won't be considered.
     * @param node the single geo node
     * @param regionsToMonitor
     */
    private static void addBranch(Node node, List<Region> regionsToMonitor) {
        // if the node is a geofence we don't consider it nor its children
        if (!BeaconNode.class.isInstance(node)) return;
        try {
            regionsToMonitor.add(BeaconNode.toAltRegion((BeaconNode) node));
        } catch (Exception e){
            //
            // e.printStackTrace();
        }

        for (Node child : node.getChildren()) {
            addBranch(child, regionsToMonitor);
        }
    }



    /**
     * Initialize app lifecycle monitor to detect the app going to the background/foreground
     * @param application
     */
    private void initAppLifecycleMonitor(Application application) {
        new AppLifecycleMonitor(application, this);
    }

    public void addRegions(List<Node> nodes){
        List<Region> regions = filterBeaconRegions(nodes);
        addAltRegions(regions);
    }

    public void addAltRegions(List<Region> regions){
        ULog.wtf(TAG, "add regions with " + regions.size());
        for (Region region : regions) {
            this.regions.add(region);
            addRegion(region);
        }
        persistRegions(regions);
        if (regionBootstrap == null && regions.size() > 0) {
            resetRanging();
            resetMonitoring();
            startRadar();
        }
    }

    private void persistRegions(List<Region> regions) {
        String regionString = new Gson().toJson(regions);
        sp.edit().putString("regions", regionString).commit();
    }

    private List<Region> loadRegions(){
        String regions = sp.getString("regions", null);
        Type type = new TypeToken<List<Region>>(){}.getType();
        List<Region> regionsList = new Gson().fromJson(regions, type);
        return regionsList;
    }

    public void addRegion(Region region){
        if (regionBootstrap!=null) regionBootstrap.addRegion(region);
    }

    public void removeRegions(List<Node> nodes){
        List<Region> regions = filterBeaconRegions(nodes);
        for (Region region : regions) {
            this.regions.remove(region);
            removeRegion(region);
        }
        if (this.regions.size() == 0){
            stopRadar();
        }
    }

    public void removeRegion(Region region) {
        try {
            stopRangingRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (regionBootstrap!=null) regionBootstrap.removeRegion(region);
    }

    /**
     * Switch to ranging mode
     */
    private void startRanging() {
        ULog.wtf(TAG, "startRanging");
        RangedBeacon.setSampleExpirationMilliseconds(5000);
        beaconManager.setBackgroundMode(false);
        beaconManager.addRangeNotifier(this);
    }

    /**
     * Stop ranging mode
     */
    private void stopRanging() {
        beaconManager.setBackgroundMode(true);
        beaconManager.removeRangeNotifier(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        ULog.wtf(TAG, "onBeaconServiceConnect");
    }

    @Override
    public Context getApplicationContext() {
        return mApplication.getApplicationContext();
    }

    @Override
    public void unbindService(ServiceConnection serviceConnection) {
        mApplication.unbindService(serviceConnection);
    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        return mApplication.bindService(intent,serviceConnection,i);
    }

    private void resetRanging() {
        for (Region region : beaconManager.getRangedRegions()) {
            try {
                stopRangingRegion(region);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void resetMonitoring() {
        for (Region region : beaconManager.getMonitoredRegions()) {
            try {
                beaconManager.stopMonitoringBeaconsInRegion(region);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onForeground() {
        ULog.wtf(TAG, "onForeground");
        // When going to the foreground, if we have regions to range, start ranging

        refreshRangingList();
        startRanging();
    }

    private void refreshRangingList() {
        if (loadRegions() == null) return;
        ULog.wtf(TAG, "refreshranging list on: " + loadRegions().size());
        for (Region region : loadRegions()) {
            beaconManager.requestStateForRegion(region);
        }
    }

    @Override
    public void onBackground() {
        ULog.wtf(TAG, "onBackground");
        // Console.clear();
        // When going to the background stop ranging, in an idempotent way (we might haven't been ranging)
        stopRanging();
        for (Map.Entry<Region, BeaconDynamicRadar> regionBeaconDynamicRadarEntry : rangingRadars.entrySet()) {
            regionBeaconDynamicRadarEntry.getValue().resetData();
        }
    }

    @Override
    public void didEnterRegion(Region region) {
        String msg = "enter region: " + region.toString();
        ULog.wtf(TAG, msg);

        logRangedRegions();
        // nearit trigger
        notifiyEventOnBeaconRegion(region, GeopolisManager.BT_ENTRY_ACTION_SUFFIX);
    }

    private void notifiyEventOnBeaconRegion(Region region, String eventActionSuffix) {
        ULog.wtf(TAG, "Region event: " + eventActionSuffix + " on region: " + region.toString());
        Intent intent  = new Intent();
        String packageName = mApplication.getPackageName();
        intent.setAction(packageName + "." + eventActionSuffix);
        intent.putExtra(GeopolisManager.NODE_ID, region.getUniqueId());
        mApplication.sendBroadcast(intent);
    }

    @Override
    public void didExitRegion(Region region) {
        String msg = "exit region: " + region.toString();
        ULog.wtf(TAG, msg);

        logRangedRegions();
        notifiyEventOnBeaconRegion(region, GeopolisManager.BT_EXIT_ACTION_SUFFIX);
        if (beaconManager.getRangedRegions().size() == 0){
            // if the list of ranged regions is empty, we stop ranging
            stopRanging();
        }

    }

    @Override
    public void didDetermineStateForRegion(int i, Region region) {

        // This is called both on region entry/exit and when the the monitor gets rebooted to re-inforce in which regions we are in and out
        // so we don't want to trigger and track a recipe here, but we still handle the region ranging in this callback.
        // basically, idempotent logic lives here

        ULog.wtf(TAG, "determine state " + i + " for region: " + region.toString());

        try {
            if (i == MonitorNotifier.INSIDE){
                // region enter
                if (region.getId2() != null && region.getId3() == null){
                    startRangingRegion(region);
                }
                if (AppLifecycleMonitor.isApplicationInForeground()){
                    // switch to ranging mode only if we are in foreground
                    startRanging();
                }
            } else {
                // region exit
                stopRangingRegion(region);
                beaconManager.stopRangingBeaconsInRegion(region);
                if (beaconManager.getRangedRegions().size() == 0){
                    // if the list of ranged regions is empty, we stop ranging
                    stopRanging();
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        ULog.wtf(TAG, "regions ranged: " + beaconManager.getRangedRegions().size());
        logRangedRegions();

    }


    private void startRangingRegion(Region region) throws RemoteException {
        rangingRadars.put(region, new BeaconDynamicRadar(mApplication, rangingBeaconsFor(region)));
        beaconManager.startRangingBeaconsInRegion(region);
    }

    private List<BeaconNode> rangingBeaconsFor(Region region) {
        List<BeaconNode> nodes = new ArrayList<>();
        Node regionNode = nodesManager.nodeFromId(region.getUniqueId());
        if (regionNode == null || regionNode.getChildren() == null) return nodes;

        for (Node node : regionNode.getChildren()) {
            if (BeaconNode.isBeacon(node)){
                nodes.add((BeaconNode) node);
            }
        }
        return nodes;
    }

    private void stopRangingRegion(Region region) throws RemoteException {
        rangingRadars.remove(region);
        beaconManager.stopRangingBeaconsInRegion(region);
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> collection, Region region) {
        String msg = "For region: " + region.getUniqueId() + " found " + collection.size() + " beacons. Distance: " + (collection.iterator().hasNext() ? collection.iterator().next().getDistance() : "none");
        ULog.wtf(TAG, msg);

        BeaconDynamicRadar radar = rangingRadars.get(region);
        radar.beaconsDiscovered((List<Beacon>) collection);

    }

    private void logRangedRegions() {
        String msg1 = "regions ranged: " + beaconManager.getRangedRegions().size();
        ULog.wtf(TAG, msg1);
    }
}
