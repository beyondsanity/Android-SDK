package it.near.sdk;

import android.content.Context;
import android.content.SharedPreferences;

import it.near.sdk.Communication.Constants;

/**
 * Created by cattaneostefano on 12/04/16.
 */
public class GlobalConfig {

    private static GlobalConfig mInstance = null;
    private Context mContext;
    private final String APIKEY = "apikey";
    private String apiKey;
    private final String APPID = "appid";
    private String appId;
    private final String SENDERID = "senderid";
    private String senderId;
    private final String DEVICETOKEN = "devicetoken";
    private String deviceToken;
    private static final String INSTALLATIONID = "installationid";
    private String installationId;
    private final String NOTIFICATIONIMAGE = "notification_image";
    private int notificationImage = 0;
    private SharedPreferences sp;
    private String prefsNameSuffix = "NearConfig";
    private SharedPreferences.Editor editor;

    public GlobalConfig(Context mContext) {
        this.mContext = mContext;
        setUpSharedPreferences();
    }

    private void setUpSharedPreferences() {
        String PACK_NAME = mContext.getApplicationContext().getPackageName();
        String PREFS_NAME = PACK_NAME + prefsNameSuffix;
        sp = mContext.getSharedPreferences(PREFS_NAME, 0);
        editor = sp.edit();
    }

    public static GlobalConfig getInstance(Context context){
        if(mInstance == null)
        {
            mInstance = new GlobalConfig(context);
        }
        return mInstance;
    }

    public int getNotificationImage() {
        if (notificationImage == 0){
            notificationImage = sp.getInt(NOTIFICATIONIMAGE, 0);
        }
        return notificationImage;
    }

    public void setNotificationImage(int notificationImage) {
        this.notificationImage = notificationImage;
        editor.putInt(NOTIFICATIONIMAGE, notificationImage).apply();
    }


    public String getApiKey() {
        if (apiKey == null){
            apiKey = getLocalString(APIKEY);
        }
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        setLocalString(APIKEY, apiKey);
    }

    public String getAppId() {
        if (appId == null){
            appId = getLocalString(APPID);
        }
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
        setLocalString(APPID, appId);
    }


    public String getSenderId() {
        if (senderId == null){
            senderId = getLocalString(SENDERID);
        }
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
        setLocalString(SENDERID, senderId);
    }

    public String getDeviceToken() {
        if (deviceToken == null){
            deviceToken = getLocalString(DEVICETOKEN);
        }
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
        setLocalString(DEVICETOKEN, deviceToken);
    }

    public String getInstallationId() {
        if (installationId == null){
            installationId = getLocalString(INSTALLATIONID);
        }
        return installationId;
    }

    public void setInstallationId(String installationId){
        this.installationId = installationId;
        setLocalString(INSTALLATIONID, installationId);
    }

    private void setLocalString(String name, String value){
        editor.putString(name, value).apply();
    }

    private String getLocalString(String name){
        return sp.getString(name, null);
    }

}
