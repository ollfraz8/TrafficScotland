package com.example.trafficscotland.utils;

import android.location.Location;

import com.example.trafficscotland.model.Situation;

public class DistanceUtils {

    private static final double METRES_PER_MILE = 1609.344;

    public static boolean isValidCoordinate(
            double latitude, double longitude) {

        return Double.isFinite(latitude)
                && Double.isFinite(longitude)
                && latitude >= -90 && latitude <= 90
                && longitude >= -180 && longitude <= 180
                && !(latitude == 0 && longitude == 0);
    }

    public static double distanceMiles(
            double startLat,
            double startLon,
            double endLat,
            double endLon) {

        float[] results = new float[1];

        Location.distanceBetween(
                startLat, startLon,
                endLat, endLon,
                results
        );

        return results[0] / METRES_PER_MILE;
    }

    public static boolean isWithinRadius(
            Situation situation,
            double latitude,
            double longitude,
            double radiusMiles) {

        // Check both ends of the affected road section.
        // A missing endpoint is ignored.

        if (isValidCoordinate(
                situation.getFromLatitude(),
                situation.getFromLongitude())) {

            if (distanceMiles(
                    latitude, longitude,
                    situation.getFromLatitude(),
                    situation.getFromLongitude()
            ) <= radiusMiles) {
                return true;
            }
        }

        if (isValidCoordinate(
                situation.getToLatitude(),
                situation.getToLongitude())) {

            return distanceMiles(
                    latitude, longitude,
                    situation.getToLatitude(),
                    situation.getToLongitude()
            ) <= radiusMiles;
        }

        return false;
    }
}
