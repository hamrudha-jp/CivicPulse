package com.civicpulse.complaint.model;

public enum Severity {
    LOW(2.0),
    MEDIUM(4.0),
    HIGH(7.0),
    CRITICAL(10.0);

    private final double baseScore;

    Severity(double baseScore) {
        this.baseScore = baseScore;
    }

    public double getBaseScore() {
        return baseScore;
    }
}
