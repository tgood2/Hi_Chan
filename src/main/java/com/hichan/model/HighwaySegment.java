package com.hichan;

import java.util.List;

/** One source geometry part, with an approximate endpoint milepost range. */
public record HighwaySegment(
        String segmentID,
        String state,
        double beginMile,
        double endMile,
        List<Point> points) {

    /** Coordinates retain GeoJSON order: longitude first, then latitude. */
    public record Point(double lon, double lat) {
        public Point {
            Geohash.validate(lat, lon);
        }
    }

    public HighwaySegment {
        boolean validState = "WA".equals(state) || "ID".equals(state);
        if (segmentID == null || segmentID.isBlank() || !validState) {
            throw new IllegalArgumentException("Invalid segment identity/state");
        }

        if (!Double.isFinite(beginMile)
                || !Double.isFinite(endMile)
                || beginMile < 0
                || endMile < beginMile) {
            throw new IllegalArgumentException("Invalid milepost range");
        }

        // Copy the list so callers cannot change geometry after it has been indexed.
        points = List.copyOf(points);
        if (points.size() < 2) {
            throw new IllegalArgumentException("Road part needs two points");
        }
    }
}
