package it.near.sdk.Reactions;

import android.content.Intent;

import it.near.sdk.Reactions.ContentNotification.ContentNotification;
import it.near.sdk.Reactions.PollNotification.PollNotification;
import it.near.sdk.Reactions.SimpleNotification.SimpleNotification;

/**
 * Interface for being notified of core content types.
 *
 * @author cattaneostefano
 */
public interface CoreContentsListener {
    public abstract void getPollNotification(Intent intent, PollNotification notification, String content_plugin, String content_action, String pulse_plugin, String pulse_action, String pulse_slice);
    public abstract void getContentNotification(Intent intent,ContentNotification notification, String content_plugin, String content_action, String pulse_plugin, String pulse_action, String pulse_slice);
    public abstract void gotSimpleNotification(Intent intent,SimpleNotification notification, String content_plugin, String content_action, String pulse_plugin, String pulse_action, String pulse_slice);
}
