package com.hichan;

import java.io.PrintStream;

/** Prints lookup results to stdout and channel-change warnings to stderr. */
public final class OutputManager {
    private final PrintStream out;
    private final PrintStream err;

    public OutputManager() {
        this(System.out, System.err);
    }

    public OutputManager(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    public void display(ChannelResult result) {
        if (result == null) {
            throw new IllegalArgumentException("ChannelResult required");
        }

        // A matched road may still have no configured channel assignment.
        if (result.channelNumber() == -1) {
            out.println("Channel information unavailable for this location");
            return;
        }

        String directionLabel;
        if (result.direction().equals("E")) {
            directionLabel = "Eastbound";
        } else {
            directionLabel = "Westbound";
        }

        out.printf(
                "Current Channel - %d (%s - %s)%n",
                result.channelNumber(),
                result.segmentName(),
                directionLabel);

        if (result.channelChanged()) {
            err.printf(
                    "WARNING: channel change detected. Switch to channel %d%n",
                    result.channelNumber());
        }
    }
}
