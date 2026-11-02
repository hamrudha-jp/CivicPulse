package com.civicpulse.analytics;

import com.civicpulse.analytics.dto.DashboardMetricsResponse;
import com.civicpulse.complaint.model.Complaint;
import com.civicpulse.complaint.model.ComplaintStatus;
import com.civicpulse.complaint.model.DuplicateMatchStatus;
import com.civicpulse.complaint.repository.ComplaintRepository;
import com.civicpulse.complaint.repository.DuplicateMatchRepository;
import com.civicpulse.department.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private final ComplaintRepository complaintRepository;
    private final DuplicateMatchRepository duplicateMatchRepository;
    private final DepartmentRepository departmentRepository;

    public AnalyticsService(
            ComplaintRepository complaintRepository,
            DuplicateMatchRepository duplicateMatchRepository,
            DepartmentRepository departmentRepository
    ) {
        this.complaintRepository = complaintRepository;
        this.duplicateMatchRepository = duplicateMatchRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public DashboardMetricsResponse getDashboardMetrics(Long deptId) {
        List<Complaint> allComplaints = deptId != null
                ? complaintRepository.findAll().stream().filter(c -> c.getDepartment().getId().equals(deptId)).toList()
                : complaintRepository.findAll();

        long total = allComplaints.size();
        long submitted = allComplaints.stream().filter(c -> c.getStatus() == ComplaintStatus.SUBMITTED).count();
        long underReview = allComplaints.stream().filter(c -> c.getStatus() == ComplaintStatus.UNDER_REVIEW).count();
        long assigned = allComplaints.stream().filter(c -> c.getStatus() == ComplaintStatus.ASSIGNED).count();
        long inProgress = allComplaints.stream().filter(c -> c.getStatus() == ComplaintStatus.IN_PROGRESS).count();
        long resolved = allComplaints.stream().filter(c -> c.getStatus() == ComplaintStatus.RESOLVED).count();
        long rejected = allComplaints.stream().filter(c -> c.getStatus() == ComplaintStatus.REJECTED).count();
        long duplicate = allComplaints.stream().filter(c -> c.getStatus() == ComplaintStatus.DUPLICATE).count();

        Instant now = Instant.now();
        long overdue = allComplaints.stream()
                .filter(c -> c.getStatus() != ComplaintStatus.RESOLVED
                        && c.getStatus() != ComplaintStatus.REJECTED
                        && c.getStatus() != ComplaintStatus.DUPLICATE
                        && c.getDueDate() != null
                        && c.getDueDate().isBefore(now))
                .count();

        long pendingDuplicates = deptId != null
                ? duplicateMatchRepository.findBySourceComplaintDepartmentIdAndStatus(deptId, DuplicateMatchStatus.PENDING_REVIEW).size()
                : duplicateMatchRepository.findByStatusOrderByCompositeConfidenceDesc(DuplicateMatchStatus.PENDING_REVIEW).size();

        double resolutionRate = total > 0 ? ((double) resolved / total) * 100.0 : 0.0;
        resolutionRate = Math.round(resolutionRate * 10.0) / 10.0;

        Map<String, Long> bySeverity = new HashMap<>();
        for (Complaint c : allComplaints) {
            if (c.getSeverity() != null) {
                bySeverity.merge(c.getSeverity().name(), 1L, Long::sum);
            }
        }

        Map<String, Long> byDept = new HashMap<>();
        for (Complaint c : allComplaints) {
            if (c.getDepartment() != null) {
                byDept.merge(c.getDepartment().getName(), 1L, Long::sum);
            }
        }

        return new DashboardMetricsResponse(
                total,
                submitted,
                underReview,
                assigned,
                inProgress,
                resolved,
                rejected,
                duplicate,
                overdue,
                pendingDuplicates,
                resolutionRate,
                bySeverity,
                byDept
        );
    }
}
