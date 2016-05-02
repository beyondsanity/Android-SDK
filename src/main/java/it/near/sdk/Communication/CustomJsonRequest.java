package it.near.sdk.Communication;

import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import it.near.sdk.GlobalConfig;
import it.near.sdk.GlobalState;
import it.near.sdk.R;

/**
 * Creates a JsonObjectRequest (from the volley library) subclass with the proper headers.
 * Also includes appending of the relative path to the host.
 * @author cattaneostefano
 */
public class CustomJsonRequest extends JsonObjectRequest {
    private Context mContext;

    public CustomJsonRequest(Context context, int method, String url, String requestBody, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        super(method, composeUrl(context, url), requestBody, listener, errorListener);
        this.mContext = context;
    }



    public CustomJsonRequest(Context context, String url, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        super(composeUrl(context, url), listener, errorListener);
        this.mContext = context;
    }

    public CustomJsonRequest(Context context, int method, String url, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        super(method, composeUrl(context, url), listener, errorListener);
        this.mContext = context;
    }

    public CustomJsonRequest(Context context, int method, String url, JSONObject jsonRequest, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        super(method, composeUrl(context, url), jsonRequest, listener, errorListener);
        this.mContext = context;
    }

    public CustomJsonRequest(Context context, String url, JSONObject jsonRequest, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        super(composeUrl(context, url), jsonRequest, listener, errorListener);
        this.mContext = context;
    }

    /**
     * Build header for every call
     * @return
     * @throws AuthFailureError
     */
    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map headers = new HashMap();
        headers.put(Constants.Headers.accessToken, "bearer " + GlobalConfig.getInstance(mContext).getApiKey());
        headers.put(Constants.Headers.contentType, Constants.Headers.jsonApiHeader);
        headers.put(Constants.Headers.accept, Constants.Headers.jsonApiHeader);
        return headers;
    }

    /**
     * Appends a path to the host
     * @param context
     * @param url
     * @return
     */
    private static String composeUrl(Context context, String url) {
        String baseUrl = context.getResources().getString(R.string.API_BASE_URL);
        return baseUrl + url;
    }

}
