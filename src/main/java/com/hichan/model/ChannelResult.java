package com.hichan;

//A channel assignment; channelNumber is -1 when no assignment is configured
public record ChannelResult(
        int channelNumber,
        String segmentName,
        boolean channelChanged,
        String direction) {

    // These getters preserve the original interface used by the project's early classes.
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
