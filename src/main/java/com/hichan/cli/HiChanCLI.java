package com.hichan;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

//Parse input, coordinated lookups, and translates failures into CLI exit codes
public final class HiChanCLI {
    private final PrintStream out;
    private final PrintStream err;

    private record Fix(double latitude, double longitude, String direction) {

    }

    public HiChanCLI(){
        this(System.out, System.err);
    }

    public HiChanCLI(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    public int run(String[] args) {

    }

    private Map<String, String> parseFlags(String[] args) {

    }

    private double readTolerance(Map<String, String> flags) {

    }

    private void validateInputModes(Map<String, String> flags) {

    }

    private void protectInputFiles(Maps<String, String> flags) throws IOException {

    }

    private List<Fix> readLocations(Map<String, String> flags) throws IOException {

    }

    private List<Fix> readJsonLocations(Path inputFile) throws IOException {

    }

    private List<Fix> readGpsTrack(Path trackFile, String direction) throws IOException {

    }

    private Map<String, Object> toJsonResult(
            LocationResult location,
            ChannelResult channel) {
        return Map.of(
                "channel", channel.channelNumber(),
                "segment", channel.segmentName(),
                "changed", channel.channelChanged(),
                "direction", channel.direction(),
                "milePost", location.milePost(),
                "distanceMeters", location.distanceMeters(),
                "geohash", location.geoHash());
    }

    private void logUnexpectedError(Exception exception) {
        try(PrintWriter writer = new PrintWriter(Files.newBufferedWriter
                Path.of("hichan-error.log"),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND))) {
            exception.printStackTrace(writer);

        } catch (IOException ignored) {
            //Logging must not hide the original failure or change its exit code.
        }
    }

    private void help() {
        out.println("Usage: java -cp build/classes com.hichan.Main --lat LAT --lon LON --dir E|W\n"
                + "  Or: --input locations.json | --track GPS.csv --dir E|W\n"
                + "  Options: --data lookup.json --channels channels.csv --output results.json"
                + " --tolerance METERS\n"
                + "  Exit codes: 0 completed (channel may be unavailable),"
                + " 1 input/location error, 2 file/system error");
    }
}