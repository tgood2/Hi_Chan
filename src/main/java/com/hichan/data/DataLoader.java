package com.hichan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

//Reads preprocessed geometry and builds the in-memory geohash candidate index

public final class DataLoader {
    //Six geohash characters contain 15 longitude bits and 15 latitude bits
    static final double WIDTH = 360.0 / 32768;
    static final double HEIGHT = 180.0 / 32768;

    private final Path file;

    public DataLoader(Path file) {
        this.file = Objects.requireNonNull(file);
    }

    public LoadedData loadChannelData() throws IOException {
        try {
            Map<?, ?> root = Json.object(Json.parse(Files.readString(file)));
            double precision = Json.number(root.get("precision"));

            if (precision != 6) {
                throw new IllegalArgumentException("Runtime index requires precision 6");
            }

            List<HighwaySegment> segments = new ArrayList<>();
            Set<String> segmentIDs = new HashSet<>();
            Map<String, Set<HighwaySegment>> index = new HashMap<>();

            for (Object segmentValue : Json.array(root.get("segments"))) {
                Map<?, ?> segmentObject = Json.object(segmentValue);
                HighwaySegment segment = readSegment(segmentObject);

                if (!segmentIDs.add(segment.segmentID())) {
                    throw new IllegalArgumentException(
                            "Duplicate segmentID: " + segment.segmentID());
                }

                segments.add(segment);
                indexSegment(segment, index);
            }

            if (segments.isEmpty()) {
                throw new IllegalArgumentException("No segments");
            }

            // Freeze both the map and its candidate lists after loading.
            Map<String, List<HighwaySegment>> immutableIndex = new HashMap<>();
            for (Map.Entry<String, Set<HighwaySegment>> entry : index.entrySet()) {
                immutableIndex.put(entry.getKey(), List.copyOf(entry.getValue()));
            }

            return new LoadedData(
                    6,
                    List.copyOf(segments),
                    Map.copyOf(immutableIndex));
        } catch (IllegalArgumentException exception) {
            throw new IOException(
                    "Invalid lookup data: " + exception.getMessage(), exception);
        }
    }

    private HighwaySegment readSegment(Map<?, ?> segmentObject) {
        List<HighwaySegment.Point> points = new ArrayList<>();

        for (Object coordinateValue : Json.array(segmentObject.get("coordinates"))) {
            List<?> coordinatePair = Json.array(coordinateValue);

            if (coordinatePair.size() != 2) {
                throw new IllegalArgumentException("Expected coordinate pair");
            }

            // GeoJSON stores longitude first; the CLI accepts latitude first.
            double longitude = Json.number(coordinatePair.get(0));
            double latitude = Json.number(coordinatePair.get(1));
            points.add(new HighwaySegment.Point(longitude, latitude));
        }

        return new HighwaySegment(
                Json.string(segmentObject.get("segmentID")),
                Json.string(segmentObject.get("state")),
                Json.number(segmentObject.get("beginMile")),
                Json.number(segmentObject.get("endMile")),
                points);
    }

    private void indexSegment(
            HighwaySegment segment,
            Map<String, Set<HighwaySegment>> index) {
        List<HighwaySegment.Point> points = segment.points();

        for (int pointIndex = 1; pointIndex < points.size(); pointIndex++) {
            HighwaySegment.Point start = points.get(pointIndex - 1);
            HighwaySegment.Point end = points.get(pointIndex);

            int minimumColumn = x(Math.min(start.lon(), end.lon()));
            int maximumColumn = x(Math.max(start.lon(), end.lon()));
            int minimumRow = y(Math.min(start.lat(), end.lat()));
            int maximumRow = y(Math.max(start.lat(), end.lat()));

            long numberOfCells = (long) (maximumColumn - minimumColumn + 1)
                    * (maximumRow - minimumRow + 1);
            if (numberOfCells > 100000) {
                throw new IllegalArgumentException("Road edge too large for index");
            }

            // Index the whole bounding rectangle, including cells with no source vertex.
            // Distance checks later discard candidates that do not actually match the road.
            for (int column = minimumColumn; column <= maximumColumn; column++) {
                for (int row = minimumRow; row <= maximumRow; row++) {
                    String geohash = cell(column, row);
                    Set<HighwaySegment> cellSegments = index.get(geohash);

                    if (cellSegments == null) {
                        cellSegments = new LinkedHashSet<>();
                        index.put(geohash, cellSegments);
                    }

                    // A set prevents the same segment being added twice to one cell.
                    cellSegments.add(segment);
                }
            }
        }
    }

    //Returns the longitude column
    //+180 belongs to the final column
    static int x(double longitude) {
        return Math.min(32767, (int) Math.floor((longitude + 180) / WIDTH));
    }

    //Returns the latitude row
    //+90 belongs to the final row
    static int y(double latitude) {
        return Math.min(32767, (int) Math.floor((latitude + 90) / HEIGHT));
    }

    static String cell(int column, int row) {
        double centerLatitude = -90 + (row + 0.5) * HEIGHT;
        double centerLongitude = -180 + (column + 0.5) * WIDTH;

        return Geohash.encode(centerLatitude, centerLongitude, 6);
    }
}