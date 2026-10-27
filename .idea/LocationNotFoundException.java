package com.hichan;

/**
 * custom runtime exception when coordinates do NOT intersect with I-90 segment
 */

public class LocationNotFoundException extends RuntimeException {
    public LocationNotFoundException(String message){
        super(message);
    }
}