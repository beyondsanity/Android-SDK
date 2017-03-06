# Handle Recipe Content

NearIT takes care of delivering content at the right time, you will just need to handle content presentation. 

## Foreground vs Background

Recipes either deliver content in background or in foreground but not both. Check this table to see how you will be notified.

| Type of trigger                  | Delivery           |
|----------------------------------|--------------------|
| Push (immediate or scheduled)    | Background intent  |
| Enter and Exit on geofences      | Background intent  |
| Enter and Exit on beacon regions | Background intent  |
| Enter in a specific beacon range | Proximity listener (foreground) |

## Foreground Content

To receive foreground content (e.g. ranging recipes) set a proximity listener with the method
```java
{
    ...
    nearItManager.addProximityListener(this);
    // remember to remove the listener when the object is being destroyed with 
    // nearItManager.removeProximityListener(this);
    ...
}

@Override
public void foregroundEvent(Parcelable content, Recipe recipe) {
    // handle the event
    // To extract the content and to have it automatically casted to the appropriate object type
    // NearUtils.parseCoreContents(content, recipe, coreContentListener)
}   
```

## Background Content

Once you have added at least one of the receivers for any background working trigger ([learn more](enable-triggers.md)) you will be delivered the actual content through an intent that will call your app launcher activity and carry some extras.
To extract the content from an intent use the utility method:
```java
NearUtils.parseCoreContents(intent, coreContentListener);
```

If you want to customize the behavior of background notification see [this page](docs/custom-bkg-notification.md)

## Trackings

NearIT analytics on recipes are built from trackings describing the status of user engagement with a recipe. The two recipe states are "Notified" and "Engaged" to represent a recipe delivered to the user and a recipe that the user responded to.

Background recipes track themselves as notified. To track the tap event, use this method:
```java
RecipesManager.sendTracking(getApplicationContext(), recipeId, Recipe.ENGAGED_STATUS);
```
You should be able to catch the event inside the activity that is started after interacting with the notification.

Foreground recipes don't have automatic tracking. You need to track both the "Notified" and the "Engaged" statuses when it's the best appropriate for you scenario.
```java
RecipesManager.sendTracking(getApplicationContext(), recipe.getId(), Recipe.NOTIFIED_STATUS);
// and
RecipesManager.sendTracking(getApplicationContext(), recipe.getId(), Recipe.ENGAGED_STATUS);
```

## Content Objects

For each callback method of the *coreContentListener* you will receive a different content object. Every object type has a `getId()` getter, and here are the details for every other one:

- `SimpleNotification` with the following getters:
    - `getNotificationMessage()` returns the notification message
    - `getNotificationTitle()` returns the notification title
    
- `Content` for the notification with content, with the following getters:
    - `getContent()` returns the text content
    - `getVideo_link()` returns the video link string
    - `getImages_links()` returns a list of *ImageSet* object containing the source links for the images
    
- `Feedback` with the following getters:
    - `getQuestion()` returns the feedback request string
    - `getRecipeId()` returns the recipeId associated with the feedback (you'll need it for answer it)
To give a feedback call this method:
```java
// rating must be an integer between 1 and 5, and you can set a comment string.
nearItManager.sendEvent(new FeedbackEvent(feedback, rating, "Awesome"));
// if you don't hold the feedback object use this constructor
nearItManager.sendEvent(new FeedbackEvent(feedbackId, rating, "Nice", recipeId));
// the sendEvent method is available in 2 variants: with or without explicit callback handler. Example:
nearItManager.sendEvent(new FeedbackEvent(...), responseHandler);
```
    
- `Coupon` with the following getters:
    - `getName()` returns the coupon name
    - `getDescription()` returns the coupon description
    - `getValue()` returns the value string
    - `getExpires_at()` returns the expiring date (as a string)
    - `getIconSet()` returns an *ImageSet* object containing the source links for the icon
    - `getSerial()` returns the serial code of the single coupon as a string
    - `getClaimedAt()` returns the claimed date (when the coupon was earned) of the coupon as a string
    - `getRedeemedAt()` returns the redeemed date (when the coupon was used) of the coupon as a string
    
- `CustomJSON` with the following getters:
    - `getContent()` returns the json content as an *HashMap<String, Object>* (just like Gson)




