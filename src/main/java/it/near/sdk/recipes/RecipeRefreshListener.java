package it.near.sdk.recipes;

/**
 * Created by cattaneostefano on 31/08/16.
 */

public interface RecipeRefreshListener {
    public abstract void onRecipesRefresh();
    public abstract void onRecipesRefreshFail();
}
