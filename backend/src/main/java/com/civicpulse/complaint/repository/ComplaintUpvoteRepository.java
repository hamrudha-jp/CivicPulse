package com.civicpulse.complaint.repository;

import com.civicpulse.complaint.model.ComplaintUpvote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComplaintUpvoteRepository extends JpaRepository<ComplaintUpvote, Long> {
    boolean existsByComplaintIdAndCitizenId(Long complaintId, Long citizenId);
    Optional<ComplaintUpvote> findByComplaintIdAndCitizenId(Long complaintId, Long citizenId);
    long countByComplaintId(Long complaintId);
}
