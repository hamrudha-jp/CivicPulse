package com.civicpulse.engine.duplicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextSimilarityServiceTest {

    private TextSimilarityService service;

    @BeforeEach
    void setUp() {
        service = new TextSimilarityService();
    }

    @Test
    @DisplayName("Identical texts should yield 1.0 similarity")
    void testIdenticalText() {
        String title = "Severe crater-sized pothole on MG Road";
        String desc = "Deep pothole causing sudden braking";
        double sim = service.calculateSimilarity(title, desc, title, desc);
        assertEquals(1.0, sim, 0.001);
    }

    @Test
    @DisplayName("Phrasing variations of same issue should yield high textual similarity")
    void testSimilarText() {
        String title1 = "Severe crater-sized pothole on MG Road near Metro Pillar 142";
        String desc1 = "Deep pothole causing sudden braking and two-wheeler accidents during evening peak hours.";

        String title2 = "Huge dangerous pothole near Trinity Metro Pillar 142";
        String desc2 = "Cars are swerving sharply to avoid a 1-foot deep pothole on MG Road.";

        double sim = service.calculateSimilarity(title1, desc1, title2, desc2);
        assertTrue(sim >= 0.35, "Similarity between phrasing variations of same issue must exceed 0.35 (actual: " + sim + ")");
        assertTrue(sim <= 0.90, "Phrasing variations should not be falsely marked as 100% identical");
    }

    @Test
    @DisplayName("Completely unrelated civic issues should yield very low similarity")
    void testUnrelatedText() {
        String title1 = "Deep pothole on MG Road";
        String desc1 = "Asphalt broken and cars getting flat tires";

        String title2 = "Streetlight blackout on 10th Main";
        String desc2 = "Entire residential stretch in darkness at night";

        double sim = service.calculateSimilarity(title1, desc1, title2, desc2);
        assertTrue(sim < 0.20, "Unrelated issues must have low similarity score");
    }
}
