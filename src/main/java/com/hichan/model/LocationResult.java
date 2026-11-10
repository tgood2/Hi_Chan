package com.hichan;

/** Location match passed from the road matcher to the channel lookup. */
public record LocationResult(
        String geoHash,
        String segmentID,
        double milePost,
        String direction,
        String state,
        double distanceMeters) {

    // Retain the original getter names as well as Java's generated record accessors.
    public String getGeoHash() {
        return geoHash;
    }

    public String getSegmentID() {
        return segmentID;
    }

    public double getMilePost() {
        return milePost;
    }

    public String getDirection() {
        return direction;
    }
}
