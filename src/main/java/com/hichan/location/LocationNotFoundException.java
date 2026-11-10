package com.hichan;

//Signals that no road geometry is close enough to given coordinates
public class LocationNotFoundException extends RuntimeException {
    public LocationNotFoundException(String message) {
        super(message);
    }
}