package com.civicpulse.engine.priority;

import com.civicpulse.complaint.model.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PriorityCalculationEngineTest {

    private PriorityCalculationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new PriorityCalculationEngine();
    }

    @Test
    @DisplayName("Critical severity with zero upvotes should have high base score")
    void testCriticalBaseScore() {
        Instant now = Instant.now();
        double score = engine.calculateScore(Severity.CRITICAL, 0, 0, now, 24, now);

        // Critical S=10 * 0.45 = 4.5. Impact I=1 * 0.30 = 0.30. Age A=0. Total = 4.80
        assertEquals(4.80, score, 0.05);
    }

    @Test
    @DisplayName("Upvotes should increase impact score logarithmically without breaking bounds")
    void testUpvoteImpactScaling() {
        Instant now = Instant.now();
        double scoreNoUpvotes = engine.calculateScore(Severity.MEDIUM, 0, 0, now, 48, now);
        double score10Upvotes = engine.calculateScore(Severity.MEDIUM, 10, 0, now, 48, now);
        double score100Upvotes = engine.calculateScore(Severity.MEDIUM, 100, 0, now, 48, now);

        assertTrue(score10Upvotes > scoreNoUpvotes, "Upvotes must increase the score");
        assertTrue(score100Upvotes > score10Upvotes, "More upvotes must yield higher priority");
        assertTrue(score100Upvotes <= 10.0, "Score must never exceed 10.0");
    }

    @Test
    @DisplayName("Overdue complaints should receive maximum aging penalty")
    void testAgingPenalty() {
        Instant now = Instant.now();
        Instant createdFresh = now;
        Instant createdOverdue = now.minus(Duration.ofHours(48)); // 2x SLA of 24h

        double scoreFresh = engine.calculateScore(Severity.HIGH, 0, 0, createdFresh, 24, now);
        double scoreOverdue = engine.calculateScore(Severity.HIGH, 0, 0, createdOverdue, 24, now);

        assertTrue(scoreOverdue > scoreFresh, "Overdue complaint score must exceed fresh complaint score");
        assertEquals(2.50, scoreOverdue - scoreFresh, 0.05, "Aging penalty difference at 200% SLA must contribute max age weight");
    }

    @Test
    @DisplayName("Score must remain clamped between 1.0 and 10.0 across extreme inputs")
    void testScoreBounds() {
        Instant now = Instant.now();
        double minScore = engine.calculateScore(Severity.LOW, 0, 0, now, 120, now);
        double maxScore = engine.calculateScore(Severity.CRITICAL, 10000, 10, now.minus(Duration.ofDays(30)), 12, now);

        assertTrue(minScore >= 1.0, "Score must be >= 1.0");
        assertTrue(maxScore <= 10.0, "Score must be <= 10.0");
    }
}
