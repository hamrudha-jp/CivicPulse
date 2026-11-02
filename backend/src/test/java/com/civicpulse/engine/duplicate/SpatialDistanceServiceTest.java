package com.civicpulse.engine.duplicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpatialDistanceServiceTest {

    private SpatialDistanceService service;

    @BeforeEach
    void setUp() {
        service = new SpatialDistanceService();
    }

    @Test
    @DisplayName("Identical coordinates should yield 0 meters distance and 1.0 spatial score")
    void testIdenticalCoordinates() {
        double dist = service.calculateDistanceMeters(12.9716, 77.5946, 12.9716, 77.5946);
        assertEquals(0.0, dist, 0.001);

        double score = service.calculateSpatialScore(dist, 200.0);
        assertEquals(1.0, score, 0.001);
    }

    @Test
    @DisplayName("Nearby points within 200m should yield accurate Haversine distance and positive score")
    void testNearbyCoordinates() {
        // Point 1: 12.9716, 77.5946
        // Point 2: 12.9719, 77.5948 (~39 meters apart)
        double dist = service.calculateDistanceMeters(12.9716, 77.5946, 12.9719, 77.5948);
        assertTrue(dist > 30.0 && dist < 50.0, "Distance should be approximately 39 meters");

        double score = service.calculateSpatialScore(dist, 200.0);
        assertTrue(score > 0.75, "Score should be higher than 0.75 for points under 50m apart");
    }

    @Test
    @DisplayName("Points farther than threshold radius should yield 0.0 spatial score")
    void testFarCoordinates() {
        double dist = service.calculateDistanceMeters(12.9716, 77.5946, 12.9850, 77.6200);
        assertTrue(dist > 2000.0, "Points in different neighborhoods should be > 2000m apart");

        double score = service.calculateSpatialScore(dist, 200.0);
        assertEquals(0.0, score, 0.001);
    }
}
