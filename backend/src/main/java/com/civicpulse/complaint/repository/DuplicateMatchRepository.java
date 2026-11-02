package com.civicpulse.complaint.repository;

import com.civicpulse.complaint.model.DuplicateMatch;
import com.civicpulse.complaint.model.DuplicateMatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DuplicateMatchRepository extends JpaRepository<DuplicateMatch, Long> {
    List<DuplicateMatch> findByStatusOrderByCompositeConfidenceDesc(DuplicateMatchStatus status);
    List<DuplicateMatch> findBySourceComplaintDepartmentIdAndStatus(Long departmentId, DuplicateMatchStatus status);
    boolean existsBySourceComplaintIdAndMatchedComplaintId(Long sourceId, Long matchedId);
}
