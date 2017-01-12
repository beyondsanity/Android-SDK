# Testing

## Manual recipe trigger

To help with testing, you can manually trigger a recipe.
The `NearItManager` object has a getter for the `RecipesManager`. 
With this object you can get the list of recipes with the method:

```java
nearItManager.getRecipesManager().getRecipes()
```
Once you pick the recipe you want to test use this method to trigger it:

```java
String id = recipe.getId();
nearItManager.processRecipe(id);
```
## Creating a tester audience

If you need to test some content, but just with some testers, you can use the profiling features and create actual recipes, selecting the proper segment.
Profile some selected users with a specific user data. For example, set a custom "testing" field to "true" for the test users, then create the proper field mapping in the settings. Now you can target test user by creating a segment in the "WHO" section of a recipe using this field.
