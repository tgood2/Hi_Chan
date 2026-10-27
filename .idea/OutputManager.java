package com.hichan;

/**
 * handling visual terminal console logs and optional storage exports
 */

public class OutputManager {

    /**
     * format and log the current metrics for the system
     */
    public void display(ChannelResult channelResult){
        //preconditions
        if(channelResult == null) {
            throw new IllegalArgumentException("Input ChannelResult object must NOT be null");
        }

        //missing channel
        if(channelResult.getChannelNumber() == -1) {
            System.out.println("Channel information unavailable for this location");
            return;
        }

        //print standard stdout
        System.out.printf("Current Channel - %d (%s - Northbound)%n",
                channelResult.getChannelNumber(),
                channelResult.getSegmentName());

        //print channel change alert warnings
        if(channelResult.isChannelChanged()) {
            System.err.printf("WARNING: Channel Change Detected! Switch to channel %d%n"),
            channelResult.getChannelNumber());
        }
    }
}