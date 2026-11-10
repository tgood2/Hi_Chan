package com.hichan;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

//Matches a GPS position to the closest road geometry within the allowed distance
public final class LocationProcessor {
    private final LoadedData data;
    private final double toleranceMeters;

    public LocationProcessor(LoadedData data, double toleranceMeters) {
        this.data = Objects.requireNonNull(data);

        if (!Double.isFinite(toleranceMeters)
                || toleranceMeters <= 0
                || toleranceMeters > 1000) {
            throw new IllegalArgumentException("Tolerance must be 0 < meters <= 1000");
        }

        this.toleranceMeters = toleranceMeters;
    }

    /** Converts accepted direction aliases into the E/W values used by channel rules. */
    public static String direction(String direction) {
        if (direction == null) {
            throw new IllegalArgumentException("Direction required");
        }

        String normalizedDirection = direction.trim().toUpperCase(Locale.ROOT);

        switch (normalizedDirection) {
            case "E", "EB", "EAST", "EASTBOUND":
                return "E";
            case "W", "WB", "WEST", "WESTBOUND":
                return "W";
            default:
                throw new IllegalArgumentException("Direction must be E or W");
        }
    }

    public LocationResult process(double latitude, double longitude, String direction) {
        Geohash.validate(latitude, longitude);
        String normalizedDirection = direction(direction);

        // This dataset covers WA/ID, so reject distant positions before searching the index.
        if (latitude < 45 || latitude > 50 || longitude < -125 || longitude > -110) {
            throw new LocationNotFoundException(
                    "Location not on or near Interstate 90 in WA/ID");
        }

        Set<HighwaySegment> candidates = findCandidates(latitude, longitude);
        HighwaySegment closestSegment = null;
        double closestDistanceMeters = Double.POSITIVE_INFINITY;
        double estimatedMilePost = 0;

        for (HighwaySegment segment : candidates) {
            List<HighwaySegment.Point> points = segment.points();
            double totalLengthMeters = 0;

            for (int pointIndex = 1; pointIndex < points.size(); pointIndex++) {
                HighwaySegment.Point start = points.get(pointIndex - 1);
                HighwaySegment.Point end = points.get(pointIndex);
                totalLengthMeters += length(start, end);
            }

            double traversedLengthMeters = 0;

            for (int pointIndex = 1; pointIndex < points.size(); pointIndex++) {
                HighwaySegment.Point start = points.get(pointIndex - 1);
                HighwaySegment.Point end = points.get(pointIndex);
                double edgeLengthMeters = length(start, end);

                // Use the GPS position as the origin of a local plane measured in meters.
                double longitudeScale = 111195 * Math.cos(Math.toRadians(latitude));
                double startX = (start.lon() - longitude) * longitudeScale;
                double startY = (start.lat() - latitude) * 111195;
                double endX = (end.lon() - longitude) * longitudeScale;
                double endY = (end.lat() - latitude) * 111195;
                double edgeX = endX - startX;
                double edgeY = endY - startY;
                double squaredEdgeLength = edgeX * edgeX + edgeY * edgeY;

                // Project onto the edge, clamping the result to its two endpoints.
                // A repeated coordinate gives a zero-length edge, so use its start point.
                double edgeFraction = 0;
                if (squaredEdgeLength != 0) {
                    double projection = -(startX * edgeX + startY * edgeY)
                            / squaredEdgeLength;
                    edgeFraction = Math.max(0, Math.min(1, projection));
                }

                double distanceMeters = Math.hypot(
                        startX + edgeFraction * edgeX,
                        startY + edgeFraction * edgeY);

                boolean isCloser = distanceMeters < closestDistanceMeters - 1e-6;
                boolean isTied = Math.abs(distanceMeters - closestDistanceMeters) <= 1e-6;
                boolean winsTie = closestSegment == null
                        || segment.segmentID().compareTo(closestSegment.segmentID()) < 0;

                // Stable ID ordering makes equal-distance matches repeatable.
                if (isCloser || (isTied && winsTie)) {
                    closestSegment = segment;
                    closestDistanceMeters = distanceMeters;

                    double segmentFraction = 0;
                    if (totalLengthMeters != 0) {
                        segmentFraction = (traversedLengthMeters
                                + edgeFraction * edgeLengthMeters) / totalLengthMeters;
                    }

                    // This assumes the part's coordinate order follows increasing mileage.
                    // Source endpoint calibration is still required; see README limitations.
                    estimatedMilePost = segment.beginMile()
                            + (segment.endMile() - segment.beginMile()) * segmentFraction;
                }

                traversedLengthMeters += edgeLengthMeters;
            }
        }

        if (closestSegment == null || closestDistanceMeters > toleranceMeters) {
            throw new LocationNotFoundException(
                    "Location not on or near Interstate 90 in WA/ID");
        }

        return new LocationResult(
                Geohash.encode(latitude, longitude, 6),
                closestSegment.segmentID(),
                estimatedMilePost,
                normalizedDirection,
                closestSegment.state(),
                closestDistanceMeters);
    }

    /** Includes nearby cells so a position can match a road across a cell boundary. */
    private Set<HighwaySegment> findCandidates(double latitude, double longitude) {
        double latitudeRadius = toleranceMeters / 110000;
        double longitudeRadius = toleranceMeters
                / (110000 * Math.cos(Math.toRadians(latitude)));

        int minimumColumn = DataLoader.x(longitude - longitudeRadius);
        int maximumColumn = DataLoader.x(longitude + longitudeRadius);
        int minimumRow = DataLoader.y(latitude - latitudeRadius);
        int maximumRow = DataLoader.y(latitude + latitudeRadius);

        Set<HighwaySegment> candidates = new LinkedHashSet<>();

        for (int column = minimumColumn; column <= maximumColumn; column++) {
            for (int row = minimumRow; row <= maximumRow; row++) {
                String geohash = DataLoader.cell(column, row);
                List<HighwaySegment> cellCandidates = data.byGeohash()
                        .getOrDefault(geohash, List.of());
                candidates.addAll(cellCandidates);
            }
        }

        return candidates;
    }

    /** Approximates the length of a short road edge using its midpoint latitude. */
    static double length(HighwaySegment.Point start, HighwaySegment.Point end) {
        double midpointLatitude = (start.lat() + end.lat()) / 2;
        double horizontalMeters = (end.lon() - start.lon())
                * 111195 * Math.cos(Math.toRadians(midpointLatitude));
        double verticalMeters = (end.lat() - start.lat()) * 111195;

        return Math.hypot(horizontalMeters, verticalMeters);
    }
}
