package com.civicpulse.engine.priority;

import com.civicpulse.complaint.model.Complaint;
import com.civicpulse.complaint.model.Severity;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class PriorityCalculationEngine {

    public static final double WEIGHT_SEVERITY = 0.45;
    public static final double WEIGHT_IMPACT = 0.30;
    public static final double WEIGHT_AGE = 0.25;

    /**
     * Calculates an objective priority score between 1.0 and 10.0.
     *
     * @param severity The severity enum
     * @param upvotes The number of citizen upvotes
     * @param mergedDuplicates The count of confirmed duplicate reports linked to this issue
     * @param createdAt The creation timestamp
     * @param slaHours The SLA allocated for this category in hours
     * @param now The evaluation timestamp
     * @return Clamped, rounded priority score in [1.0, 10.0]
     */
    public double calculateScore(
            Severity severity,
            int upvotes,
            int mergedDuplicates,
            Instant createdAt,
            int slaHours,
            Instant now
    ) {
        // 1. Severity Base Component S in [2.0, 10.0]
        double s = severity != null ? severity.getBaseScore() : Severity.MEDIUM.getBaseScore();

        // 2. Public Impact Component I in [1.0, 10.0]
        // Uses log10 to prevent upvote spamming from skewing priority unfairly
        double upvoteContribution = 2.5 * Math.log10(1.0 + Math.max(0, upvotes));
        double duplicateContribution = 1.5 * Math.max(0, mergedDuplicates);
        double i = Math.min(10.0, 1.0 + upvoteContribution + duplicateContribution);

        // 3. Waiting / Aging Penalty A in [0.0, 10.0]
        // As complaint approaches and passes SLA deadline, urgency grows
        long elapsedSeconds = Math.max(0, Duration.between(createdAt, now).getSeconds());
        double elapsedHours = elapsedSeconds / 3600.0;
        double effectiveSlaHours = Math.max(1.0, (double) slaHours);

        double slaProgressRatio = elapsedHours / effectiveSlaHours;
        double a = Math.min(10.0, slaProgressRatio * 5.0);

        // Weighted Composite Score
        double composite = (WEIGHT_SEVERITY * s) + (WEIGHT_IMPACT * i) + (WEIGHT_AGE * a);

        // Bound to [1.0, 10.0] and round to 2 decimal places
        double clamped = Math.max(1.0, Math.min(10.0, composite));
        return Math.round(clamped * 100.0) / 100.0;
    }

    public double calculateScore(Complaint complaint, Instant now) {
        int slaHours = (complaint.getCategory() != null && complaint.getCategory().getSlaHours() != null)
                ? complaint.getCategory().getSlaHours()
                : 48;

        return calculateScore(
                complaint.getSeverity(),
                complaint.getUpvotesCount() != null ? complaint.getUpvotesCount() : 0,
                complaint.getMergedDuplicatesCount() != null ? complaint.getMergedDuplicatesCount() : 0,
                complaint.getCreatedAt(),
                slaHours,
                now
        );
    }
}
