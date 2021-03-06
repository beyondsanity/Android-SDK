package it.near.sdk.geopolis.beacons.ranging;

import android.content.Context;
import android.content.Intent;


import java.util.ArrayList;
import java.util.HashMap;

import it.near.sdk.geopolis.beacons.BeaconNode;
import it.near.sdk.geopolis.GeopolisManager;
import it.near.sdk.logging.NearLog;

/**
 * @author cattaneostefano
 */
public class BeaconDynamicData {

    public static final int UNDETERMINED = 0;
    public static final int IMMEDIATE = 1;
    public static final int NEAR = 2;
    public static final int FAR = 3;

    private static final String TAG = "BeaconDynamicData";
    private final Context mContext;

    private ArrayList<Integer> proximityValues;
    private double average;
    private BeaconNode beaconNode;

    private int currentProximity = 0;

    public int getCurrentProximity() {
        return currentProximity;
    }

    public void setCurrentProximity(int newProximity) {
        if (currentProximity == UNDETERMINED || newProximity != currentProximity) {
            notifiyEvent(getBeaconNode(), newProximity);
        }
        currentProximity = newProximity;
    }

    private void notifiyEvent(BeaconNode beaconNode, int newProximity) {
        NearLog.d(TAG, "Beacon event: " + newProximity + " on beacon: " + beaconNode.identifier);
        Intent intent = new Intent();
        String packageName = mContext.getPackageName();
        intent.setAction(packageName + "." + getActionFrom(newProximity));
        intent.putExtra(GeopolisManager.NODE_ID, beaconNode.getId());
        mContext.sendBroadcast(intent);
    }


    public BeaconDynamicData(Context context) {
        mContext = context;
        proximityValues = new ArrayList<>();
    }

    public double getAverage() {
        return average;
    }

    public void setAverage(double average) {
        this.average = average;
    }


    public BeaconNode getBeaconNode() {
        return beaconNode;
    }

    public void setBeaconNode(BeaconNode beaconNode) {
        this.beaconNode = beaconNode;
    }

    public void initializeCycleData() {
        proximityValues.add(UNDETERMINED);
        if (proximityValues.size() > 4)
            proximityValues.remove(0);
    }

    public void resetData() {
        proximityValues.clear();
        currentProximity = UNDETERMINED;
    }


    public void saveDistance(double _distance) {

        int proximity = distanceToProximity(_distance);

        if (_distance > 0) {
            if (proximityValues.size() > 0) {
                proximityValues.remove(proximityValues.size() - 1);
            }
            proximityValues.add(proximity);
        }

        computeProximity();

        setCurrentProximity(distanceToProximity(_distance));
    }

    private void computeProximity() {
        /*int numberOfValid = 0;
        for (Integer proximityValue : proximityValues) {
            if (proximityValue != UNDETERMINED)
                numberOfValid++;
        }

        if (numberOfValid != 0) {
            HashMap<Integer, Integer> scoreboard = buildScoreboard(proximityValues);
            if (scoreboard.get(FAR) >= 3){
                setCurrentProximity(FAR);
            } else if (scoreboard.get(NEAR) >= 3){
                setCurrentProximity(NEAR);
            } else if (scoreboard.get(IMMEDIATE) >= 3){
                setCurrentProximity(IMMEDIATE);
            }
        }*/

    }

    private HashMap<Integer, Integer> buildScoreboard(ArrayList<Integer> proximityValues) {
        HashMap<Integer, Integer> scoreboard = new HashMap<>();
        scoreboard.put(FAR, 0);
        scoreboard.put(NEAR, 0);
        scoreboard.put(IMMEDIATE, 0);
        for (int dist : proximityValues) {
            if (dist != UNDETERMINED) {
                scoreboard.put(dist, scoreboard.get(dist) + 1);
            }
        }
        return scoreboard;
    }

    public boolean hasMinumumData() {
        int numberOfValid = 0;
        for (Integer proximityValue : proximityValues) {
            if (proximityValue != UNDETERMINED)
                numberOfValid++;
        }

        return numberOfValid >= 2;
    }


    public static int distanceToProximity(double distance) {
        if (distance <= 0)
            // negative distance, FAR
            return UNDETERMINED;

        else if (distance <= 0.16)
            // IMMEDIATE
            return IMMEDIATE;

        else if (distance <= 1.5)
            // NEAR
            return NEAR;

        else if (distance > 1.5)
            // FAR
            return FAR;

        return UNDETERMINED;
    }

    private String getActionFrom(int newProximity) {
        switch (newProximity) {
            case FAR:
                return GeopolisManager.GF_RANGE_FAR_SUFFIX;
            case NEAR:
                return GeopolisManager.GF_RANGE_NEAR_SUFFIX;
            case IMMEDIATE:
                return GeopolisManager.GF_RANGE_IMMEDIATE_SUFFIX;
        }
        return null;
    }
}
