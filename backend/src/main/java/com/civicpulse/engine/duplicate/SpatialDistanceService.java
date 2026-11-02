package com.civicpulse.engine.duplicate;

import org.springframework.stereotype.Component;

@Component
public class SpatialDistanceService {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    /**
     * Calculates the great-circle distance between two geographic points using the Haversine formula.
     *
     * @param lat1 Latitude of first point in decimal degrees
     * @param lon1 Longitude of first point in decimal degrees
     * @param lat2 Latitude of second point in decimal degrees
     * @param lon2 Longitude of second point in decimal degrees
     * @return Distance in meters
     */
    public double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(rLat1) * Math.cos(rLat2)
                * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));

        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Computes a normalized spatial similarity score between 0.0 and 1.0.
     *
     * @param distanceMeters Distance in meters
     * @param maxRadiusMeters Threshold radius (e.g. 200 meters)
     * @return Score where 1.0 is exact same location, decaying to 0.0 at maxRadiusMeters
     */
    public double calculateSpatialScore(double distanceMeters, double maxRadiusMeters) {
        if (distanceMeters >= maxRadiusMeters) {
            return 0.0;
        }
        return 1.0 - (distanceMeters / maxRadiusMeters);
    }

    public record BoundingBox(double minLat, double maxLat, double minLon, double maxLon) {}

    /**
     * Computes a rough geographic bounding box around a center point for efficient SQL indexing.
     */
    public BoundingBox calculateBoundingBox(double lat, double lon, double radiusMeters) {
        // 1 deg latitude ~ 111,000 meters
        double latDelta = radiusMeters / 111_000.0;
        // 1 deg longitude ~ 111,000 * cos(lat) meters
        double lonDelta = radiusMeters / (111_000.0 * Math.cos(Math.toRadians(lat)));

        return new BoundingBox(
                lat - latDelta,
                lat + latDelta,
                lon - Math.abs(lonDelta),
                lon + Math.abs(lonDelta)
        );
    }
}
