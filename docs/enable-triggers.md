# Enable triggers #

Based on what recipe triggers you want to use some setup will be necessary.

## Location based Triggers ##

When you want to start the radar for geofences and beacons call this method

```java
    // call this when you are given the proper permission for scanning (ACCESS_FINE_LOCATION)
    nearItManager.startRadar()
    // to stop call this method nearItManager.stopRadar()
```

The SDK automatically includes the permission for location access in its manifest (necessary for beacon and geofence monitoring). When targeting API level 23+, please ask for and verify the presence of ACCESS_FINE_LOCATION permissions at runtime.

### Built-in region background receivers ###

If you want to be notified with content from recipes working on the background (bluetooth or geofence) using the NearIT built-in background notifications, put this in your app manifest. 
```xml
<!-- built in region receivers -->
<receiver android:name="it.near.sdk.Geopolis.Background.RegionBroadcastReceiver"
    android:exported="false">
    <intent-filter>
        <action android:name="it.near.sdk.permission.GEO_MESSAGE"/>
        <category android:name="android.intent.category.DEFAULT"/>
    </intent-filter>
</receiver>
```

You can set your own icon for the notifications with the method *setNotificationImage(int imgRes)* of *NearItManager*

## Enable Push Notifications ##

NearIt offers a default push notification reception and visualization. It shows a system notification with the notification message and title entered in the what section of a recipe.
When a user taps on a notification, it starts the app launcher and passes the intent with all the necessary information about the push, including the reaction bundle (the content to display) just like a proximity-driven notifications.

To enable push notification, set up a firebase project and follow the official instructions to integrate it into an app. [If you need help follow those steps](docs/firebase.md)
Enter the cloud messaging firebase server key into the CMS. Push notification only work if a profile is created. We automatically create an anonymous profile for every user, but if you want to know more about profiles check [the user profilation section](docs/user-profilation.md).

To receive the system notification of a push recipe, add this receiver in the *application* tag of your app *manifest*
```xml
<application ...>
...
    <receiver
         android:name="it.near.sdk.Push.FcmBroadcastReceiver"
         android:exported="false">
         <intent-filter>
                <action android:name="it.near.sdk.permission.PUSH_MESSAGE" />
                <category android:name="android.intent.category.DEFAULT" />
         </intent-filter>
    </receiver>
</application>
```


