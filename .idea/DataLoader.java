package com.hichan;

//Upon creation should load in the preprocessed data in the struct
//
public class DataLoader {
    private final String preprocessedJSONFile;

    //1.Propogate through the preprocessed JSON input file
    //2.Lati and longi should be readily accessible
    //3.Run geohash to receive string with location information
    //4.The JSON to geohash processor associates the geohash with the location data
    public void loadChannelData(string preprocessedJSONFile){

    }
    //INPUTS: Latitude, Longitude, Precision (6)
    //OUTPUTS: A string containing the geohash for the given latitude and longitude
    //no need for input validation, this should be done in the preprocessing stage
    public string geohash(double lati, double longi, int precision){

    }
    //
    public string
}