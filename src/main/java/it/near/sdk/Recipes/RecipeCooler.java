package it.near.sdk.Recipes;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import it.near.sdk.Recipes.Models.Recipe;

import static it.near.sdk.Utils.NearUtils.checkNotNull;

/**
 * Created by cattaneostefano on 13/02/2017.
 */

public class RecipeCooler {

    private final SharedPreferences mSharedPreferences;

    public static final String NEAR_RECIPECOOLER_PREFSNAME = "NearRecipeCoolerPrefsName";
    public static final String LOG_MAP = "LOG_MAP";
    public static final String LATEST_LOG = "LATEST_LOG";

    public static final String GLOBAL_COOLDOWN = "global_cooldown";
    public static final String SELF_COOLDOWN = "self_cooldown";

    private Map<String, Long> mRecipeLogMap;
    private Long mLatestLogEntry;

    public RecipeCooler(@NonNull SharedPreferences sharedPreferences) {
        mSharedPreferences = checkNotNull(sharedPreferences);;
    }

    /**
     * Register the recipe as shown for future cooldown evaluation.
     * @param recipeId the recipe identifier.
     */
    public void markRecipeAsShown(String recipeId){
        long timeStamp = System.currentTimeMillis();
        getRecipeLogMap().put(recipeId, timeStamp);
        saveMap(mRecipeLogMap);
        saveLatestEntry(System.currentTimeMillis());
    }

    /**
     * Filters a recipe list against the log of recipes that have been marked as shown and its cooldown period.
     * @param recipes the recipe list to filter. This object will be modified.
     */
    public void filterRecipe(List<Recipe> recipes){
        for (Iterator<Recipe> it = recipes.iterator(); it.hasNext();) {
            Recipe recipe = it.next();
            if (!canShowRecipe(recipe)){
                it.remove();
            }
        }
    }

    /**
     * Get the latest recipe shown event timestamp.
     * @return the timestamp for the last recipe shown event.
     */
    public Long getLatestLogEntry(){
        if (mLatestLogEntry == null) {
            mLatestLogEntry = loadLatestEntry();
        }
        return mLatestLogEntry;
    }

    /**
     * Get the map of recipe shown event timestamps.
     * @return the map of timestamps.
     */
    public Map<String, Long> getRecipeLogMap() {
        if (mRecipeLogMap == null){
            mRecipeLogMap = loadMap();
        }
        return mRecipeLogMap;
    }

    private boolean canShowRecipe(Recipe recipe){
        Map <String, Object> cooldown = recipe.getCooldown();
        return cooldown == null ||
                ( globalCooldownCheck(cooldown) && selfCooldownCheck(recipe, cooldown) );
    }

    private boolean globalCooldownCheck(Map<String, Object> cooldown) {
        if (!cooldown.containsKey(GLOBAL_COOLDOWN) ||
                cooldown.get(GLOBAL_COOLDOWN) == null) return true;

        long expiredSeconds = (System.currentTimeMillis() - getLatestLogEntry()) / 1000;
        return expiredSeconds >= (Long)cooldown.get(GLOBAL_COOLDOWN);
    }

    private boolean selfCooldownCheck(Recipe recipe, Map<String, Object> cooldown){
        if (!cooldown.containsKey(SELF_COOLDOWN) ||
                cooldown.get(SELF_COOLDOWN) == null ||
                !getRecipeLogMap().containsKey(recipe.getId())) return true;

        long recipeLatestEntry = getRecipeLogMap().get(recipe.getId());
        long expiredSeconds = (System.currentTimeMillis() - recipeLatestEntry) / 1000;
        return expiredSeconds >= (Long)cooldown.get(SELF_COOLDOWN);
    }

    private void saveMap(Map<String, Long> inputMap){
        if (mSharedPreferences != null){
            JSONObject jsonObject = new JSONObject(inputMap);
            String jsonString = jsonObject.toString();
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.remove(LOG_MAP).commit();
            editor.putString(LOG_MAP, jsonString);
            editor.commit();
        }
    }

    private Map<String,Long> loadMap(){
        Map<String,Long> outputMap = new HashMap<String,Long>();
        try{
            if (mSharedPreferences != null){
                String jsonString = mSharedPreferences.getString(LOG_MAP, (new JSONObject()).toString());
                JSONObject jsonObject = new JSONObject(jsonString);
                Iterator<String> keysItr = jsonObject.keys();
                while(keysItr.hasNext()) {
                    String key = keysItr.next();
                    Long value = (Long) jsonObject.get(key);
                    outputMap.put(key, value);
                }
            }
        }catch(Exception e){
            // e.printStackTrace();
        }
        return outputMap;
    }

    private Long loadLatestEntry() {
        if (mSharedPreferences != null) {
            return mSharedPreferences.getLong(LATEST_LOG, 0L);
        }
        return 0L;
    }

    private void saveLatestEntry(long timestamp) {
        mLatestLogEntry = timestamp;
        if (mSharedPreferences!=null){
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.remove(LATEST_LOG).commit();
            editor.putLong(LATEST_LOG, timestamp);
            editor.commit();
        }
    }
}
