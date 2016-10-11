package it.near.sdk.Geopolis;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;

import com.loopj.android.http.JsonHttpResponseHandler;

import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.auth.AuthenticationException;
import it.near.sdk.Geopolis.Beacons.AltBeaconMonitor;
import it.near.sdk.Geopolis.Beacons.BeaconNode;
import it.near.sdk.Geopolis.GeoFence.GeoFenceMonitor;
import it.near.sdk.Geopolis.GeoFence.GeoFenceNode;
import it.near.sdk.Communication.Constants;
import it.near.sdk.Communication.NearAsyncHttpClient;
import it.near.sdk.Communication.NearNetworkUtil;
import it.near.sdk.Geopolis.GeoFence.GeoFenceSystemEventsReceiver;
import it.near.sdk.Geopolis.Ranging.ProximityListener;
import it.near.sdk.GlobalConfig;
import it.near.sdk.MorpheusNear.Morpheus;
import it.near.sdk.Recipes.RecipesManager;
import it.near.sdk.Trackings.Events;
import it.near.sdk.Utils.NearUtils;
import it.near.sdk.Utils.ULog;

/**
 * Manages a beacon forest, the plugin for monitoring regions structured in a tree.
 * This region structure was made up to enable background monitoring of more than 20 regions on iOS.
 * In this plugin every region is specified by ProximityUUID, minor and major. It the current implementation every region is a beacon.
 * The AltBeacon altBeaconMonitor is initialized with this setting:
 * - The background period between scans is 8 seconds.
 * - The background length of a scan 1 second.
 * - The period to wait before finalizing a region exit.
 *
 * In our current plugin representation:
 * - this is a "pulse" plugin
 * - the plugin name is: beacon-forest
 * - the only supported action is: enter_region
 * - the bundle is the id of a region
 *
 * @author cattaneostefano
 */
public class GeopolisManager {

    // ---------- beacon forest ----------
    public static final String BEACON_FOREST_PATH =         "beacon-forest";
    public static final String BEACON_FOREST_TRACKINGS =    "trackings";
    public static final String BEACON_FOREST_BEACONS =      "beacons";

    private static final String TAG = "GeopolisManager";
    private static final String PREFS_SUFFIX = "GeopolisManager";
    private static final String PLUGIN_NAME = "geopolis";

    private static final String RADAR_ON = "radar_on";
    private static final String GEOPOLIS_CONFIG = "cached_config";
    public static final String GF_ENTRY_ACTION_SUFFIX = "REGION_ENTRY";
    public static final String GF_EXIT_ACTION_SUFFIX = "REGION_EXIT";
    public static final String BT_ENTRY_ACTION_SUFFIX = "BT_REGION_ENTRY";
    public static final String BT_EXIT_ACTION_SUFFIX = "BT_REGION_EXIT";
    public static final String GF_RANGE_FAR_SUFFIX = "RANGE_FAR";
    public static final String GF_RANGE_NEAR_SUFFIX = "RANGE_NEAR";
    public static final String GF_RANGE_IMMEDIATE_SUFFIX = "RANGE_IMMEDIATE";
    public static final String NODE_ID = "identifier";
    public static final String RESET_MONITOR_ACTION_SUFFIX = "RESET_SCAN";

    private final RecipesManager recipesManager;
    private final SharedPreferences sp;
    private final SharedPreferences.Editor editor;

    private List<Region> regionList;
    private Application mApplication;
    private Morpheus morpheus;
    private AltBeaconMonitor altBeaconMonitor;
    private final GeoFenceMonitor geofenceMonitor;
    private NodesManager nodesManager;

    private NearAsyncHttpClient httpClient;
    private List<ProximityListener> proximityListeners = new ArrayList<>();

    public GeopolisManager(Application application, RecipesManager recipesManager) {
        this.mApplication = application;
        this.recipesManager = recipesManager;
        this.nodesManager = new NodesManager(application);
        this.altBeaconMonitor = new AltBeaconMonitor(application, nodesManager);
        this.geofenceMonitor = new GeoFenceMonitor(application);

        registerProximityReceiver();
        registerResetReceiver();

        String PACK_NAME = mApplication.getApplicationContext().getPackageName();
        String PREFS_NAME = PACK_NAME + PREFS_SUFFIX;
        sp = mApplication.getSharedPreferences(PREFS_NAME, 0);
        editor = sp.edit();

        httpClient = new NearAsyncHttpClient();
        refreshConfig();
    }

