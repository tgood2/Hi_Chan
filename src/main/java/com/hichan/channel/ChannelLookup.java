package com.hichan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

//Select a state/direction channel and remembers the previous valid assignment
public final class ChannelLookup {
    private record Rule(
            String state,
            double startMile,
            double endMile,
            int eastChannel,
            int westChannel) {
    }

    private final List<Rule> rules = new ArrayList<>();
    private Integer previousChannel;

    public ChannelLookup(Path csv) throws IOException {
        List<String> lines = Files.readAllLines(csv);
        String expectedHeader = "state,startMile,endMile,eastChannel,westChannel";

        if (lines.isEmpty() || !lines.get(0).equals(expectedHeader)) {
            throw new IOException("Invalid channel CSV header");
        }

        try {
            for (int lineIndex = 1; lineIndex < lines.size(); lineIndex++) {
                String line = lines.get(lineIndex).trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] fields = line.split(",", -1);
                if (fields.length != 5) {
                    throw new IllegalArgumentException("Expected five fields");
                }

                String state = fields[0].trim();
                double startMile = Double.parseDouble(fields[1]);
                double endMile = Double.parseDouble(fields[2]);
                int eastChannel = Integer.parseInt(fields[3]);
                int westChannel = Integer.parseInt(fields[4]);

                boolean validState = Set.of("WA", "ID").contains(state);
                boolean validRange = Double.isFinite(startMile)
                        && Double.isFinite(endMile)
                        && startMile >= 0
                        && endMile > startMile;
                boolean validChannels = eastChannel >= 1 && eastChannel <= 10
                        && westChannel >= 1 && westChannel <= 10;

                if (!validState || !validRange || !validChannels) {
                    throw new IllegalArgumentException("Invalid channel rule");
                }

                for (Rule existingRule : rules) {
                    boolean sameState = existingRule.state().equals(state);
                    boolean rangesOverlap = startMile < existingRule.endMile()
                            && endMile > existingRule.startMile();

                    if (sameState && rangesOverlap) {
                        throw new IllegalArgumentException("Overlapping channel ranges");
                    }
                }

                rules.add(new Rule(state, startMile, endMile, eastChannel, westChannel));
            }
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid channel CSV: " + exception.getMessage(), exception);
        }
    }

    public ChannelResult lookup(LocationResult location) {
        if (location == null) {
            throw new IllegalArgumentException("LocationResult required");
        }

        String direction = LocationProcessor.direction(location.direction());
        int channelNumber = -1;

        for (Rule rule : rules) {
            boolean sameState = rule.state().equals(location.state());
            boolean inRange = location.milePost() >= rule.startMile()
                    && location.milePost() < rule.endMile();

            if (sameState && inRange) {
                if (direction.equals("E")) {
                    channelNumber = rule.eastChannel();
                } else {
                    channelNumber = rule.westChannel();
                }
                break;
            }
        }

        // The first assignment is not a change. Missing data preserves the last valid one.
        boolean channelChanged = channelNumber != -1
                && previousChannel != null
                && previousChannel != channelNumber;

        if (channelNumber != -1) {
            previousChannel = channelNumber;
        }

        String segmentName = String.format(
                Locale.ROOT, "I-90 %s mile %.3f", location.state(), location.milePost());
        return new ChannelResult(channelNumber, segmentName, channelChanged, direction);
    }
}
