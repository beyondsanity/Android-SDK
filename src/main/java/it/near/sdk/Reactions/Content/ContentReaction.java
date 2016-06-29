package it.near.sdk.Reactions.Content;

import android.content.Context;
import android.net.Uri;

import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.auth.AuthenticationException;
import it.near.sdk.Communication.Constants;
import it.near.sdk.Reactions.CoreReaction;
import it.near.sdk.Recipes.NearNotifier;
import it.near.sdk.Recipes.Models.Recipe;
import it.near.sdk.Utils.NearUtils;
import it.near.sdk.Utils.ULog;

/**
 * @author cattaneostefano
 */
public class ContentReaction extends CoreReaction {
    // ---------- content notification plugin ----------
    public static final String CONTENT_NOTIFICATION_PATH =      "content-notification";
    public static final String CONTENT_NOTIFICATION_RESOURCE =  "contents";
    private static final String PLUGIN_NAME = "content-notification";
    private static final String SHOW_CONTENT_ACTION_NAME = "show_content";
    private static final String TAG = "ContentReaction";
    public static final String PREFS_SUFFIX = "NearContentNot";
    private List<Content> contentList;

    public ContentReaction(Context context, NearNotifier nearNotifier) {
        super(context, nearNotifier);
    }

    @Override
    protected String getResTypeName() {
        return "contents";
    }

    @Override
    public void handleReaction(String reaction_action, String reaction_bundle, Recipe recipe) {
        switch (reaction_action){
            case SHOW_CONTENT_ACTION_NAME:
                showContent(reaction_bundle, recipe);
                break;
        }
    }



    private void showContent(String reaction_bundle, Recipe recipe) {
        ULog.d(TAG, "Show content: " + reaction_bundle);
        Content notification = getNotification(reaction_bundle);
        if (notification == null) return;
        nearNotifier.deliverBackgroundReaction(notification, recipe);
    }

    private Content getNotification(String reaction_bundle) {
        if (contentList == null) return null;
        for ( Content cn : contentList){
            if (cn.getId().equals(reaction_bundle)){
                return cn;
            }
        }
        return null;
    }

    public void refreshConfig() {
        Uri url = Uri.parse(Constants.API.PLUGINS_ROOT).buildUpon()
                    .appendPath(CONTENT_NOTIFICATION_PATH)
                    .appendPath(CONTENT_NOTIFICATION_RESOURCE)
                    .appendQueryParameter("include", "images").build();
        try {
            httpClient.nearGet(mContext, url.toString(), new JsonHttpResponseHandler(){
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    ULog.d(TAG, response.toString());
                    contentList = NearUtils.parseList(morpheus, response, Content.class);
                    formatLinks(contentList);
                    persistList(TAG, contentList);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    ULog.d(TAG, "Error: " + statusCode);
                    try {
                        contentList = loadList();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (AuthenticationException e) {
            e.printStackTrace();
        }
/*
        GlobalState.getInstance(mContext).getRequestQueue().add(
                new CustomJsonRequest(mContext, url.toString(), new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        ULog.d(TAG, response.toString());
                        contentList = NearUtils.parseList(morpheus, response, Content.class);
                        formatLinks(contentList);
                        persistList(TAG, contentList);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        ULog.d(TAG, "Error: " + error.toString());
                        try {
                            contentList = loadList();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                })
        );
*/
    }

    @Override
    public void handlePushReaction(final Recipe recipe, final String push_id, String bundleId) {
        // TODO not tested
        requestSingleReaction(bundleId, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                ULog.d(TAG, response.toString());
                Content content = NearUtils.parseElement(morpheus, response, Content.class);
                formatLinks(content);
                nearNotifier.deliverBackgroundPushReaction(content, recipe, push_id);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                ULog.d(TAG, "Error downloading push content: " + statusCode);
            }
        });

/*
        GlobalState.getInstance(mContext).getRequestQueue().add(
                new CustomJsonRequest(mContext, url.toString(), new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        ULog.d(TAG, response.toString());
                        Content content = NearUtils.parseElement(morpheus, response, Content.class);
                        formatLinks(content);
                        nearNotifier.deliverBackgroundPushReaction(content, recipe, push_id);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        ULog.d(TAG, "Error downloading push content: " + error.toString());
                    }
                })
        );
*/
    }

    @Override
    public void handleEvaluatedReaction(final Recipe recipe, String bundleId) {
        // TODO test this method. Since recipes with a content won't need online evaluation this won't get called for now
        requestSingleReaction(bundleId, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                ULog.d(TAG, response.toString());
                Content content = NearUtils.parseElement(morpheus, response, Content.class);
                formatLinks(content);
                nearNotifier.deliverBackgroundReaction(content, recipe);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                ULog.d(TAG, "Error downloading content:" + statusCode);
            }
        });
    }

    public void requestSingleReaction(String bundleId, AsyncHttpResponseHandler responseHandler){
        Uri url = Uri.parse(Constants.API.PLUGINS_ROOT).buildUpon()
                .appendPath(CONTENT_NOTIFICATION_PATH)
                .appendPath(CONTENT_NOTIFICATION_RESOURCE)
                .appendPath(bundleId)
                .appendQueryParameter("include", "images").build();
        try {
            httpClient.nearGet(mContext, url.toString(), responseHandler);
        } catch (AuthenticationException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<Content> loadList() throws JSONException {
        String cachedString = loadCachedString(TAG);
        return gson.fromJson(cachedString , new TypeToken<Collection<Content>>(){}.getType());
    }

    private void formatLinks(List<Content> notifications){
        for (Content notification : notifications) {
            formatLinks(notification);
        }
    }

    private void formatLinks(Content notification){
        List<Image> images = notification.getImages();
        List<ImageSet> imageSets = new ArrayList<>();
        for (Image image : images) {
            ImageSet imageSet = new ImageSet();
            HashMap<String, Object> map = image.getImage();
            imageSet.setFullSize((String) map.get("url"));
            imageSet.setBigSize(((LinkedTreeMap<String, Object>)map.get("max_1920_jpg")).get("url").toString());
            imageSet.setSmallSize(((LinkedTreeMap<String, Object>)map.get("square_300")).get("url").toString());
            imageSets.add(imageSet);
        }
        notification.setImages_links(imageSets);
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public String getPrefSuffix() {
        return PREFS_SUFFIX;
    }

    @Override
    protected HashMap<String, Class> getModelHashMap() {
        HashMap<String, Class> map = new HashMap<>();
        map.put("contents", Content.class);
        map.put("images", Image.class);
        return map;
    }

    @Override
    public void buildActions() {
        supportedActions = new ArrayList<String>();
        supportedActions.add(SHOW_CONTENT_ACTION_NAME);
    }

}
