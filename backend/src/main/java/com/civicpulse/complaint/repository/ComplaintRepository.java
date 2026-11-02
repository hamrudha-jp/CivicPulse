package com.civicpulse.complaint.repository;

import com.civicpulse.complaint.model.Complaint;
import com.civicpulse.complaint.model.ComplaintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long>, JpaSpecificationExecutor<Complaint> {

    @EntityGraph(attributePaths = {"department", "category", "citizen"})
    Optional<Complaint> findByTrackingNumber(String trackingNumber);

    @Override
    @EntityGraph(attributePaths = {"department", "category", "citizen"})
    Optional<Complaint> findById(Long id);

    Page<Complaint> findByCitizenId(Long citizenId, Pageable pageable);

    Page<Complaint> findByDepartmentId(Long departmentId, Pageable pageable);

    Page<Complaint> findByDepartmentIdAndStatusIn(Long departmentId, Collection<ComplaintStatus> statuses, Pageable pageable);

    // Candidates for spatial duplicate checking:
    // Complaints in same department/category within rough lat/lng bounding box that are active
    @Query("""
        SELECT c FROM Complaint c
        WHERE c.id != :excludeId
        AND c.status NOT IN ('RESOLVED', 'REJECTED', 'DUPLICATE', 'CANCELLED')
        AND c.latitude BETWEEN :minLat AND :maxLat
        AND c.longitude BETWEEN :minLon AND :maxLon
    """)
    List<Complaint> findActiveComplaintsInBoundingBox(
            @Param("excludeId") Long excludeId,
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon,
            @Param("maxLon") Double maxLon
    );

    // Unescalated overdue complaints
    @Query("""
        SELECT c FROM Complaint c
        WHERE c.status NOT IN ('RESOLVED', 'REJECTED', 'DUPLICATE', 'CANCELLED')
        AND c.dueDate < :now
        AND c.isEscalated = false
    """)
    List<Complaint> findOverdueUnescalatedComplaints(@Param("now") Instant now);

    // Active complaints requiring periodic priority score updates (due to aging)
    @Query("""
        SELECT c FROM Complaint c
        WHERE c.status NOT IN ('RESOLVED', 'REJECTED', 'DUPLICATE', 'CANCELLED')
    """)
    List<Complaint> findAllActiveComplaints();

    // Dashboard count aggregations
    long countByStatus(ComplaintStatus status);

    long countByDepartmentIdAndStatus(Long departmentId, ComplaintStatus status);

    long countByDepartmentId(Long departmentId);

    @Query("""
        SELECT COUNT(c) FROM Complaint c
        WHERE c.status NOT IN ('RESOLVED', 'REJECTED', 'DUPLICATE', 'CANCELLED')
        AND c.dueDate < :now
    """)
    long countOverdueComplaints(@Param("now") Instant now);

    @Query("""
        SELECT COUNT(c) FROM Complaint c
        WHERE c.department.id = :departmentId
        AND c.status NOT IN ('RESOLVED', 'REJECTED', 'DUPLICATE', 'CANCELLED')
        AND c.dueDate < :now
    """)
    long countOverdueComplaintsByDepartment(@Param("departmentId") Long departmentId, @Param("now") Instant now);
}
