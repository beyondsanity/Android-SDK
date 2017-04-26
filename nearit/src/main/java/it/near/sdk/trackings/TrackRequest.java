package it.near.sdk.trackings;

import org.json.JSONException;
import org.json.JSONObject;


public class TrackRequest {

    static final String KEY_URL = "url";
    static final String KEY_BODY = "body";
    final String url;
    final String body;
    boolean sending;

    public TrackRequest(String url, String body, boolean sending) {
        this.url = url;
        this.body = body;
        this.sending = sending;
    }

    public JSONObject getJsonObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(KEY_URL, this.url);
            jsonObject.put(KEY_BODY, this.body);
        } catch (JSONException ignored) {
        }
        return jsonObject;
    }

    public static TrackRequest fromJsonObject(JSONObject json) throws JSONException {
        String url = json.getString(KEY_URL);
        String body = json.getString(KEY_BODY);
        return new TrackRequest(url, body, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrackRequest that = (TrackRequest) o;

        if (url != null ? !url.equals(that.url) : that.url != null) return false;
        return body != null ? body.equals(that.body) : that.body == null;

    }
}
