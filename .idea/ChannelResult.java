package com.highchan;

/**
 * Data design container that represents the output of channel lookup system
 */

public class ChannelResult {
    private final int channelNumber;
    private final String segmentName;
    private final boolean channelChanged;

    public ChannelResult(int channelNumber, String segmentName, boolean channelChanged) {
        this.channelChanged = channelChanged;
        this.channelNumber = channelNumber;
        this.segmentName = segmentName;
    }

    public int getChannelNumber() {
        return channelNumber;
    }

    public String getSegmentName() {
        return segmentName;
    }

    public boolean isChannelChanged() {
        return channelChanged;
    }
}