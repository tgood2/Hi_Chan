package com.hichan;

//Standard base32 geohash, longitude bit first
public final class Geohash {
    private static final String ALPHABET = "0123456789bcdefghjkmnpqrstuvwxyz";

    public static void validate(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be finite and between -90 and 90");
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be finite and between -180 and 180");
        }
    }

    public static String encode(double latitude, double longitude, int precision) {
        validate(latitude, longitude);
        if (precision < 1 || precision > 12) {
            throw new IllegalArgumentException("Precision must be 1 through 12");
        }
        // Axis zero is longitude; axis one is latitude. Bits alternate between them.
        double[] lowerBounds = {-180, -90};
        double[] upperBounds = {180, 90};
        double[] coordinates = {longitude, latitude};
        StringBuilder geohash = new StringBuilder();
        int characterBits = 0;

        for (int bitIndex = 0; bitIndex < precision * 5; bitIndex++) {
            int axis = bitIndex % 2;
            double middle = (lowerBounds[axis] + upperBounds[axis]) / 2;
            // Make room for the next bit, then narrow the selected coordinate range.
            characterBits <<= 1;
            if (coordinates[axis] >= middle) {
                characterBits |= 1;
                lowerBounds[axis] = middle;
            } else {
                upperBounds[axis] = middle;
            }
            // Five binary decisions select one character from the base32 alphabet.
            if (bitIndex % 5 == 4) {
                geohash.append(ALPHABET.charAt(characterBits));
                characterBits = 0;
            }
        }
        return geohash.toString();
    }
}