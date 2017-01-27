# User profiling

NearIT creates an anonymous profile for every user of your app. You can choose to add data to user profile. This data will be available inside recipes to allow the creation of user targets.

## Send user-data to NearIT

We automatically create an anonymous profile for every installation of the app. You can check that a profile was created by checking the existance of a profile ID.
```java
String profileId = NearItUserProfile.getProfileId(context);
```
If the result is null, it means that no profile is associated with the app installation (probably due to a network error).

To explicitly register a new user in our platform call the method:
```java
NearItUserProfile.createNewProfile(this, new ProfileCreationListener() {
            @Override
            public void onProfileCreated(boolean created, String profileId) {
                // see the created boolean to know if the profile was freshly created or was already created 
            }

            @Override
            public void onProfileCreationError(String error) {
                
            }
        });
```
Calling this method multiple times will NOT create multiple profiles.

After the profile is created set user data:
```java
NearItUserProfile.setUserData(context, "name", "John", new UserDataNotifier() {
    @Override
    public void onDataCreated() {
        // data was set/created                                                
    }
                                                       
    @Override
    public void onDataNotSetError(String error) {
        // there was an error                        
    }
});
```

If you have multiple data properties, set them in batch:
```java
HashMap<String, String> userDataMap = new HashMap<>();
userDataMap.put("name", "John");
userDataMap.put("age", "23");           // set everything as String
userDataMap.put("saw_tutorial", "true") // even booleans, the server has all the right logic
NearItUserProfile.setBatchUserData(context, userDataMap, new UserDataNotifier() {
            @Override
            public void onDataCreated() {
                // data was set/created 
            }

            @Override
            public void onDataNotSetError(String error) {

            }
        });
```
If you try to set user data before creating a profile the error callback will be called.

If you want to reset your profile use this method:
```java
NearItUserProfile.resetProfileId(context)
```
Further calls to NearItUserProfile.getProfileId(context) will return null.
A creation of a new profile after the reset will create a profile with no user data.

## Link external data to a NearIT profile

You might want to keep a reference between the data hosted on you DBs and NearIT data.
You can do it by setting the user ID explicitly. 
```java
NearItUserProfile.setProfileId(context, profileId);
```
You can then set the relevant user-data to this profile with the aforementioned methods.

This way, you will have a reference between NearIT data and your others data sources. Please keep in mind that you will be responsible of generating unique user IDs.
