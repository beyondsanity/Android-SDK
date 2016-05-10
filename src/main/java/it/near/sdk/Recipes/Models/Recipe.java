package it.near.sdk.Recipes.Models;

import it.near.sdk.MorpheusNear.Annotations.Relationship;
import it.near.sdk.MorpheusNear.Annotations.SerializeName;
import it.near.sdk.MorpheusNear.Resource;

/**
 * @author cattaneostefano
 */
public class Recipe extends Resource {

    @SerializeName("name")
    String name;
    @SerializeName("pulse_plugin_id")
    String pulse_plugin_id;
    @SerializeName("pulse_bundle")
    PulseBundle pulse_bundle;
    @Relationship("pulse_action")
    PulseAction pulse_action;
    @SerializeName("reaction_plugin_id")
    String reaction_plugin_id;
    @SerializeName("reaction_bundle")
    ReactionBundle reaction_bundle;
    @Relationship("reaction_action")
    ReactionAction reaction_action;
    /*@SerializeName("operation_plugin_id")
    String operation_plugin_id;
    @SerializeName("operation_slice_id")
    String operation_slice_id;*/
    /*@Relationship("operation_action")
    OperationAction operation_action;*/

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPulse_plugin_id() {
        return pulse_plugin_id;
    }

    public void setPulse_plugin_id(String pulse_plugin_id) {
        this.pulse_plugin_id = pulse_plugin_id;
    }

    public PulseBundle getPulse_bundle() {
        return pulse_bundle;
    }

    public void setPulse_bundle(PulseBundle pulse_bundle) {
        this.pulse_bundle = pulse_bundle;
    }

    /*public String getOperation_plugin_id() {
        return operation_plugin_id;
    }

    public void setOperation_plugin_id(String operation_plugin_id) {
        this.operation_plugin_id = operation_plugin_id;
    }

    public String getOperation_slice_id() {
        return operation_slice_id;
    }

    public void setOperation_slice_id(String operation_slice_id) {
        this.operation_slice_id = operation_slice_id;
    }*/

    public String getReaction_plugin_id() {
        return reaction_plugin_id;
    }

    public void setReaction_plugin_id(String reaction_plugin_id) {
        this.reaction_plugin_id = reaction_plugin_id;
    }

    public ReactionBundle getReaction_bundle() {
        return reaction_bundle;
    }

    public void setReaction_bundle(ReactionBundle reaction_bundle) {
        this.reaction_bundle = reaction_bundle;
    }

    public PulseAction getPulse_action() {
        return pulse_action;
    }

    public void setPulse_action(PulseAction pulse_action) {
        this.pulse_action = pulse_action;
    }

    /*public OperationAction getOperation_action() {
        return operation_action;
    }

    public void setOperation_action(OperationAction operation_action) {
        this.operation_action = operation_action;
    }*/

    public ReactionAction getReaction_action() {
        return reaction_action;
    }

    public void setReaction_action(ReactionAction reaction_action) {
        this.reaction_action = reaction_action;
    }

}
