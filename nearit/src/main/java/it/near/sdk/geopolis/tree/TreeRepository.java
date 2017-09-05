package it.near.sdk.geopolis.tree;

import android.content.Context;
import android.content.SharedPreferences;

import it.near.sdk.utils.CurrentTime;
import it.near.sdk.utils.timestamp.NearTimestampChecker;

public class TreeRepository {

    private static final String NEAR_TREE_REPO_PREFS_NAME = "NearITTreeSP";
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
    }

    private void setCacheTimestamp(long timestamp) {
        sp.edit().putLong(TIMESTAMP, timestamp).commit();
    }

    private long getCacheTimestamp() {
        return sp.getLong(TIMESTAMP, TIMESTAMP_DEF_VALUE);
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(NEAR_TREE_REPO_PREFS_NAME, Context.MODE_PRIVATE);
    }
}
