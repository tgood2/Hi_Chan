package com.hichan;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


//Upon creation should load in the preprocessed data in the struct
//
public final class DataLoader {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String GEOHASH_ALPHABET = "01234566789bcdefghjkmnpqrstuvwxyz";

    private final Path preprocessedJSONFile;

    public DataLoader(Path preprocessedJSONFile) {
        this.preprocessedJSONFile = Objects.requireNonNull(
                preprocessedJSONFile, "preprocessedJSONFile cannot be NULL");
    }



    public LoadedData loadChannelData() throws IOException {
        JsonNode root = JSON.readTree(preprocessedJSONFile.toFile());
        if(root == null || !root.isObject()){
            throw new IOException("Expected a JSON object at the file root");
        }

        int precision = requireInteger(root, "precision", "root");
        if(precision < 1 || precision > 12){
            throw new IOException("Geohash precision must be between 1 and 12");
        }

        JsonNode entries = root.get("segments");
        if(entries == null || !entries.isArray() || entries.isEmpty()){
            throw new IOException("segments must be a nonempty JSON array");
        }

        Map<String, HighwaySegment> byID = new LinkedHashMap<>();
        Map<String, List<HighwaySegment>> byGeohash = new HashMap<>();

        for(int = 0; i < entries.size(); i++){
            HighwaySegment segment = parseSegment(entries.get(i), precision, i);

            if(byId.putIfAbsent(segment.segmentId(), segment) != null){
                throw new IOException("Duplicate segmentId: " + segment.segmentId());
            }

            for(String geohash : segment.geohashes()){
                byGeohash.computeIfAbsent(geohash, unused -> new ArrayList<>()).add(segment);

            }
        }
        Map<String, List<H>>
    }
    //INPUTS: Latitude, Longitude, Precision (6)
    //OUTPUTS: A string containing the geohash for the given latitude and longitude
    //no need for input validation, this should be done in the preprocessing stage
    public string geohash(double lati, double longi, int precision){
        return string "Hello";
    }
}