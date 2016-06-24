package it.near.sdk.Recipes;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.auth.AuthenticationException;
import it.near.sdk.Communication.Constants;
import it.near.sdk.Communication.CustomJsonRequest;
import it.near.sdk.Communication.NearAsyncHttpClient;
import it.near.sdk.GlobalConfig;
import it.near.sdk.GlobalState;
import it.near.sdk.MorpheusNear.Morpheus;
import it.near.sdk.Reactions.Reaction;
import it.near.sdk.Recipes.Models.OperationAction;
import it.near.sdk.Recipes.Models.PulseAction;
import it.near.sdk.Recipes.Models.PulseBundle;
import it.near.sdk.Recipes.Models.ReactionAction;
import it.near.sdk.Recipes.Models.ReactionBundle;
import it.near.sdk.Recipes.Models.Recipe;
import it.near.sdk.Utils.NearUtils;
import it.near.sdk.Utils.ULog;

/**
 * Menage recipes download, caching and direct calling.
 *
 * @author cattaneostefano
 */
public class RecipesManager {
    private static final String TAG = "RecipesManager";
    public static final String PREFS_SUFFIX = "NearRecipes";
    private static final String PROCESS_PATH = "process";
    public final String PREFS_NAME;
    private final SharedPreferences sp;
    private Context mContext;
    private Morpheus morpheus;
    private List<Recipe> recipes = new ArrayList<>();
    private HashMap<String, Reaction> reactions = new HashMap<>();
    SharedPreferences.Editor editor;
    private NearAsyncHttpClient httpClient;

