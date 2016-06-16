package it.near.sdk.Reactions;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.near.sdk.MorpheusNear.JsonApiObject;
import it.near.sdk.MorpheusNear.Morpheus;
import it.near.sdk.MorpheusNear.Resource;
import it.near.sdk.Recipes.NearNotifier;
import it.near.sdk.Recipes.Models.Recipe;
import it.near.sdk.Utils.ULog;

/**
 * Superclass for plugins of type "reaction". Subclass to add the support of new "What" types and handle requests to fire contents,
 * from within the devices and form push notifications.
 * @author cattaneostefano
 */
public abstract class Reaction {
    public List<String> supportedActions = null;
    /**
     * App context.
     */
    protected Context mContext;
    /**
     * Notifier of content to the app.
     */
    protected NearNotifier nearNotifier;

    public Reaction(Context mContext, NearNotifier nearNotifier) {
        this.mContext = mContext;
        this.nearNotifier = nearNotifier;
    }

    public List<String> getSupportedActions() {
        if (supportedActions == null){
            buildActions();
        }
        return supportedActions;
    }

    /**
     * Method called by the recipe manager to trigger a reaction.
     * @param recipe matched recipe
     */
    public void handleReaction(Recipe recipe){
        if (!getPluginName().equals(recipe.getReaction_plugin_id())){
            return;
        }
        handleReaction(recipe.getReaction_action().getId(), recipe.getReaction_bundle().getId(), recipe);
    }

    /**
     * Build supported actions
     */
    public abstract void buildActions();

    /**
     * Refresh configuration from the server. Consider caching the results so you can support offline mode.
     */
    public abstract void refreshConfig();

    /**
     * @return the profile name.
     */
    public abstract String getPluginName();

    /**
     * Handle a reaction, including the call to the NearNotifier object.
     * @param reaction_action the reaction anction of the recipe.
     * @param reaction_bundle the reaction bundle of the recipe.
     * @param recipe the entire recipe object.
     */
    protected abstract void handleReaction(String reaction_action, String reaction_bundle, Recipe recipe);

    /**
     * Handle a reaction from a push notification, including the call to the NearNotifier object. Since this will be called after the insertion
     * of a push based rrecipe, it's highly unlikely that the recipe information will be cached.
     * @param recipe the recipe object.
     * @param push_id the id of the push notification.
     * @param bundle_id the id of the reaction bundle.
     */
    public abstract void handlePushReaction(Recipe recipe, String push_id, String bundle_id);


}
