package it.near.sdk.recipes;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import it.near.sdk.logging.NearLog;
import it.near.sdk.reactions.Reaction;
import it.near.sdk.recipes.models.Recipe;
import it.near.sdk.recipes.pulse.TriggerRequest;
import it.near.sdk.recipes.validation.RecipeValidationFilter;

/**
 * Menage recipes download, caching and direct calling.
 *
 * @author cattaneostefano
 */
public class RecipesManager implements RecipeEvaluator {

    private static final String TAG = "RecipesManager";

    private HashMap<String, Reaction> reactions = new HashMap<>();
    private final RecipeTrackSender recipeTrackSender;
    private final RecipeValidationFilter recipeValidationFilter;
    private final RecipeRepository recipeRepository;
    private final RecipesApi recipesApi;
    private boolean online_eval = true;

    public RecipesManager(RecipeValidationFilter recipeValidationFilter,
                          RecipeTrackSender recipeTrackSender,
                          RecipeRepository recipeRepository,
                          RecipesApi recipesApi) {
        this.recipeValidationFilter = recipeValidationFilter;
        this.recipeTrackSender = recipeTrackSender;
        this.recipeRepository = recipeRepository;
        this.recipesApi = recipesApi;
    }

    public void addReaction(Reaction reaction) {
        reactions.put(reaction.getReactionPluginName(), reaction);
    }

    /**
     * return the list of recipes
     *
     * @return the list of recipes
     */
    public List<Recipe> getRecipes() {
        return recipeRepository.getLocalRecipes();
    }

    /**
     * Tries to refresh the recipes list. If some network problem occurs, a cached version will be used.
     */
    public void refreshConfig() {
        refreshConfig(new RecipeRefreshListener() {
            @Override
            public void onRecipesRefresh() {
            }

            @Override
            public void onRecipesRefreshFail() {
            }
        });
    }

    public void syncConfig() {
        syncConfig(new RecipeRefreshListener() {
            @Override
            public void onRecipesRefresh() {

            }

            @Override
            public void onRecipesRefreshFail() {

            }
        });
    }

    /**
     * Tries to refresh the recipes list. If some network problem occurs, a cached version will be used.
     * Plus a listener will be notified of the refresh process.
     */
    public void refreshConfig(final RecipeRefreshListener listener) {
        recipeRepository.refreshRecipes(new RecipeRepository.RecipesListener() {
            @Override
            public void onGotRecipes(List<Recipe> recipes, boolean online_evaluation_fallback, boolean dataChanged) {
                listener.onRecipesRefresh();
                RecipesManager.this.online_eval = online_evaluation_fallback;
            }
        });
    }

    /**
     * Sync the recipe configuration, only if the cache is cold.
     */
    public void syncConfig(final RecipeRefreshListener listener) {
        recipeRepository.syncRecipes(new RecipeRepository.RecipesListener() {
            @Override
            public void onGotRecipes(List<Recipe> recipes, boolean online_evaluation_fallback, boolean dataChanged) {
                listener.onRecipesRefresh();
                RecipesManager.this.online_eval = online_evaluation_fallback;
            }
        });
    }

    /**
     * Tries to trigger a recipe. If no reaction plugin can handle the recipe, nothing happens.
     *
     * @param recipe the recipe to trigger.
     */
    public void gotRecipe(Recipe recipe) {
        Reaction reaction = reactions.get(recipe.getReaction_plugin_id());
        if (reaction != null) {
            reaction.handleReaction(recipe);
        }
    }

    /**
     * Process a recipe from its id. Typically called for processing a push recipe.
     *
     * @param id push id.
     */
    public void processRecipe(final String id) {
        recipesApi.fetchRecipe(id, new RecipesApi.SingleRecipeListener() {
            @Override
            public void onRecipeFetchSuccess(Recipe recipe) {
                String reactionPluginName = recipe.getReaction_plugin_id();
                Reaction reaction = reactions.get(reactionPluginName);
                reaction.handlePushReaction(recipe, recipe.getReaction_bundle());
            }

            @Override
            public void onRecipeFetchError(String error) {
                NearLog.d(TAG, "single recipe failed: " + error);
            }
        });
    }

    /**
     * Process a recipe from the reaction triple. Used for getting a content from a push
     */
    public void processRecipe(String recipeId, String notificationText, String reactionPlugin, String reactionAction, String reactionBundleId) {
        Reaction reaction = reactions.get(reactionPlugin);
        if (reaction == null) return;
        reaction.handlePushReaction(recipeId, notificationText, reactionAction, reactionBundleId);
    }

    /**
     * Handle a push recipe that contained the actual reaction bundle, encoded in the payload
     *
     * @return whether the content was properly consumed by the reaction plugin
     */
    public boolean processReactionBundle(String recipeId, String notificationText, String reactionPlugin, String reactionAction, String reactionBundleString) {
        Reaction reaction = reactions.get(reactionPlugin);
        if (reaction == null) return false;
        return reaction.handlePushBundledReaction(recipeId, notificationText, reactionAction, reactionBundleString);
    }

