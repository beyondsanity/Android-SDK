package it.near.sdk.Push;

import android.content.Intent;

import org.json.JSONException;

import it.near.sdk.R;
import it.near.sdk.Reactions.Content.Content;
import it.near.sdk.Reactions.CoreContentsListener;
import it.near.sdk.Reactions.Coupon.Coupon;
import it.near.sdk.Reactions.CustomJSON.CustomJSON;
import it.near.sdk.Reactions.Feedback.Feedback;
import it.near.sdk.Reactions.Poll.Poll;
import it.near.sdk.Reactions.SimpleNotification.SimpleNotification;
import it.near.sdk.Recipes.Models.Recipe;
import it.near.sdk.Utils.BaseIntentService;
import it.near.sdk.Utils.IntentConstants;
import it.near.sdk.Utils.NearNotification;

/**
 * @author cattaneostefano.
 */
public class FcmIntentService extends BaseIntentService implements CoreContentsListener {

    private static String TAG = "FcmIntentService";
    private static final int PUSH_NOTIFICATION_ID = 2;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public FcmIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        parseCoreContents(intent, this);
        FcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    @Override
    public void getPollNotification(Intent intent, Poll notification, String notifText, String content_plugin, String content_action, String pulse_plugin, String pulse_action, String pulse_bundle) {
        sendSimpleNotification(intent);
    }

    @Override
    public void getContentNotification(Intent intent, Content notification, String notifText, String content_plugin, String content_action, String pulse_plugin, String pulse_action, String pulse_bundle) {
        sendSimpleNotification(intent);
    }

    @Override
    public void getCouponNotification(Intent intent, Coupon notification, String notificationText, String content_plugin, String content_action, String pulse_plugin, String pulse_action, String pulse_bundle) {
        sendSimpleNotification(intent);
    }

    @Override
    public void getCustomJSONNotification(Intent intent, CustomJSON notification, String notificationText, String content_plugin, String content_action, String pulse_plugin, String pulse_action, String pulse_bundle) {
        // TODO this was a custom json, implementation may vary, it's usually overriden
    }

    @Override
    public void getSimpleNotification(Intent intent, SimpleNotification s_notif, String notif_body, String reaction_plugin, String reaction_action, String pulse_plugin, String pulse_action, String pulse_bundle) {
        sendSimpleNotification(intent);
    }

    @Override
    public void getFeedbackNotification(Intent intent, Feedback s_notif, String notif_body, String reaction_plugin, String reaction_action, String pulse_plugin, String pulse_action, String pulse_bundle) {
        sendSimpleNotification(intent);
    }

    private void sendSimpleNotification(Intent intent){
        Intent targetIntent = getPackageManager().getLaunchIntentForPackage(this.getPackageName());
        targetIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        targetIntent.putExtras(intent.getExtras());
        String notif_title = intent.getStringExtra(IntentConstants.NOTIF_TITLE);
        if (notif_title == null) {
            notif_title = getApplicationInfo().loadLabel(getPackageManager()).toString();
        }
        String notifText = intent.getStringExtra(IntentConstants.NOTIF_BODY);
        String recipeId = intent.getStringExtra(IntentConstants.RECIPE_ID);
        try {
            Recipe.sendTracking(getApplicationContext(), recipeId, Recipe.NOTIFIED_STATUS);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        NearNotification.send(this, R.drawable.ic_send_white_24dp, notif_title, notifText, targetIntent, PUSH_NOTIFICATION_ID);

    }
}
