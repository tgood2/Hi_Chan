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

    }

    public ChannelResult lookup(LocationResult location) {

    }
}