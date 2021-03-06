package it.near.sdk.utils;

import android.content.Intent;
import android.os.Parcelable;
import android.util.Base64;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Collections;

import it.near.sdk.logging.NearLog;
import it.near.sdk.reactions.contentplugin.model.Content;
import it.near.sdk.reactions.couponplugin.model.Coupon;
import it.near.sdk.reactions.customjsonplugin.model.CustomJSON;
import it.near.sdk.reactions.feedbackplugin.model.Feedback;
import it.near.sdk.reactions.simplenotificationplugin.model.SimpleNotification;
import it.near.sdk.trackings.TrackingInfo;

public class NearUtils {

    /**
     * Compute app Id from the Apptoken (apikey)
     *
     * @param apiKey token
     * @return the App Id as defined in our servers
     */
    public static String fetchAppIdFrom(String apiKey) {
        String secondSegment = substringBetween(apiKey, ".", ".");
        String appId = "";
        try {
            String decodedAK = decodeString(secondSegment);
            JSONObject jwt = new JSONObject(decodedAK);
            JSONObject account = jwt.getJSONObject("data").getJSONObject("account");
            appId = account.getString("id");
        } catch (Exception e) {
            NearLog.e("NearITErrors", "Error while processing NearIT API token. Please check if you are using the correct key.");
        }
        return appId;
    }

    /**
     * Decode base 64 string
     *
     * @param encoded encoded string
     * @return decoded string
     */
    private static String decodeString(String encoded) throws NullPointerException {
        byte[] dataDec = Base64.decode(encoded, Base64.DEFAULT);
        String decodedString = "";
        try {
            decodedString = new String(dataDec, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
        }
        return decodedString;
    }

    private static String substringBetween(String str, String open, String close) {
        if (str == null || open == null || close == null) {
            return null;
        }
        int start = str.indexOf(open);
        if (start != -1) {
            int end = str.indexOf(close, start + open.length());
            if (end != -1) {
                return str.substring(start + open.length(), end);
            }
        }
        return null;
    }

    /**
     * Utility method for automatically casting content. It notifies the listener if the intent contains a recognized core content.
     *
     * @param intent   The intent to analyze.
     * @param listener Contains a callback method for each content type.
     * @return true if the content was recognized as core and passed to a callback method, false if it wasn't.
     */
    public static boolean parseCoreContents(Intent intent, CoreContentsListener listener) {
        TrackingInfo trackingInfo = intent.getParcelableExtra(NearItIntentConstants.TRACKING_INFO);
        Parcelable content = intent.getParcelableExtra(NearItIntentConstants.CONTENT);

        return carriesNearItContent(intent) &&
                parseContent(
                        content,
                        trackingInfo,
                        listener
                );
    }

    /**
     * Parse a parcelable content received from a proximity receiver. Since there's no intent, the intent field of the listener callback methods is always null.
     *
     * @param content  the @{@link Parcelable} content to parse. This contains the object set in the what component of the recipe.
     * @param listener the listener for the casted content types.
     * @return true if the content was recognized as core and passed to a callback method, false if it wasn't.
     */
    public static boolean parseCoreContents(Parcelable content, TrackingInfo trackingInfo, CoreContentsListener listener) {
        return parseContent(content, trackingInfo, listener);
    }

    private static boolean parseContent(Parcelable content, TrackingInfo trackingInfo, CoreContentsListener listener) {
        boolean coreContent = false;
        if (content instanceof Content) {
            Content c_notif = (Content) content;
            listener.gotContentNotification(c_notif, trackingInfo);
            coreContent = true;
        } else if (content instanceof SimpleNotification) {
            SimpleNotification s_notif = (SimpleNotification) content;
            listener.gotSimpleNotification(s_notif, trackingInfo);
            coreContent = true;
        } else if (content instanceof Coupon) {
            Coupon coup_notif = (Coupon) content;
            listener.gotCouponNotification(coup_notif, trackingInfo);
            coreContent = true;
        } else if (content instanceof CustomJSON) {
            CustomJSON custom_notif = (CustomJSON) content;
            listener.gotCustomJSONNotification(custom_notif, trackingInfo);
            coreContent = true;
        } else if (content instanceof Feedback) {
            Feedback f_notif = (Feedback) content;
            listener.gotFeedbackNotification(f_notif, trackingInfo);
            coreContent = true;
        }
        return coreContent;
    }

    /**
     * Checks if the intent carries NearIT content.
     *
     * @param intent the intent to check
     * @return true if the intent carries NearIT content, false otherwise.
     */
    public static boolean carriesNearItContent(Intent intent) {
        return intent.hasExtra(NearItIntentConstants.CONTENT);
    }

    /**
     * Ensures that an object reference passed as a parameter to the calling method is not null.
     *
     * @param reference an object reference
     * @return the non-null reference that was validated
     * @throws NullPointerException if {@code reference} is null
     */
    public static <T> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

    /**
     * Ensures that an object reference passed as a parameter to the calling method is not null.
     *
     * @param reference    an object reference
     * @param errorMessage the exception message to use if the check fails
     * @return the non-null reference that was validated
     * @throws NullPointerException if {@code reference} is null
     */
    public static <T> T checkNotNull(T reference, String errorMessage) {
        if (reference == null) {
            throw new NullPointerException(errorMessage);
        }
        return reference;
    }

    public static <T> Iterable<T> safe(Iterable<T> iterable) {
        return iterable == null ? Collections.<T>emptyList() : iterable;
    }

}
