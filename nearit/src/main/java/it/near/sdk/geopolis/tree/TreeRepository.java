package it.near.sdk.geopolis.tree;

import android.content.Context;
import android.content.SharedPreferences;

import it.near.sdk.logging.NearLog;
import it.near.sdk.utils.CurrentTime;
import it.near.sdk.utils.timestamp.NearTimestampChecker;

public class TreeRepository {

    private static final String NEAR_TREE_REPO_PREFS_NAME = "NearITTreeSP";
    private static final String TAG = "TreeRepository";
    private static final String MIN_DISTANCE = "MinDistance";
    private static final long DEFAULT_MIN_DISTANCE = 0L;
    private final NearTimestampChecker nearTimestampChecker;
    // TODO field for some local cache
    // TODO dependency for disk "cache"
    private final TreeApi treeApi;
    private final CurrentTime currentTime;
    static final String TIMESTAMP = "timestamp";
    static final long TIMESTAMP_DEF_VALUE = 0L;
    private final SharedPreferences sp;

    public TreeRepository(NearTimestampChecker nearTimestampChecker,
                          TreeApi treeApi,
                          CurrentTime currentTime,
                          SharedPreferences sp) {
        this.nearTimestampChecker = nearTimestampChecker;
        this.treeApi = treeApi;
        this.currentTime = currentTime;
        this.sp = sp;

        // TODO load from disk to in-memory cache
    }

    // TODO method to access in-memory tree data

    void syncTree(/* a listener*/) {
        long timestamp = getCacheTimestamp();
        if (timestamp == TIMESTAMP_DEF_VALUE) {
            refreshTree(/*the listener*/);
            return;
        }
        nearTimestampChecker.checkGeopolisTimeStamp(timestamp, new NearTimestampChecker.SyncCheckListener() {
            @Override
            public void syncNeeded() {
                refreshTree(/*the listener*/);
            }

            @Override
            public void syncNotNeeded() {
                NearLog.i(TAG, "Local tree was up to date");
                // call listener
            }
        });
    }

    void refreshTree(/*a listener*/) {
        treeApi.refreshTreeCofing(new TreeRequestListener() {
            // TODO callbacks
            // on success: update cache and timestamp, call listener. Save min distance value.
            // on failure: return cache.
        });
    }

    private void setCacheTimestamp(long timestamp) {
        sp.edit().putLong(TIMESTAMP, timestamp).commit();
    }

    private long getCacheTimestamp() {
        return sp.getLong(TIMESTAMP, TIMESTAMP_DEF_VALUE);
    }

    private void setMinDistanceToUpdate(long minDistance) {
        sp.edit().putLong(MIN_DISTANCE, minDistance).commit();
    }

    public long getMinDistanceToUpdate() {
        return sp.getLong(MIN_DISTANCE, DEFAULT_MIN_DISTANCE);
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(NEAR_TREE_REPO_PREFS_NAME, Context.MODE_PRIVATE);
    }
}
