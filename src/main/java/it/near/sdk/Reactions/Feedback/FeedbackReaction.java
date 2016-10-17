package it.near.sdk.Reactions.Feedback;

import android.content.Context;
import android.net.Uri;
import android.os.Parcelable;

import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.auth.AuthenticationException;
import it.near.sdk.Communication.Constants;
import it.near.sdk.Reactions.CoreReaction;
import it.near.sdk.Recipes.Models.ReactionBundle;
import it.near.sdk.Recipes.Models.Recipe;
import it.near.sdk.Recipes.NearNotifier;
import it.near.sdk.Utils.NearUtils;
import it.near.sdk.Utils.ULog;

/**
 * Created by cattaneostefano on 11/10/2016.
 */

public class FeedbackReaction extends CoreReaction {

    public static final String PREFS_SUFFIX = "NearFeedbackNot";
    private static final String PLUGIN_NAME = "feedbacks";
    private static final String ASK_FEEDBACK_ACTION_NAME = "ask_feedback";
    public static final String FEEDBACKS_NOTIFICATION_RESOURCE =  "feedbacks";
    private static final String TAG = "FeedbackReaction";
    public static final String ANSWERS_RESOURCE = "answers";

    private List<Feedback> feedbackList;



    public FeedbackReaction(Context mContext, NearNotifier nearNotifier) {
        super(mContext, nearNotifier);
    }

    @Override
    public void refreshConfig() {
        Uri url = Uri.parse(Constants.API.PLUGINS_ROOT).buildUpon()
                .appendPath(PLUGIN_NAME)
                .appendPath(FEEDBACKS_NOTIFICATION_RESOURCE).build();

        try {
            httpClient.nearGet(mContext, url.toString(), new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    ULog.d(TAG, response.toString());
                    feedbackList = NearUtils.parseList(morpheus, response, Feedback.class);
                    persistList(TAG, feedbackList);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    ULog.d(TAG, "Error: " + statusCode);
                    try {
                        feedbackList = loadList();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (AuthenticationException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<Feedback> loadList() throws JSONException {
        String cachedString = loadCachedString(TAG);
        return gson.fromJson(cachedString , new TypeToken<Collection<Feedback>>(){}.getType());
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    public void requestSingleReaction(String bundleId, AsyncHttpResponseHandler responseHandler){
        Uri url = Uri.parse(Constants.API.PLUGINS_ROOT).buildUpon()
                .appendPath(PLUGIN_NAME)
                .appendPath(FEEDBACKS_NOTIFICATION_RESOURCE)
                .appendPath(bundleId).build();
        try {
            httpClient.nearGet(mContext, url.toString(), responseHandler);
        } catch (AuthenticationException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void handleReaction(String reaction_action, ReactionBundle reaction_bundle, Recipe recipe) {
        switch (reaction_action){
            case ASK_FEEDBACK_ACTION_NAME:
                showContent(reaction_bundle.getId(), recipe);
                break;
        }
    }

    @Override
    public void handlePushReaction(final Recipe recipe, final String push_id, ReactionBundle reaction_bundle) {
        requestSingleReaction(reaction_bundle.getId(), new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                ULog.d(TAG, response.toString());
                Feedback feedback = NearUtils.parseElement(morpheus, response, Feedback.class);
                feedback.setRecipeId(recipe.getId());
                nearNotifier.deliverBackgroundPushReaction(feedback, recipe, push_id);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                ULog.d(TAG, "Error downloading feedback: " + statusCode);
            }
        });
    }

    @Override
    public void handleEvaluatedReaction(final Recipe recipe, String bundle_id) {
        requestSingleReaction(bundle_id, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                ULog.d(TAG, response.toString());
                Feedback feedback = NearUtils.parseElement(morpheus, response, Feedback.class);
                feedback.setRecipeId(recipe.getId());
                nearNotifier.deliverBackgroundReaction(feedback, recipe);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                ULog.d(TAG, "Error downloading feedback:" + statusCode);
            }
        });
    }

    public void sendEvent(FeedbackEvent event) {
        try {
            String answerBody = event.toJsonAPI(mContext);
            ULog.d(TAG, "Answer" + answerBody);
            Uri url = Uri.parse(Constants.API.PLUGINS_ROOT).buildUpon()
                    .appendPath(PLUGIN_NAME)
                    .appendPath(FEEDBACKS_NOTIFICATION_RESOURCE)
                    .appendPath(event.getFeedbackId())
                    .appendPath(ANSWERS_RESOURCE).build();
            try {
                httpClient.nearPost(mContext, url.toString(), answerBody, new JsonHttpResponseHandler(){
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        ULog.d(TAG, "Feedback sent successfully");
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        ULog.d(TAG, "Error in sending answer: " + statusCode);
                    }
                });
            } catch (AuthenticationException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }


        } catch (JSONException e) {
            e.printStackTrace();
            ULog.d(TAG, "Error: incorrect format " + e.toString());
        }

    }

    @Override
    protected Parcelable getContent(String reaction_bundle, Recipe recipe) {
        if (feedbackList == null) return null;
        for ( Feedback fb : feedbackList){
            if (fb.getId().equals(reaction_bundle)){
                fb.setRecipeId(recipe.getId());
                return fb;
            }
        }
        return null;
    }

    @Override
    public String getPrefSuffix() {
        return PREFS_SUFFIX;
    }

    @Override
    protected HashMap<String, Class> getModelHashMap() {
        HashMap<String, Class> map = new HashMap<>();
        map.put(FEEDBACKS_NOTIFICATION_RESOURCE, Feedback.class);
        return map;
    }

    @Override
    protected String getResTypeName() {
        return FEEDBACKS_NOTIFICATION_RESOURCE;
    }

    @Override
    public void buildActions() {
        supportedActions = new ArrayList<String>();
        supportedActions.add(ASK_FEEDBACK_ACTION_NAME);
    }
}
