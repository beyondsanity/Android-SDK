package it.near.sdk.Beacons;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.RemoteException;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.startup.RegionBootstrap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import it.near.sdk.GlobalState;
import it.near.sdk.Models.Configuration;
import it.near.sdk.Models.Matching;
import it.near.sdk.Models.NearBeacon;
import it.near.sdk.Utils.ULog;

/**
 * Wrapper around AltBeacon.
 *
 * Created by cattaneostefano on 14/03/16.
 */
public class AltBeaconWrapper implements BeaconConsumer {

    private static final String TAG = "AltBeaconWrapper";
    private BeaconManager beaconManager;
    private Context mContext;
    private RegionBootstrap regionBootstrap;
    private ArrayList<Region> testRegions;

    public AltBeaconWrapper(Context context) {
        ULog.d(TAG , "Constructor called");
        mContext = context;
        beaconManager = BeaconManager.getInstanceForApplication(mContext);
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        GlobalState.getInstance(context).setNearRangeNotifier(new NearRangeNotifier(context));
        GlobalState.getInstance(context).setNearMonitorNotifier(new NearMonitorNotifier(context, regionListener));
        //beaconManager.bind(this);
        beaconManager.setBackgroundMode(true);

        craftTestRegions(context);
        /*beaconManager.setBackgroundBetweenScanPeriod(40000);
        beaconManager.setBackgroundScanPeriod(1500);*/
        Region region1 = new Region("Region", Identifier.parse("8492E75F-4FD6-469D-B132-043FE94921D8"), Identifier.fromInt(1197), null);
        Region region2 = new Region("Region", Identifier.parse("B9407F30-F5F8-466E-AFF9-25556B57FE6D"), Identifier.fromInt(452), null);

        ArrayList<Region> regions = new ArrayList<>();
        regions.add(region1);
        regions.add(region2);
        regions.addAll(testRegions);
        regionBootstrap = new RegionBootstrap(GlobalState.getInstance(mContext).getNearMonitorNotifier(), regions);
    }



    public void startRanging() {
        ULog.d(TAG , "startRanging");
        // todo reenable ranging
        // beaconManager.setBackgroundMode(false);

        beaconManager.setRangeNotifier(GlobalState.getInstance(mContext).getNearRangeNotifier());
        // beaconManager.setMonitorNotifier(GlobalState.getInstance(mContext).getNearMonitorNotifier());
    }

    public void stopRangingAll(){
        ULog.d(TAG, "stopRangingAll");
        resetRanging();
        // beaconManager.unbind(this);
        beaconManager.setRangeNotifier(null);
        beaconManager.setBackgroundMode(true);
    }

    private void resetRanging(){
        ULog.d(TAG, "resetRanging");
        Collection<Region> regionList = beaconManager.getRangedRegions();
        for (Region r : regionList){
            try {
                beaconManager.stopRangingBeaconsInRegion(r);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void resetMonitoring() {
        ULog.d(TAG , "resetMonitoring");
        Collection<Region> regionList = beaconManager.getMonitoredRegions();
        for (Region region : regionList){
            try {
                beaconManager.stopMonitoringBeaconsInRegion(region);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onBeaconServiceConnect() {
        ULog.d(TAG, "onBeaconServiceConnect, startRanging");

        // after we connected with the beacon service, we use the configuration (if we have it yet) to configure our radar
        configureScanner(GlobalState.getInstance(getApplicationContext()).getConfiguration());
    }

    @Override
    public Context getApplicationContext() {
        return mContext;
    }

    @Override
    public void unbindService(ServiceConnection serviceConnection) {
        mContext.unbindService(serviceConnection);
    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        return mContext.bindService(intent, serviceConnection, i);
    }


    /**
     * Reset configuration.
     * Stop current ranging and range new configuration beacons.
     *
     * @param configuration
     */
    public void configureScanner(Configuration configuration){
        ULog.d(TAG , "configureScanner");
        resetRanging();
        // resetMonitoring();
        List<NearBeacon> beaconList = configuration.getBeaconList();

        BeaconDynamicRadar radar = new BeaconDynamicRadar(this.getApplicationContext(), beaconList, proximityListener);
        GlobalState.getInstance(this.getApplicationContext()).setBeaconDynamicRadar(radar);

        if ( beaconList == null  ||  beaconList.size() == 0 ) return;

        try {
            // Since every beacon can have completely different identifiers, we don't range for specific regions, we range all beacons
            // when we will have actual regions we will range regions
            Region region1 = new Region("Region", Identifier.parse("8492E75F-4FD6-469D-B132-043FE94921D8"), Identifier.fromInt(1197), null);
            Region region2 = new Region("Region", Identifier.parse("B9407F30-F5F8-466E-AFF9-25556B57FE6D"), Identifier.fromInt(452), null);

            ArrayList<Region> regions = new ArrayList<>();
            regions.add(region1);
            regions.add(region2);
            regions.addAll(testRegions);
            regionBootstrap = new RegionBootstrap(GlobalState.getInstance(mContext).getNearMonitorNotifier(), regions);
            // beaconManager.startMonitoringBeaconsInRegion(new Region("Region", Identifier.parse("B9407F30-F5F8-466E-AFF9-25556B57FE6D"), Identifier.fromInt(451), null));
            // beaconManager.startMonitoringBeaconsInRegion(new Region("Region", Identifier.parse("B9407F30-F5F8-466E-AFF9-25556B57FE6D"), Identifier.fromInt(452), null));
            beaconManager.startRangingBeaconsInRegion(new Region("Region", Identifier.parse("B9407F30-F5F8-466E-AFF9-25556B57FE6D"), Identifier.fromInt(452), null));
            //beaconManager.startRangingBeaconsInRegion(new Region("Region" + b.getMinor() + b.getMajor(), Identifier.parse(b.getProximity_uuid()), Identifier.fromInt(b.getMajor()), Identifier.fromInt(b.getMinor())));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }




    public void setBackgroundMode(boolean isInBackground){
        beaconManager.setBackgroundMode(isInBackground);
    }

    private GlobalState getGlobalState(){
        return GlobalState.getInstance(getApplicationContext());
    }

    private void trace(String trace){
        getGlobalState().getTraceNotifier().trace(trace);
    }


    private void craftTestRegions(Context context) {
        testRegions = new ArrayList<>();
        testRegions = TestRegionCrafter.getTestRegions(context);
    }


    private ProximityListener proximityListener = new ProximityListener() {
        @Override
        public void enterBeaconRange(NearBeacon beacon) {
            Matching matching = getGlobalState().getConfiguration().getMatchingFromBeacon(beacon);
            getGlobalState().getNearNotifier().onRuleFullfilled(matching);
        }

        @Override
        public void exitBeaconRange(NearBeacon beacon) {
        }
    };

    private RegionListener regionListener = new RegionListener() {
        @Override
        public void enterRegion(Region region) {
            getGlobalState().getNearNotifier().onEnterRegion(region);
        }

        @Override
        public void exitRegion(Region region) {
            getGlobalState().getNearNotifier().onExitRegion(region);
        }
    };


}
