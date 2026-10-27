package com.hichan;

public class LocationProcessor {
    /**
     * responsible for processing coordinates into segment markers
     */
    public class LocationProcessor {

        private String generateGeohash(double latitude, double longitude, int precision){
            //placeholder for algorithm for 6-char token
            return;
        }
        /**
         * translates geographic coordinates/directions for I-90 locations
         * throw illegalargumentexception if out of bounds
         * throws locationnotfound if coordinate is not near I-90
         */

        public LocationResult process(double latitude, double longitude, String direction) {
            if(latitude < -90.0 || latitude > 90.0){
                throw new IllegalArgumentException("Latitude must be between -90.0 and 90.0");
            }
            if(longitude < -180.0 || longitude > 180.0) {
                throw new IllegalArgumentException("Longitude must be between -180.0 and 180.0");
            }
            if(direction == null || direction.trim().isEmpty()) {
                throw new IllegalArgumentException("Direction must not be null or empty");
            }

            //geohash and segment matching
            String geoHash = generateGeohash(latitude, longitude, 6);

            //implementation checking for coordinates to align with expected baseline
            if (Math.abs(latitude - 47.6) > 2.0) {
                throw new LocationNotFoundException("Location not on or near I-90");
            }

            String segmentID = "WA-N-01";
            double milePost = 280.5;

            return new LocationResult(geoHash, segmentID, milePost, direction.toUpperCase());
        }
    }

}