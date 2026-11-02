package com.civicpulse.complaint.service;

import com.civicpulse.common.exception.BadRequestException;
import com.civicpulse.common.exception.ResourceNotFoundException;
import com.civicpulse.common.exception.UnauthorizedActionException;
import com.civicpulse.complaint.dto.ComplaintDtos.*;
import com.civicpulse.complaint.model.*;
import com.civicpulse.complaint.repository.ComplaintAttachmentRepository;
import com.civicpulse.complaint.repository.ComplaintRepository;
import com.civicpulse.complaint.repository.ComplaintStatusHistoryRepository;
import com.civicpulse.complaint.repository.DuplicateMatchRepository;
import com.civicpulse.engine.priority.PriorityCalculationEngine;
import com.civicpulse.security.UserPrincipal;
import com.civicpulse.storage.FileStorageService;
import com.civicpulse.user.Role;
import com.civicpulse.user.User;
import com.civicpulse.user.UserRepository;
import com.civicpulse.user.dto.UserDtos.UserProfileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@Service
public class OfficialComplaintService {

    private static final Logger log = LoggerFactory.getLogger(OfficialComplaintService.class);

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final ComplaintStatusHistoryRepository historyRepository;
    private final ComplaintAttachmentRepository attachmentRepository;
    private final DuplicateMatchRepository duplicateMatchRepository;
    private final PriorityCalculationEngine priorityEngine;
    private final FileStorageService fileStorageService;
    private final ComplaintService complaintService;

