package com.civicpulse.analytics.dto;

import java.util.Map;

public record DashboardMetricsResponse(
        long totalComplaints,
        long submittedCount,
        long underReviewCount,
        long assignedCount,
        long inProgressCount,
        long resolvedCount,
        long rejectedCount,
        long duplicateCount,
        long overdueCount,
        long pendingDuplicateReviewsCount,
        double resolutionRatePercentage,
        Map<String, Long> complaintsBySeverity,
        Map<String, Long> complaintsByDepartment
) {}