    private void onlinePulseEvaluation(TriggerRequest triggerRequest) {
        recipesApi.onlinePulseEvaluation(triggerRequest.plugin_name, triggerRequest.plugin_action, triggerRequest.bundle_id, new RecipesApi.SingleRecipeListener() {
            @Override
            public void onRecipeFetchSuccess(Recipe recipe) {
                recipeRepository.addRecipe(recipe);
                gotRecipe(recipe);
            }

            @Override
            public void onRecipeFetchError(String error) {
                NearLog.d(TAG, "Recipe eval by pulse error: " + error);
            }
        });
    }

    /**
     * Online evaluation of a recipe.
     *
     * @param recipeId recipe identifier.
     */
    void evaluateRecipe(String recipeId) {
        recipesApi.evaluateRecipe(recipeId, new RecipesApi.SingleRecipeListener() {
            @Override
            public void onRecipeFetchSuccess(Recipe recipe) {
                gotRecipe(recipe);
            }

            @Override
            public void onRecipeFetchError(String error) {
                NearLog.d(TAG, "Recipe evaluation error: " + error);
            }
        });
    }

    private boolean filterAndNotify(List<Recipe> matchingRecipes) {
        matchingRecipes = recipeValidationFilter.filterRecipes(matchingRecipes);
        if (matchingRecipes.isEmpty()) return false;
        Recipe winnerRecipe = matchingRecipes.get(0);
        if (winnerRecipe.isEvaluatedOnline()) {
            evaluateRecipe(winnerRecipe.getId());
        } else {
            gotRecipe(winnerRecipe);
        }
        return true;
    }

    private boolean handlePulseLocally(TriggerRequest triggerRequest) {
        if (triggerRequest == null ||
                triggerRequest.plugin_name == null ||
                triggerRequest.plugin_action == null ||
                triggerRequest.bundle_id == null) return false;

        List<Recipe> recipes = recipeRepository.getLocalRecipes();
        List<Recipe> matchingRecipes = new ArrayList<>();
        if (recipes == null) return false;
        // Find the recipes that matches the pulse
        for (Recipe recipe : recipes) {
            if (recipe.getPulse_plugin_id() == null ||
                    recipe.getPulse_action() == null ||
                    recipe.getPulse_action().getId() == null ||
                    recipe.getPulse_bundle() == null ||
                    recipe.getPulse_bundle().getId() == null)
                continue;
            if (recipe.getPulse_plugin_id().equals(triggerRequest.plugin_name) &&
                    recipe.getPulse_action().getId().equals(triggerRequest.plugin_action) &&
                    recipe.getPulse_bundle().getId().equals(triggerRequest.bundle_id)) {
                matchingRecipes.add(recipe);
            }
        }

        if (matchingRecipes.isEmpty()) return false;

        return filterAndNotify(matchingRecipes);

    }


    private boolean handlePulseTags(TriggerRequest triggerRequest) {
        if (triggerRequest == null ||
                triggerRequest.plugin_name == null ||
                triggerRequest.plugin_tag_action == null ||
                triggerRequest.tags == null ||
                triggerRequest.tags.isEmpty())
            return false;

        List<Recipe> recipes = recipeRepository.getLocalRecipes();
        List<Recipe> matchingRecipes = new ArrayList<>();
        if (recipes == null) return false;
        // Find the recipes that matches the pulse

        for (Recipe recipe : recipes) {
            if (recipe.getPulse_plugin_id() == null ||
                    recipe.getPulse_action() == null ||
                    recipe.getPulse_action().getId() == null ||
                    recipe.tags == null ||
                    recipe.tags.isEmpty())
                continue;
            if (recipe.getPulse_plugin_id().equals(triggerRequest.plugin_name) &&
                    recipe.getPulse_action().getId().equals(triggerRequest.plugin_tag_action) &&
                    triggerRequest.tags.containsAll(recipe.tags)) {
                matchingRecipes.add(recipe);
            }
        }

        if (matchingRecipes.isEmpty()) return false;

        return filterAndNotify(matchingRecipes);
    }

    private void handlePulseOnline(final TriggerRequest triggerRequest) {
        if (online_eval) {
            onlinePulseEvaluation(triggerRequest);
        } else {
            recipeRepository.syncRecipes(new RecipeRepository.RecipesListener() {
                @Override
                public void onGotRecipes(List<Recipe> recipes, boolean online_evaluation_fallback, boolean dataChanged) {
                    if (dataChanged) {
                        boolean found = handlePulseLocally(triggerRequest) ||
                                handlePulseTags(triggerRequest);
                        if (!found && online_evaluation_fallback) {
                            onlinePulseEvaluation(triggerRequest);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void handleTriggerRequest(TriggerRequest triggerRequest) {
        if (!handlePulseLocally(triggerRequest) &&
                !handlePulseTags(triggerRequest)) {
            handlePulseOnline(triggerRequest);
        }
    }

    /**
     * Sends tracking on a recipe.
     * Those two statuses are natively supported:
     * {@value Recipe#NOTIFIED_STATUS} and {@value Recipe#ENGAGED_STATUS}
     * If you wish to use custom tracking, send your string as a tracking event.
     *
     * @param recipeId      the recipe identifier.
     * @param trackingEvent notified status to send.
     * @throws JSONException
     */
    public void sendTracking(String recipeId, String trackingEvent) throws JSONException {
        recipeTrackSender.sendTracking(recipeId, trackingEvent);
    }

}