    public RecipesManager(Context context) {
        this.mContext = context;

        String PACK_NAME = mContext.getApplicationContext().getPackageName();
        PREFS_NAME = PACK_NAME + PREFS_SUFFIX;
        sp = mContext.getSharedPreferences(PREFS_NAME, 0);
        editor = sp.edit();
        httpClient = new NearAsyncHttpClient();
        try {
            loadChachedList();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        setUpMorpheusParser();
        refreshConfig();
    }

    /**
     * Set up Morpheus parser. Morpheus parses jsonApi encoded resources
     * https://github.com/xamoom/Morpheus
     * We didn't actually use this library due to its minSdkVersion. We instead imported its code and adapted it.
     */
    private void setUpMorpheusParser() {
        morpheus = new Morpheus();
        // register your resources
        morpheus.getFactory().getDeserializer().registerResourceClass("recipes", Recipe.class);
        morpheus.getFactory().getDeserializer().registerResourceClass("pulse_actions", PulseAction.class);
        morpheus.getFactory().getDeserializer().registerResourceClass("operation_actions", OperationAction.class);
        morpheus.getFactory().getDeserializer().registerResourceClass("reaction_actions", ReactionAction.class);
        morpheus.getFactory().getDeserializer().registerResourceClass("pulse_bundles", PulseBundle.class);
        morpheus.getFactory().getDeserializer().registerResourceClass("reaction_bundles", ReactionBundle.class);
    }

    public void addReaction(String plugin, Reaction reaction){
        reactions.put(plugin, reaction);
    }

    /**
     * return the list of recipes
     * @return the list of recipes
     */
    public List<Recipe> getRecipes() {
        return recipes;
    }

    /**
     * Tries to refresh the recipes list. If some network problem occurs, a cached version will be used.
     */
    public void refreshConfig(){
        Uri url = Uri.parse(Constants.API.RECIPES_PATH).buildUpon()
                .appendPath(PROCESS_PATH).build();
        HashMap<String, Object> map = new HashMap<>();
        map.put("app_id", GlobalConfig.getInstance(mContext).getAppId());
        map.put("installation_id", GlobalConfig.getInstance(mContext).getInstallationId());
        JSONObject congregoObj = new JSONObject();
        try {
            JSONObject evaluateObj = new JSONObject();
            evaluateObj.put("profile_id", GlobalConfig.getInstance(mContext).getProfileId());
            congregoObj.put("evaluate_segment", evaluateObj);
        } catch (JSONException e) {
            e.printStackTrace();
            ULog.d(TAG, "profileId not present");
        }
        map.put("congrego", congregoObj);
        String requestBody = null;
        try {
            requestBody = NearUtils.toJsonAPI("evaluates", map);
        } catch (JSONException e) {
            e.printStackTrace();
            ULog.d(TAG, "Can't build request body");
        }

        try {
            httpClient.nearPost(mContext, url.toString(), requestBody, new JsonHttpResponseHandler(){
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    ULog.d(TAG, "Got recipes: " + response.toString());
                    recipes = NearUtils.parseList(morpheus, response, Recipe.class);
                    persistList(recipes);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    ULog.d(TAG, "Error in downloading recipes: " + statusCode);
                    try {
                        recipes = loadChachedList();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (AuthenticationException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
/*
        GlobalState.getInstance(mContext).getRequestQueue().add(
                new CustomJsonRequest(mContext, Request.Method.POST, url.toString(), requestBody,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                ULog.d(TAG, "Got recipes: " + response.toString());
                                recipes = NearUtils.parseList(morpheus, response, Recipe.class);
                                persistList(recipes);
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                ULog.d(TAG, "Error in downloading recipes: " + error.toString());
                                try {
                                    recipes = loadChachedList();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        })
        );
*/

    }

    private void persistList(List<Recipe> recipes) {
        Gson gson = new Gson();
        String listStringified = gson.toJson(recipes);
        ULog.d(TAG , "Persist: " + listStringified);
        editor.putString(TAG , listStringified);
        editor.apply();
    }

    private List<Recipe> loadChachedList() throws JSONException {
        Gson gson = new Gson();
        Type collectionType = new TypeToken<Collection<Recipe>>(){}.getType();
        ArrayList<Recipe> recipes = gson.fromJson(sp.getString(TAG, ""), collectionType);
        return recipes;
    }

    /**
     * Tries to trigger a recipe, stating the plugin, action and bundle of the pulse.
     * If nothing matches, nothing happens.
     *
     * @param pulse_plugin the plugin of the pulse.
     * @param pulse_action the action of the pulse.
     * @param pulse_bundle the bundle of the pulse.
     */
    public void gotPulse(String pulse_plugin, String pulse_action, String pulse_bundle){
        List<Recipe> matchingRecipes = new ArrayList<>();
        if (recipes == null) return;
        for (Recipe recipe : recipes){
             if ( recipe.getPulse_plugin_id().equals(pulse_plugin) &&
                  recipe.getPulse_action().getId().equals(pulse_action) &&
                  recipe.getPulse_bundle().getId().equals(pulse_bundle) ) {
                 matchingRecipes.add(recipe);
             }
        }
        if (matchingRecipes.isEmpty()){return;}
        Recipe winnerRecipe = matchingRecipes.get(0);
        gotRecipe(winnerRecipe);
    }

    /**
     * Tries to trigger a recipe. If no reaction plugin can handle the recipe, nothing happens.
     *
     * @param recipe the recipe to trigger.
     */
    public void gotRecipe(Recipe recipe){
        String stringRecipe = recipe.getName();
        ULog.d(TAG , stringRecipe!=null? stringRecipe : "nameless recipe");
        Reaction reaction = reactions.get(recipe.getReaction_plugin_id());
        reaction.handleReaction(recipe);
    }

    /**
     * Process a recipe from it's id. Typically called for processing a push recipe.
     * @param id push id.
     * @return true if the recipe was found, false otherwise.
     */
    public boolean processRecipe(final String id) {
        // TODO use new evaluation endpoint for single recipes
        Uri url = Uri.parse(Constants.API.RECIPES_PATH).buildUpon()
                .appendEncodedPath(id)
                .build();

        try {
            httpClient.nearGet(mContext, url.toString(), new JsonHttpResponseHandler(){
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    ULog.d(TAG, response.toString());
                    Recipe recipe = NearUtils.parseElement(morpheus, response, Recipe.class);
                    ULog.d(TAG, recipe.toString());
                    String reactionPluginName = recipe.getReaction_plugin_id();
                    Reaction reaction = reactions.get(reactionPluginName);
                    reaction.handlePushReaction(recipe, id, recipe.getReaction_bundle().getId());
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    ULog.d(TAG, "single recipe failed");
                }
            });
        } catch (AuthenticationException e) {
            e.printStackTrace();
        }

        // inside receiver, parse the response to know what reaction plugin to use
        // than fire the reaction
        // if we got a network error, return false
        return true;
    }

    public boolean evaluateRecipe(String recipeId){
        if (recipeId == null) return false;
        Uri url = Uri.parse(Constants.API.RECIPES_PATH).buildUpon()
                .appendEncodedPath(recipeId)
                .appendPath(EVALUATE).build();
        String evaluateBody = null;
        try {
            evaluateBody = buildEvaluateBody();
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        try {
            httpClient.nearPost(mContext, url.toString(), evaluateBody, new JsonHttpResponseHandler(){
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    ULog.d(TAG, response.toString());
                    Recipe recipe = NearUtils.parseElement(morpheus, response, Recipe.class);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    super.onFailure(statusCode, headers, responseString, throwable);
                }
            });
        } catch (AuthenticationException | UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }

        return false;
    }

    private String buildEvaluateBody() throws JSONException {
        if (GlobalConfig.getInstance(mContext).getProfileId() == null ||
                GlobalConfig.getInstance(mContext).getInstallationId() == null ||
                GlobalConfig.getInstance(mContext).getAppId() == null){
            throw new JSONException("missing data");
        }
        HashMap<String, Object> coreAttributes = new HashMap<>();
        coreAttributes.put("profile_id", GlobalConfig.getInstance(mContext).getProfileId());
        coreAttributes.put("installation_id", GlobalConfig.getInstance(mContext).getInstallationId());
        coreAttributes.put("app_id", GlobalConfig.getInstance(mContext).getAppId());
        HashMap<String, Object> attributes = new HashMap<>();
        attributes.put("core" , coreAttributes);
        String body = NearUtils.toJsonAPI("evaluation", attributes);
        return null;
    }
}
