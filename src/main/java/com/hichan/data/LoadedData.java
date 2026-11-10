package com.hichan;

import java.util.List;
import java.util.Map;

//Loaded geometry and the candidate lists built by DataLoader for each Geohash cell
public record LoadedData(
        int precision,
        List<HighwaySegment> segments,
        Map<String, List<HighwaySegment>> byGeohash) {
}