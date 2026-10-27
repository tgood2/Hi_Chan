package com.hichan;

/**
 * data design container that represents the output of the location processor
 */

Public class LocationResult {
    private final String geoHash;
    private final String segmentID;
    private final double milePost;
    private final String direction;

    public LocationResult(String geoHash, String segmentID, double milePost, String direction) {
        this.geoHash = geoHash;
        this.direction = direction;
        this.milePost = milePost;
        this.segmentID = segmentID;
    }

    public String getGeoHash() {
        return geoHash;
    }

    public String getSegmentID() {
        return segmentID;
    }

    public String getDirection() {
        return direction;
    }

    public double getMilePost() { return milePost; }
}