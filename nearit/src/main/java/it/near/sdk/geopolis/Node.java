package it.near.sdk.geopolis;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import it.near.sdk.morpheusnear.annotations.Relationship;
import it.near.sdk.morpheusnear.Resource;

/**
 * Created by cattaneostefano on 21/09/16.
 */

public class Node extends Resource {
    @SerializedName("identifier")
    public String identifier;

    @Relationship("parent")
    public Node parent;

    @Relationship("children")
    public List<Node> children;

    public Node() {
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Node getParent() {
        return parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public List<Node> getChildren() {
        return children;
    }

    public void setChildren(List<Node> children) {
        this.children = children;
    }
}
