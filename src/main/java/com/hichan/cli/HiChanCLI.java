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
        try {
            if (args.length == 1 && args[0].equals("--help")) {
                help();
                return 0;
            }

            Map<String, String> flags = parseFlags(args);
            double toleranceMeters = readTolerance(flags);
            validateInputModes(flags);
            protectInputFiles(flags);

            List<Fix> locations = readLocations(flags);
            if (locations.isEmpty()) {
                throw new IllegalArgumentException("Input contains no locations");
            }

            // Validate the entire batch before starting any lookup or producing results.
            for (Fix location : locations) {
                Geohash.validate(location.latitude(), location.longitude());
                LocationProcessor.direction(location.direction());
            }

            Path dataFile = Path.of(flags.getOrDefault("--data", "data/i90-lookup.json"));
            Path channelFile = Path.of(flags.getOrDefault("--channels", "data/channels.csv"));
            LoadedData data = new DataLoader(dataFile).loadChannelData();
            LocationProcessor processor = new LocationProcessor(data, toleranceMeters);
            ChannelLookup channelLookup = new ChannelLookup(channelFile);
            OutputManager display = new OutputManager(out, err);

            List<Object> results = new ArrayList<>();
            int exitCode = 0;

            // Reuse one lookup instance so channel-change state survives across a batch.
            for (Fix location : locations) {
                try {
                    LocationResult processedLocation = processor.process(
                            location.latitude(), location.longitude(), location.direction());
                    ChannelResult channelResult = channelLookup.lookup(processedLocation);
                    display.display(channelResult);
                    results.add(toJsonResult(processedLocation, channelResult));
                } catch (LocationNotFoundException exception) {
                    // One off-road point should not prevent the rest of a track being read.
                    err.println(exception.getMessage());
                    results.add(Map.of(
                            "error", exception.getMessage(),
                            "lat", location.latitude(),
                            "lon", location.longitude()));
                    exitCode = 1;
                }
            }

            if (flags.containsKey("--output")) {
                Path outputFile = Path.of(flags.get("--output"));
                Files.writeString(outputFile, Json.write(results) + System.lineSeparator());
            }

            return exitCode;
        } catch (IllegalArgumentException exception) {
            err.println(exception.getMessage());
            err.println("Use --help for usage instructions.");
            return 1;
        } catch (IOException exception) {
            err.println("File/data error: " + exception.getMessage());
            return 2;
        } catch (Exception exception) {
            logUnexpectedError(exception);
            err.println("System Error");
            return 2;
        }
    }

    private Map<String, String> parseFlags(String[] args) {
        Set<String> allowedFlags = Set.of(
                "--lat", "--lon", "--dir", "--data", "--channels",
                "--input", "--track", "--output", "--tolerance");
        Map<String, String> flags = new HashMap<>();

        // Each option consumes two arguments: its name and then its value.
        for (int argumentIndex = 0; argumentIndex < args.length; argumentIndex += 2) {
            String flag = args[argumentIndex];

            if (!allowedFlags.contains(flag)) {
                throw new IllegalArgumentException("Unknown flag: " + flag);
            }
            if (argumentIndex + 1 == args.length) {
                throw new IllegalArgumentException("Missing value for " + flag);
            }
            if (flags.putIfAbsent(flag, args[argumentIndex + 1]) != null) {
                throw new IllegalArgumentException("Duplicate flag: " + flag);
            }
        }

        if (args.length == 0) {
            throw new IllegalArgumentException("Location arguments required");
        }

        return flags;
    }

    private double readTolerance(Map<String, String> flags) {
        double toleranceMeters = Double.parseDouble(flags.getOrDefault("--tolerance", "150"));

        if (!Double.isFinite(toleranceMeters)
                || toleranceMeters <= 0
                || toleranceMeters > 1000) {
            throw new IllegalArgumentException("Tolerance must be 0 < meters <= 1000");
        }

        return toleranceMeters;
    }

    private void validateInputModes(Map<String, String> flags) {
        boolean hasJsonInput = flags.containsKey("--input");
        boolean hasTrackInput = flags.containsKey("--track");
        boolean hasCoordinates = flags.containsKey("--lat") || flags.containsKey("--lon");

        if (hasJsonInput && hasTrackInput) {
            throw new IllegalArgumentException("Choose --input or --track");
        }
        if ((hasJsonInput || hasTrackInput) && hasCoordinates) {
            throw new IllegalArgumentException("Batch input cannot be combined with --lat/--lon");
        }
        if (hasJsonInput && flags.containsKey("--dir")) {
            throw new IllegalArgumentException("JSON batch entries each supply dir");
        }
    }

    private void protectInputFiles(Map<String, String> flags) throws IOException {
        if (!flags.containsKey("--output")) {
            return;
        }

        Path outputFile = Path.of(flags.get("--output")).toAbsolutePath().normalize();
        List<String> inputFiles = new ArrayList<>(List.of(
                flags.getOrDefault("--data", "data/i90-lookup.json"),
                flags.getOrDefault("--channels", "data/channels.csv")));

        if (flags.containsKey("--input")) {
            inputFiles.add(flags.get("--input"));
        }
        if (flags.containsKey("--track")) {
            inputFiles.add(flags.get("--track"));
        }

        for (String inputFile : inputFiles) {
            Path inputPath = Path.of(inputFile).toAbsolutePath().normalize();
            boolean samePath = outputFile.equals(inputPath);
            boolean sameExistingFile = Files.exists(outputFile)
                    && Files.exists(inputPath)
                    && Files.isSameFile(outputFile, inputPath);

            // isSameFile also catches links that point at an input under another name.
            if (samePath || sameExistingFile) {
                throw new IllegalArgumentException("Output must differ from input files");
            }
        }
    }

    private List<Fix> readLocations(Map<String, String> flags) throws IOException {
        if (flags.containsKey("--input")) {
            return readJsonLocations(Path.of(flags.get("--input")));
        }

        if (flags.containsKey("--track")) {
            String direction = LocationProcessor.direction(flags.get("--dir"));
            return readGpsTrack(Path.of(flags.get("--track")), direction);
        }

        if (!flags.keySet().containsAll(Set.of("--lat", "--lon", "--dir"))) {
            throw new IllegalArgumentException("--lat, --lon and --dir required");
        }

        double latitude = Double.parseDouble(flags.get("--lat"));
        double longitude = Double.parseDouble(flags.get("--lon"));
        Fix location = new Fix(latitude, longitude, flags.get("--dir"));
        return List.of(location);
    }

    private List<Fix> readJsonLocations(Path inputFile) throws IOException {
        List<Fix> locations = new ArrayList<>();

        try {
            Object parsedInput = Json.parse(Files.readString(inputFile));

            for (Object locationValue : Json.array(parsedInput)) {
                Map<?, ?> locationObject = Json.object(locationValue);
                double latitude = Json.number(locationObject.get("lat"));
                double longitude = Json.number(locationObject.get("lon"));
                String direction = Json.string(locationObject.get("dir"));
                locations.add(new Fix(latitude, longitude, direction));
            }
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid batch JSON: " + exception.getMessage(), exception);
        }

        return locations;
    }

    private List<Fix> readGpsTrack(Path trackFile, String direction) throws IOException {
        List<String> lines = Files.readAllLines(trackFile);
        if (lines.isEmpty() || !lines.get(0).startsWith("date time,latitude,longitude,")) {
            throw new IOException("Invalid GPS CSV header");
        }

        List<Fix> locations = new ArrayList<>();
        try {
            // Row zero is the header; columns one and two contain latitude and longitude.
            for (int lineIndex = 1; lineIndex < lines.size(); lineIndex++) {
                String line = lines.get(lineIndex);
                if (line.isBlank()) {
                    continue;
                }

                String[] fields = line.split(",", -1);
                double latitude = Double.parseDouble(fields[1]);
                double longitude = Double.parseDouble(fields[2]);
                locations.add(new Fix(latitude, longitude, direction));
            }
        } catch (RuntimeException exception) {
            throw new IOException("Invalid GPS CSV record", exception);
        }

        return locations;
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
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(
                Path.of("hichan-error.log"),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND))) {
            exception.printStackTrace(writer);
        } catch (IOException ignored) {
            // Logging must not hide the original failure or change its exit code.
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