    private void registerProximityReceiver() {
        IntentFilter regionFilter = new IntentFilter();
        String packageName = mApplication.getPackageName();
        regionFilter.addAction(packageName + "." + GF_ENTRY_ACTION_SUFFIX);
        regionFilter.addAction(packageName + "." + GF_EXIT_ACTION_SUFFIX);
        regionFilter.addAction(packageName + "." + BT_ENTRY_ACTION_SUFFIX);
        regionFilter.addAction(packageName + "." + BT_EXIT_ACTION_SUFFIX);
        regionFilter.addAction(packageName + "." + GF_RANGE_FAR_SUFFIX);
        regionFilter.addAction(packageName + "." + GF_RANGE_NEAR_SUFFIX);
        regionFilter.addAction(packageName + "." + GF_RANGE_IMMEDIATE_SUFFIX);
        mApplication.registerReceiver(regionEventsReceiver, regionFilter);
    }

    private void registerResetReceiver() {
        IntentFilter resetFilter = new IntentFilter();
        String packageName = mApplication.getPackageName();
        resetFilter.addAction(packageName + "." + RESET_MONITOR_ACTION_SUFFIX);
        mApplication.registerReceiver(resetEventReceiver, resetFilter);
    }




    /**
     * Refresh the configuration of the component. The list of beacons to altBeaconMonitor will be downloaded from the APIs.
     * If there's an error in refreshing the configuration, a cached version will be used instead.
     *
     */
    public void refreshConfig(){
        Uri url = Uri.parse(Constants.API.PLUGINS_ROOT).buildUpon()
                    .appendPath(BEACON_FOREST_PATH)
                    .appendPath(BEACON_FOREST_BEACONS).build();
        url = Uri.parse(Constants.API.PLUGINS_ROOT).buildUpon()
                .appendPath("geopolis")
                .appendPath("nodes")
                .appendQueryParameter("filter[app_id]",GlobalConfig.getInstance(mApplication).getAppId())
                .appendQueryParameter("include", "children.*.children")
                .build();
        try {
            httpClient.nearGet(mApplication, url.toString(), new JsonHttpResponseHandler(){
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    ULog.d(TAG, response.toString());

                    List<Node> nodes = nodesManager.parseAndSetNodes(response);
                    startRadarOnNodes(nodes);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    ULog.d(TAG, "Error " + statusCode);
                    // load the config
                    startRadarOnNodes(nodesManager.getNodes());
                }
            });
        } catch (AuthenticationException e) {
            e.printStackTrace();
        }

    }

    public void startRadarOnNodes(List<Node> nodes) {
        if (nodes == null) return;
        geofenceMonitor.setUpMonitor(GeoFenceMonitor.filterGeofence(nodes));
        // altBeaconMonitor.setUpMonitor(nodes);
    }

    public void startRadar(){
        if (isRadarStarted(mApplication)) return;
        setRadarState(true);
        List<Node> nodes = nodesManager.getNodes();
        // altBeaconMonitor.setUpMonitor(nodes);
        geofenceMonitor.setUpMonitor(GeoFenceMonitor.filterGeofence(nodes));
        geofenceMonitor.startGFRadar();
    }

    public void stopRadar(){
        setRadarState(false);
        altBeaconMonitor.stopRadar();
        geofenceMonitor.stopGFRadar();
    }


    /**
     * Notify the RECIPES_PATH manager of the occurance of a registered pulse.
     * @param pulseAction the action of the pulse to notify
     * @param pulseBundle the region identifier of the pulse
     */
    private void firePulse(String pulseAction, String pulseBundle) {
        ULog.d(TAG, "firePulse!");
        recipesManager.gotPulse(PLUGIN_NAME, pulseAction, pulseBundle);
    }


    BroadcastReceiver regionEventsReceiver = new BroadcastReceiver() {
        public static final String TAG = "RegionEventReceiver";
        @Override
        public void onReceive(Context context, Intent intent) {
            ULog.wtf(this.TAG, "receiverEvent");
            if (!intent.hasExtra(NODE_ID)) return;
            // trim the package name
            String packageName = mApplication.getPackageName();
            String action = intent.getAction().replace(packageName + ".", "");
            Node node = nodesManager.nodeFromId(intent.getStringExtra(NODE_ID));

            switch (action){
                case GF_ENTRY_ACTION_SUFFIX:
                    if (node == null) return;
                    trackAndFirePulse(node.getIdentifier(), Events.ENTER_PLACE);
                    if (node.getChildren() != null){
                        geofenceMonitor.setUpMonitor(GeoFenceMonitor.geofencesOnEnter(nodesManager.getNodes(), node));
                        altBeaconMonitor.addRegions(node.getChildren());
                    }
                    break;
                case GF_EXIT_ACTION_SUFFIX:
                    if (node == null) return;
                    trackAndFirePulse(node.getIdentifier(), Events.LEAVE_PLACE);
                    geofenceMonitor.setUpMonitor(GeoFenceMonitor.geofencesOnExit(nodesManager.getNodes(), node));
                    altBeaconMonitor.removeRegions(node.getChildren());

                    break;
                case BT_ENTRY_ACTION_SUFFIX:
                    if (node == null) return;
                    trackAndFirePulse(node.getIdentifier(), Events.ENTER_REGION);
                    break;
                case BT_EXIT_ACTION_SUFFIX:
                    if (node == null) return;
                    trackAndFirePulse(node.getIdentifier(), Events.LEAVE_REGION);
                    break;
                case GF_RANGE_FAR_SUFFIX:
                    trackAndFirePulse(node.getIdentifier(), Events.RANGE_FAR);
                    break;
                case GF_RANGE_NEAR_SUFFIX:
                    trackAndFirePulse(node.getIdentifier(), Events.RANGE_NEAR);
                    break;
                case GF_RANGE_IMMEDIATE_SUFFIX:
                    trackAndFirePulse(node.getIdentifier(), Events.RANGE_IMMEDIATE);
                    break;
            }
        }
    };

    /**
     * Tracks the geographical interaction and fires the proper pulse. It does nothing if the identifier is null.
     * @param identifier
     * @param event
     */
    private void trackAndFirePulse(String identifier, String event) {
        if (identifier != null){
            trackEvent(identifier, event);
            firePulse(event, identifier);
        }
    }

    private BroadcastReceiver resetEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ULog.wtf(TAG, "reset intent received");
            if (intent.getBooleanExtra(GeoFenceSystemEventsReceiver.LOCATION_STATUS, false)){
                startRadarOnNodes(nodesManager.getNodes());
            } else {
                altBeaconMonitor.stopRadar();
                geofenceMonitor.stopGFRadar();
            }

        }
    };

    /**
     * Send tracking data to the Forest beacon APIs about a region enter (every beacon is a region).
     */
    private void trackEvent(String identifier, String event) {
        try {
            Uri url = Uri.parse(Constants.API.PLUGINS_ROOT).buildUpon()
                    .appendPath(BEACON_FOREST_PATH)
                    .appendPath(BEACON_FOREST_TRACKINGS).build();
            NearNetworkUtil.sendTrack(mApplication, url.toString(), buildTrackBody(identifier, event));
        } catch (JSONException e) {
            ULog.d(TAG, "Unable to send track: " +  e.toString());
        }
    }


    /**
     * Compute the HTTP request body from the region identifier in jsonAPI format.
     * @param identifier the node identifier
     * @param event the event
     * @return the correctly formed body
     * @throws JSONException
     */
    private String buildTrackBody(String identifier, String event) throws JSONException {
        HashMap<String, Object> map = new HashMap<>();
        map.put("identifier" , identifier);
        map.put("event", event);
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        Date now = new Date(System.currentTimeMillis());
        String formatted = sdf.format(now);
        map.put("tracked_at", formatted);
        map.put("profile_id", GlobalConfig.getInstance(mApplication).getProfileId());
        map.put("installation_id", GlobalConfig.getInstance(mApplication).getInstallationId());
        return NearUtils.toJsonAPI("trackings", map);
    }



    /**
     * Returns wether the app started the location radar.
     * @param context
     * @return
     */
    public static boolean isRadarStarted(Context context){
        String PACK_NAME = context.getApplicationContext().getPackageName();
        SharedPreferences sp = context.getSharedPreferences(PACK_NAME + PREFS_SUFFIX, 0);
        return sp.getBoolean(RADAR_ON, false);
    }

    public void setRadarState(boolean b){
        String PACK_NAME = mApplication.getApplicationContext().getPackageName();
        SharedPreferences.Editor edit = mApplication.getSharedPreferences(PACK_NAME + PREFS_SUFFIX, 0).edit();
        edit.putBoolean(RADAR_ON, b).apply();

    }

}
