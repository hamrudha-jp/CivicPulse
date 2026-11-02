package com.civicpulse.complaint.model;

import com.civicpulse.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "duplicate_matches", uniqueConstraints = {
        @UniqueConstraint(name = "uk_duplicate_source_target", columnNames = {"source_complaint_id", "matched_complaint_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DuplicateMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_complaint_id", nullable = false)
    private Complaint sourceComplaint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matched_complaint_id", nullable = false)
    private Complaint matchedComplaint;

    @Column(name = "distance_meters", nullable = false)
    private Double distanceMeters;

    @Column(name = "text_similarity_score", nullable = false)
    private Double textSimilarityScore;

    @Column(name = "composite_confidence", nullable = false)
    private Double compositeConfidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private DuplicateMatchStatus status = DuplicateMatchStatus.PENDING_REVIEW;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedByUser;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
