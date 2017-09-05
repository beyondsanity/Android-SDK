package it.near.sdk.geopolis.tree;

import it.near.sdk.communication.NearAsyncHttpClient;
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



}
