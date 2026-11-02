package com.civicpulse.engine.duplicate;

import com.civicpulse.complaint.model.Complaint;
import com.civicpulse.complaint.model.DuplicateMatch;
import com.civicpulse.complaint.model.DuplicateMatchStatus;
import com.civicpulse.complaint.repository.ComplaintRepository;
import com.civicpulse.complaint.repository.DuplicateMatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DuplicateDetectionEngine {

    private static final Logger log = LoggerFactory.getLogger(DuplicateDetectionEngine.class);

    public static final double MAX_DISTANCE_THRESHOLD_METERS = 200.0;
    public static final double CONFIDENCE_THRESHOLD = 0.55;
    public static final double WEIGHT_SPATIAL = 0.40;
    public static final double WEIGHT_TEXT = 0.60;

    private final ComplaintRepository complaintRepository;
    private final DuplicateMatchRepository duplicateMatchRepository;
    private final SpatialDistanceService spatialDistanceService;
    private final TextSimilarityService textSimilarityService;

    public DuplicateDetectionEngine(
            ComplaintRepository complaintRepository,
            DuplicateMatchRepository duplicateMatchRepository,
            SpatialDistanceService spatialDistanceService,
            TextSimilarityService textSimilarityService
    ) {
        this.complaintRepository = complaintRepository;
        this.duplicateMatchRepository = duplicateMatchRepository;
        this.spatialDistanceService = spatialDistanceService;
        this.textSimilarityService = textSimilarityService;
    }

    /**
     * Scans active complaints within spatial threshold and calculates composite similarity.
     * Records candidate matches exceeding the confidence threshold for official review.
     *
     * @param target The newly submitted complaint
     */
    @Transactional
    public void scanAndFlagDuplicates(Complaint target) {
        if (target.getId() == null || target.getLatitude() == null || target.getLongitude() == null) {
            return;
        }

        SpatialDistanceService.BoundingBox bbox = spatialDistanceService.calculateBoundingBox(
                target.getLatitude(),
                target.getLongitude(),
                MAX_DISTANCE_THRESHOLD_METERS
        );

        List<Complaint> candidates = complaintRepository.findActiveComplaintsInBoundingBox(
                target.getId(),
                bbox.minLat(),
                bbox.maxLat(),
                bbox.minLon(),
                bbox.maxLon()
        );

        log.debug("Found {} potential spatial candidates for complaint ID: {}", candidates.size(), target.getId());

        for (Complaint candidate : candidates) {
            // Check if already matched
            if (duplicateMatchRepository.existsBySourceComplaintIdAndMatchedComplaintId(target.getId(), candidate.getId())) {
                continue;
            }

            double distanceMeters = spatialDistanceService.calculateDistanceMeters(
                    target.getLatitude(), target.getLongitude(),
                    candidate.getLatitude(), candidate.getLongitude()
            );

            if (distanceMeters <= MAX_DISTANCE_THRESHOLD_METERS) {
                double spatialScore = spatialDistanceService.calculateSpatialScore(distanceMeters, MAX_DISTANCE_THRESHOLD_METERS);
                double textScore = textSimilarityService.calculateSimilarity(
                        target.getTitle(), target.getDescription(),
                        candidate.getTitle(), candidate.getDescription()
                );

                // Category match bonus: if identical category, boost textual confidence
                if (target.getCategory() != null && candidate.getCategory() != null
                        && target.getCategory().getId().equals(candidate.getCategory().getId())) {
                    textScore = Math.min(1.0, textScore + 0.15);
                }

                double compositeConfidence = (WEIGHT_SPATIAL * spatialScore) + (WEIGHT_TEXT * textScore);
                compositeConfidence = Math.round(compositeConfidence * 100.0) / 100.0;

                if (compositeConfidence >= CONFIDENCE_THRESHOLD) {
                    log.info("Duplicate candidate flagged: Complaint #{} matches #{} (Confidence: {}, Distance: {}m)",
                            target.getTrackingNumber(), candidate.getTrackingNumber(), compositeConfidence, Math.round(distanceMeters));

                    DuplicateMatch match = DuplicateMatch.builder()
                            .sourceComplaint(target)
                            .matchedComplaint(candidate)
                            .distanceMeters(Math.round(distanceMeters * 10.0) / 10.0)
                            .textSimilarityScore(Math.round(textScore * 100.0) / 100.0)
                            .compositeConfidence(compositeConfidence)
                            .status(DuplicateMatchStatus.PENDING_REVIEW)
                            .build();

                    duplicateMatchRepository.save(match);
                }
            }
        }
    }
}
