package it.near.sdk.geopolis.tree;

import android.support.annotation.Nullable;

import java.util.List;

import it.near.sdk.geopolis.Node;

import static it.near.sdk.utils.NearUtils.safe;

public class TreeLevel {

    public static final String GEOFENCE_ENTER = "enter";
    public static final String GEOFENCE_EXIT = "exit";

    private Node parent;
    private List<Node> children;

    public TreeLevel(Node parent, List<Node> children) {
        this.parent = parent;
        this.children = children;
    }

    public boolean contains(String id) {
        return fetchNode(id) != null;
    }

    public boolean shouldConsiderEvent(String id, String event) {
        Node node = fetchNode(id);
        // TODO se è nullo il nodo non è contnuto nel livello attuale. E' un errore grave e non recuperabile. Super reset?
        if (node == null) return false;
        // enter in parent: ignore
        if (node == parent && GEOFENCE_ENTER.equals(event)) return false;
        // exit from children: ignore
        if (children.contains(node) && GEOFENCE_EXIT.equals(event)) return false;
        // everything else is a real event
        return true;
    }

    @Nullable
    private Node fetchNode(String id) {
        if (id == null) return null;
        if (parent.getId().equals(id)) return parent;
        for (Node child : safe(children)) {
            if (child.getId().equals(id))
                return child;
        }
        return null;
    }
}