    public OfficialComplaintService(
            ComplaintRepository complaintRepository,
            UserRepository userRepository,
            ComplaintStatusHistoryRepository historyRepository,
            ComplaintAttachmentRepository attachmentRepository,
            DuplicateMatchRepository duplicateMatchRepository,
            PriorityCalculationEngine priorityEngine,
            FileStorageService fileStorageService,
            ComplaintService complaintService
    ) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
        this.attachmentRepository = attachmentRepository;
        this.duplicateMatchRepository = duplicateMatchRepository;
        this.priorityEngine = priorityEngine;
        this.fileStorageService = fileStorageService;
        this.complaintService = complaintService;
    }

    @Transactional(readOnly = true)
    public Page<ComplaintSummaryResponse> getOfficialQueue(
            Long deptId,
            ComplaintStatus status,
            Severity severity,
            Boolean isEscalated,
            Pageable pageable,
            UserPrincipal principal
    ) {
        Long effectiveDeptId = deptId;
        if (principal != null && principal.role() == Role.ROLE_OFFICIAL) {
            if (principal.departmentId() == null) {
                throw new UnauthorizedActionException("Official is not associated with any municipal department");
            }
            effectiveDeptId = principal.departmentId();
        }

        Specification<Complaint> spec = Specification.where(null);

        if (effectiveDeptId != null) {
            final Long targetDeptId = effectiveDeptId;
            spec = spec.and((root, query, cb) -> cb.equal(root.get("department").get("id"), targetDeptId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (severity != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("severity"), severity));
        }
        if (isEscalated != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isEscalated"), isEscalated));
        }

        return complaintRepository.findAll(spec, pageable)
                .map(complaintService::mapToSummaryResponse);
    }

    @Transactional
    public ComplaintResponse assignComplaint(Long complaintId, Long officialId, UserPrincipal assigner) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", complaintId));

        validateDepartmentAccess(complaint, assigner);

        User official = userRepository.findById(officialId)
                .orElseThrow(() -> new ResourceNotFoundException("Official", "id", officialId));

        if (official.getRole() == Role.ROLE_CITIZEN) {
            throw new BadRequestException("Complaints cannot be assigned to citizens");
        }

        // Target official must belong to the same department as the complaint (unless assigner is admin)
        if (assigner.role() != Role.ROLE_ADMIN && !official.getDepartment().getId().equals(complaint.getDepartment().getId())) {
            throw new BadRequestException("Cannot assign complaint to an official from a different department");
        }

        User actingUser = userRepository.findById(assigner.id()).orElse(null);

        complaint.setAssignedOfficial(official);
        if (complaint.getStatus() == ComplaintStatus.SUBMITTED || complaint.getStatus() == ComplaintStatus.UNDER_REVIEW) {
            complaint.setStatus(ComplaintStatus.ASSIGNED);
        }

        ComplaintStatusHistory history = ComplaintStatusHistory.builder()
                .complaint(complaint)
                .changedByUser(actingUser)
                .fromStatus(complaint.getStatus())
                .toStatus(complaint.getStatus())
                .remarks("Assigned to official: " + official.getFullName())
                .build();
        historyRepository.save(history);

        return complaintService.mapToComplaintResponse(complaintRepository.save(complaint));
    }

    @Transactional
    public ComplaintResponse updateStatus(Long complaintId, UpdateStatusRequest request, UserPrincipal official) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", complaintId));

        validateDepartmentAccess(complaint, official);

        ComplaintStatus oldStatus = complaint.getStatus();
        ComplaintStatus newStatus = request.status();

        if (oldStatus == ComplaintStatus.RESOLVED && newStatus != ComplaintStatus.UNDER_REVIEW) {
            throw new BadRequestException("Resolved complaints can only be reopened to UNDER_REVIEW");
        }

        User actingUser = userRepository.findById(official.id()).orElse(null);

        complaint.setStatus(newStatus);

        ComplaintStatusHistory history = ComplaintStatusHistory.builder()
                .complaint(complaint)
                .changedByUser(actingUser)
                .fromStatus(oldStatus)
                .toStatus(newStatus)
                .remarks(request.remarks() != null ? request.remarks().trim() : "Status updated to " + newStatus)
                .build();
        historyRepository.save(history);

        return complaintService.mapToComplaintResponse(complaintRepository.save(complaint));
    }

    @Transactional
    public ComplaintResponse resolveComplaint(
            Long complaintId,
            ResolveComplaintRequest request,
            MultipartFile proofPhoto,
            UserPrincipal official
    ) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", complaintId));

        validateDepartmentAccess(complaint, official);

        Instant now = Instant.now();
        User actingUser = userRepository.findById(official.id()).orElse(null);

        complaint.setStatus(ComplaintStatus.RESOLVED);
        complaint.setResolvedAt(now);
        complaint.setResolutionRemarks(request.remarks().trim());

        ComplaintStatusHistory history = ComplaintStatusHistory.builder()
                .complaint(complaint)
                .changedByUser(actingUser)
                .fromStatus(complaint.getStatus())
                .toStatus(ComplaintStatus.RESOLVED)
                .remarks("Resolution: " + request.remarks().trim())
                .createdAt(now)
                .build();
        historyRepository.save(history);

        if (proofPhoto != null && !proofPhoto.isEmpty()) {
            String fileUrl = fileStorageService.storeFile(proofPhoto);
            ComplaintAttachment attachment = ComplaintAttachment.builder()
                    .complaint(complaint)
                    .fileName(proofPhoto.getOriginalFilename())
                    .fileUrl(fileUrl)
                    .fileType(proofPhoto.getContentType())
                    .fileSizeBytes(proofPhoto.getSize())
                    .attachmentType(AttachmentType.RESOLUTION_PROOF)
                    .uploadedAt(now)
                    .build();
            attachmentRepository.save(attachment);
        }

        return complaintService.mapToComplaintResponse(complaintRepository.save(complaint));
    }

    @Transactional(readOnly = true)
    public List<DuplicateMatchDto> getPendingDuplicates(Long deptId, UserPrincipal principal) {
        Long effectiveDeptId = deptId;
        if (principal != null && principal.role() == Role.ROLE_OFFICIAL) {
            if (principal.departmentId() == null) {
                throw new UnauthorizedActionException("Official is not associated with any municipal department");
            }
            effectiveDeptId = principal.departmentId();
        }

        List<DuplicateMatch> matches;
        if (effectiveDeptId != null) {
            matches = duplicateMatchRepository.findBySourceComplaintDepartmentIdAndStatus(effectiveDeptId, DuplicateMatchStatus.PENDING_REVIEW);
        } else {
            matches = duplicateMatchRepository.findByStatusOrderByCompositeConfidenceDesc(DuplicateMatchStatus.PENDING_REVIEW);
        }

        return matches.stream().map(this::mapToDuplicateMatchDto).toList();
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAssignmentTargets(Long deptId, UserPrincipal principal) {
        Long effectiveDeptId = deptId;
        if (principal != null && principal.role() == Role.ROLE_OFFICIAL) {
            if (principal.departmentId() == null) {
                throw new UnauthorizedActionException("Official is not associated with any municipal department");
            }
            effectiveDeptId = principal.departmentId();
        }

        List<User> officials = effectiveDeptId != null
                ? userRepository.findByDepartmentId(effectiveDeptId)
                : userRepository.findByRole(Role.ROLE_OFFICIAL);

        return officials.stream()
                .filter(u -> u.getRole() == Role.ROLE_OFFICIAL && Boolean.TRUE.equals(u.getIsActive()))
                .map(u -> new UserProfileResponse(
                        u.getId(),
                        u.getEmail(),
                        u.getFullName(),
                        u.getPhoneNumber(),
                        u.getRole().name(),
                        u.getDepartment() != null ? u.getDepartment().getId() : null,
                        u.getDepartment() != null ? u.getDepartment().getName() : null
                ))
                .toList();
    }

    @Transactional
    public void reviewDuplicate(Long matchId, ReviewDuplicateRequest request, UserPrincipal official) {
        DuplicateMatch match = duplicateMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("DuplicateMatch", "id", matchId));

        if (match.getStatus() != DuplicateMatchStatus.PENDING_REVIEW) {
            throw new BadRequestException("Duplicate match has already been reviewed");
        }

        validateDepartmentAccess(match.getSourceComplaint(), official);

        Instant now = Instant.now();
        User actingUser = userRepository.findById(official.id()).orElse(null);

        match.setStatus(request.action());
        match.setReviewedByUser(actingUser);
        match.setReviewedAt(now);
        duplicateMatchRepository.save(match);

        if (request.action() == DuplicateMatchStatus.CONFIRMED_MERGED) {
            Complaint source = match.getSourceComplaint();
            Complaint target = match.getMatchedComplaint();

            source.setStatus(ComplaintStatus.DUPLICATE);
            source.setParentComplaint(target);

            ComplaintStatusHistory sourceHistory = ComplaintStatusHistory.builder()
                    .complaint(source)
                    .changedByUser(actingUser)
                    .fromStatus(source.getStatus())
                    .toStatus(ComplaintStatus.DUPLICATE)
                    .remarks("Confirmed duplicate of #" + target.getTrackingNumber() + ". " + (request.remarks() != null ? request.remarks() : ""))
                    .createdAt(now)
                    .build();
            historyRepository.save(sourceHistory);
            complaintRepository.save(source);

            // Increment target's merged duplicates count and recalculate priority score
            target.setMergedDuplicatesCount(target.getMergedDuplicatesCount() + 1);
            double newScore = priorityEngine.calculateScore(target, now);
            target.setPriorityScore(newScore);

            ComplaintStatusHistory targetHistory = ComplaintStatusHistory.builder()
                    .complaint(target)
                    .changedByUser(actingUser)
                    .fromStatus(target.getStatus())
                    .toStatus(target.getStatus())
                    .remarks("Merged duplicate report #" + source.getTrackingNumber() + ". Priority recalculated to " + newScore)
                    .createdAt(now)
                    .build();
            historyRepository.save(targetHistory);
            complaintRepository.save(target);

            log.info("Merged complaint #{} into #{}, new priority: {}",
                    source.getTrackingNumber(), target.getTrackingNumber(), newScore);
        }
    }

    private DuplicateMatchDto mapToDuplicateMatchDto(DuplicateMatch m) {
        Complaint s = m.getSourceComplaint();
        Complaint t = m.getMatchedComplaint();

        return new DuplicateMatchDto(
                m.getId(),
                s.getId(),
                s.getTrackingNumber(),
                s.getTitle(),
                s.getCategory().getName(),
                s.getLatitude(),
                s.getLongitude(),

                t.getId(),
                t.getTrackingNumber(),
                t.getTitle(),
                t.getCategory().getName(),
                t.getLatitude(),
                t.getLongitude(),

                m.getDistanceMeters(),
                m.getTextSimilarityScore(),
                m.getCompositeConfidence(),
                m.getStatus(),
                m.getCreatedAt()
        );
    }

    /**
     * Enforces department-level authorization.
     * Officials can only act on complaints assigned to their own department.
     * Admins have system-wide authority across all departments.
     */
    private void validateDepartmentAccess(Complaint complaint, UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedActionException("Authentication required to perform official actions");
        }

        if (principal.role() == Role.ROLE_ADMIN) {
            return; // Admins are authorized across all departments
        }

        if (principal.role() == Role.ROLE_OFFICIAL) {
            Long officialDeptId = principal.departmentId();
            Long complaintDeptId = complaint.getDepartment() != null ? complaint.getDepartment().getId() : null;

            if (officialDeptId == null || !officialDeptId.equals(complaintDeptId)) {
                log.warn("Cross-department access blocked: Official {} (Dept {}) attempted action on Complaint #{} (Dept {})",
                        principal.id(), officialDeptId, complaint.getTrackingNumber(), complaintDeptId);
                throw new UnauthorizedActionException(
                        "Access denied: You are only authorized to manage complaints belonging to your department (Department ID: "
                                + officialDeptId + ")"
                );
            }
            return;
        }

        throw new UnauthorizedActionException("Access denied: You do not possess official or administrator authority");
    }
}
