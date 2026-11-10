package com.hichan;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static com.hichan.Json.array;
import static com.hichan.Json.number;
import static com.hichan.Json.object;
import static com.hichan.Json.parse;
import static com.hichan.Json.write;

/**
 * Converts supplied GeoJSON to runtime schema. Does not invent radio channels.
 */
public final class I90Preprocessor {

    public static void convert(Path input, Path output) throws IOException {
        try {
            Map<?, ?> root = object(parse(Files.readString(input)));
            if (!"FeatureCollection".equals(root.get("type"))) {
                throw new IllegalArgumentException("Expected FeatureCollection");
            }
            List<Object> segments = new ArrayList<>();
            int segmentNumber = 0;
            for (Object feature : array(root.get("features"))) {
                Map<?, ?> featureObject = object(feature);
                // Support standard GeoJSON properties and the supplied flat feature format.
                Map<?, ?> properties;
                if (featureObject.containsKey("properties")) {
                    properties = object(featureObject.get("properties"));
                } else {
                    properties = featureObject;
                }
                if (!"I90".equals(properties.get("SIGN1"))) {
                    continue;
                }
                String stateCode = String.valueOf(properties.get("STFIPS"));
                String state;
                switch (stateCode) {
                    case "16", "16.0":
                        state = "ID";
                        break;
                    case "53", "53.0":
                        state = "WA";
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported STFIPS");
                }

                Map<?, ?> geometry = object(featureObject.get("geometry"));
                List<?> parts;
                if ("LineString".equals(geometry.get("type"))) {
                    parts = List.of(array(geometry.get("coordinates")));
                } else if ("MultiLineString".equals(geometry.get("type"))) {
                    parts = array(geometry.get("coordinates"));
                } else {
                    throw new IllegalArgumentException("Unsupported geometry");
                }
                // Each geometry part receives its own runtime segment ID.
                for (Object part : parts) {
                    List<?> coordinates = array(part);
                    List<List<Double>> normalized = new ArrayList<>();
                    for (Object pair : coordinates) {
                        List<?> coordinatePair = array(pair);
                        double longitude = number(coordinatePair.get(0));
                        double latitude = number(coordinatePair.get(1));
                        Geohash.validate(latitude, longitude);
                        normalized.add(List.of(longitude, latitude));
                    }
                    double beginMile = number(properties.get("BEGMP"));
                    double endMile = number(properties.get("ENDMP"));
                    if (normalized.size() < 2 || beginMile < 0 || endMile < beginMile) {
                        throw new IllegalArgumentException("Invalid geometry/mileposts");
                    }
                    segmentNumber++;
                    segments.add(Map.of(
                            "segmentID", state + "-" + segmentNumber,
                            "state", state,
                            "beginMile", beginMile,
                            "endMile", endMile,
                            "coordinates", normalized));
                }
            }
            if (segments.isEmpty()) {
                throw new IllegalArgumentException("No I90 geometry");
            }
            // Geohash candidate indexing happens when DataLoader reads this geometry.
            Files.writeString(output, write(Map.of("precision", 6, "segments", segments)));
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid GeoJSON: " + exception.getMessage(), exception);
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: I90Preprocessor input.json output.json");
            System.exit(1);
        }
        try {
            convert(Path.of(args[0]), Path.of(args[1]));
        } catch (IOException exception) {
            System.err.println(exception.getMessage());
            System.exit(2);
        }
    }
}
