package com.hichan;

public class ChannelLookup {
    private int previousChannel = -1;

    /**
     * references map definitions for local truck channels
     */

    public ChannelResult lookup(LocationResult locationResult){
        //preconditions
        if(locationResult == null){
            throw new IllegalArgumentException("Input Location result object must NOT be null");
        }

        //lookup logic
        String segmentID = locationResult.getSegmentID();
        int channelNumber;
        String segmentName;


    }
}