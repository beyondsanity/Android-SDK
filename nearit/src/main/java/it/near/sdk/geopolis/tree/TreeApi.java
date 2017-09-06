package it.near.sdk.geopolis.tree;

import it.near.sdk.communication.NearAsyncHttpClient;
import it.near.sdk.geopolis.Node;
import it.near.sdk.geopolis.beacons.BeaconNode;
import it.near.sdk.geopolis.geofences.GeofenceNode;
import it.near.sdk.morpheusnear.Morpheus;

public class TreeApi {

    private final NearAsyncHttpClient httpClient;
    private final Morpheus morpheus;

    public TreeApi(NearAsyncHttpClient httpClient, Morpheus morpheus) {
        this.httpClient = httpClient;
        this.morpheus = morpheus;
    }

    public void refreshTreeCofing(TreeRequestListener listener) {
        // TODO call listener after fetching server data
    }

    static Morpheus buildMorpheus() {
        Morpheus morpheus = new Morpheus();
        morpheus.getFactory().getDeserializer().registerResourceClass("nodes", Node.class);
        morpheus.getFactory().getDeserializer().registerResourceClass("beacon_nodes", BeaconNode.class);
        morpheus.getFactory().getDeserializer().registerResourceClass("geofence_nodes", GeofenceNode.class);
        return morpheus;
    }

}
