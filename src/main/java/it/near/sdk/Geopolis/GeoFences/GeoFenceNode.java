package it.near.sdk.Geopolis.GeoFences;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.location.Geofence;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import it.near.sdk.Geopolis.Node;

/**
 * Created by cattaneostefano on 21/09/16.
 */

public class GeoFenceNode extends Node implements Parcelable{
    private static final int LOITERING_DELAY = 30000;
    @SerializedName("latitude")
    Double latitude;

    @SerializedName("longitude")
    Double longitude;

    @SerializedName("radius")
    Double radius;

    public GeoFenceNode() {
        super();
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public Geofence toGeofence(){
        return new Geofence.Builder()
                .setRequestId(getId())
                .setCircularRegion(getLatitude(), getLongitude(), getRadius().intValue())
                .setLoiteringDelay(LOITERING_DELAY)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT | Geofence.GEOFENCE_TRANSITION_DWELL)
                .build();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeDouble(radius);
        dest.writeString(getId());
    }

    protected GeoFenceNode(Parcel in) {
        latitude = in.readDouble();
        longitude = in.readDouble();
        radius = in.readDouble();
        setId(in.readString());
    }

    public static final Creator<GeoFenceNode> CREATOR = new Creator<GeoFenceNode>() {
        @Override
        public GeoFenceNode createFromParcel(Parcel in) {
            return new GeoFenceNode(in);
        }

        @Override
        public GeoFenceNode[] newArray(int size) {
            return new GeoFenceNode[size];
        }
    };

    public static List<Geofence> toGeofences(List<GeoFenceNode> geoFenceNodes) {
        List<Geofence> geofences = new ArrayList<>();
        for (GeoFenceNode geoFenceNode : geoFenceNodes) {
            geofences.add(geoFenceNode.toGeofence());
        }
        return geofences;
    }

    /**
     * Exclusion strategy for gson serializing, since there are circular references in the structure
     * @return
     */
    public static ExclusionStrategy getExclusionStrategy(){
        ExclusionStrategy es = new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                return f.getName().equals("parent") || f.getName().equals("children");
            }
            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        };
        return es;
    }
}
