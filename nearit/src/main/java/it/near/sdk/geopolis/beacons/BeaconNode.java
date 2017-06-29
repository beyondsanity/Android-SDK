package it.near.sdk.geopolis.beacons;

import com.google.gson.annotations.SerializedName;

import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;

import java.util.UUID;

import it.near.sdk.geopolis.Node;

public class BeaconNode extends Node {
    @SerializedName("proximity_uuid")
    public String proximityUUID;

    @SerializedName("major")
    public Integer major;

    @SerializedName("minor")
    public Integer minor;

    public BeaconNode() {
    }

    public String getProximityUUID() {
        return proximityUUID;
    }

    public void setProximityUUID(String proximityUUID) {
        this.proximityUUID = proximityUUID;
    }

    public Integer getMajor() {
        return major;
    }

    public void setMajor(Integer major) {
        this.major = major;
    }

    public Integer getMinor() {
        return minor;
    }

    public void setMinor(Integer minor) {
        this.minor = minor;
    }

    public static Region toAltRegion(BeaconNode beaconNode) throws NullPointerException {
        if (beaconNode.identifier == null || beaconNode.getMinor() != null)
            throw new NullPointerException();
        return new Region(beaconNode.getId(),
                Identifier.fromUuid(UUID.fromString(beaconNode.getProximityUUID())),
                beaconNode.getMajor() != null ? Identifier.fromInt(beaconNode.getMajor()) : null,
                beaconNode.getMinor() != null ? Identifier.fromInt(beaconNode.getMinor()) : null);
    }

    public static boolean isBeacon(Node node) {
        return node instanceof BeaconNode &&
                ((BeaconNode) node).getMinor() != null &&
                ((BeaconNode) node).getMajor() != null;
    }
}
