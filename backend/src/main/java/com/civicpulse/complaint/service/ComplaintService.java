package com.civicpulse.complaint.service;

import com.civicpulse.common.exception.BadRequestException;
import com.civicpulse.common.exception.ResourceNotFoundException;
import com.civicpulse.common.exception.UnauthorizedActionException;
import com.civicpulse.complaint.dto.ComplaintDtos.*;
import com.civicpulse.complaint.model.*;
import com.civicpulse.complaint.repository.ComplaintAttachmentRepository;
import com.civicpulse.complaint.repository.ComplaintRepository;
import com.civicpulse.complaint.repository.ComplaintStatusHistoryRepository;
import com.civicpulse.complaint.repository.ComplaintUpvoteRepository;
import com.civicpulse.department.model.Category;
import com.civicpulse.department.repository.CategoryRepository;
import com.civicpulse.engine.duplicate.DuplicateDetectionEngine;
import com.civicpulse.engine.priority.PriorityCalculationEngine;
import com.civicpulse.security.UserPrincipal;
import com.civicpulse.storage.FileStorageService;
import com.civicpulse.user.User;
import com.civicpulse.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ComplaintService {

    private static final Logger log = LoggerFactory.getLogger(ComplaintService.class);

    private final ComplaintRepository complaintRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ComplaintAttachmentRepository attachmentRepository;
    private final ComplaintStatusHistoryRepository historyRepository;
    private final ComplaintUpvoteRepository upvoteRepository;
    private final PriorityCalculationEngine priorityEngine;
    private final DuplicateDetectionEngine duplicateEngine;
    private final FileStorageService fileStorageService;

    public ComplaintService(
            ComplaintRepository complaintRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            ComplaintAttachmentRepository attachmentRepository,
            ComplaintStatusHistoryRepository historyRepository,
            ComplaintUpvoteRepository upvoteRepository,
            PriorityCalculationEngine priorityEngine,
            DuplicateDetectionEngine duplicateEngine,
            FileStorageService fileStorageService
    ) {
        this.complaintRepository = complaintRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.attachmentRepository = attachmentRepository;
        this.historyRepository = historyRepository;
        this.upvoteRepository = upvoteRepository;
        this.priorityEngine = priorityEngine;
        this.duplicateEngine = duplicateEngine;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public ComplaintResponse createComplaint(
            CreateComplaintRequest request,
            MultipartFile photo,
            UserPrincipal principal
    ) {
        User citizen = userRepository.findById(principal.id())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.id()));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.categoryId()));

        Severity severity = request.severity() != null ? request.severity() : category.getDefaultSeverity();
        Instant now = Instant.now();
        Instant dueDate = now.plus(Duration.ofHours(category.getSlaHours()));

        double initialPriority = priorityEngine.calculateScore(
                severity, 0, 0, now, category.getSlaHours(), now
        );

        String trackingNumber = generateTrackingNumber();

        Complaint complaint = Complaint.builder()
                .trackingNumber(trackingNumber)
                .citizen(citizen)
                .category(category)
                .department(category.getDepartment())
                .title(request.title().trim())
                .description(request.description().trim())
                .severity(severity)
                .status(ComplaintStatus.SUBMITTED)
                .latitude(request.latitude())
                .longitude(request.longitude())
                .addressLandmark(request.addressLandmark().trim())
                .priorityScore(initialPriority)
                .dueDate(dueDate)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Complaint savedComplaint = complaintRepository.save(complaint);

        // Record initial status in timeline
        ComplaintStatusHistory initialHistory = ComplaintStatusHistory.builder()
                .complaint(savedComplaint)
                .changedByUser(citizen)
                .toStatus(ComplaintStatus.SUBMITTED)
                .remarks("Complaint submitted by citizen")
                .createdAt(now)
                .build();
        historyRepository.save(initialHistory);

        // Handle optional photo attachment
        if (photo != null && !photo.isEmpty()) {
            String fileUrl = fileStorageService.storeFile(photo);
            ComplaintAttachment attachment = ComplaintAttachment.builder()
                    .complaint(savedComplaint)
                    .fileName(photo.getOriginalFilename())
                    .fileUrl(fileUrl)
                    .fileType(photo.getContentType())
                    .fileSizeBytes(photo.getSize())
                    .attachmentType(AttachmentType.SUBMISSION_PROOF)
                    .uploadedAt(now)
                    .build();
            attachmentRepository.save(attachment);
        }

        // Trigger spatial and text duplicate detection
        try {
            duplicateEngine.scanAndFlagDuplicates(savedComplaint);
        } catch (Exception e) {
            log.error("Error during duplicate scanning for complaint {}: ", savedComplaint.getId(), e);
        }

        return getComplaintById(savedComplaint.getId());
    }

    @Transactional(readOnly = true)
    public ComplaintResponse getComplaintByTrackingNumber(String trackingNumber) {
        Complaint complaint = complaintRepository.findByTrackingNumber(trackingNumber.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "trackingNumber", trackingNumber));
        return mapToComplaintResponse(complaint);
    }

    @Transactional(readOnly = true)
    public ComplaintResponse getComplaintById(Long id) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", id));
        return mapToComplaintResponse(complaint);
    }

    @Transactional(readOnly = true)
    public Page<ComplaintSummaryResponse> getCitizenComplaints(Long citizenId, Pageable pageable) {
        return complaintRepository.findByCitizenId(citizenId, pageable)
                .map(this::mapToSummaryResponse);
    }

    @Transactional(readOnly = true)
    public List<ComplaintSummaryResponse> getPublicFeed(Double minLat, Double maxLat, Double minLon, Double maxLon) {
        if (minLat == null || maxLat == null || minLon == null || maxLon == null) {
            return complaintRepository.findAllActiveComplaints().stream()
                    .limit(100)
                    .map(this::mapToSummaryResponse)
                    .toList();
        }

        return complaintRepository.findActiveComplaintsInBoundingBox(0L, minLat, maxLat, minLon, maxLon).stream()
                .map(this::mapToSummaryResponse)
                .toList();
    }

    @Transactional
    public ComplaintSummaryResponse toggleUpvote(Long complaintId, UserPrincipal principal) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", complaintId));

        if (complaint.getStatus() == ComplaintStatus.RESOLVED || complaint.getStatus() == ComplaintStatus.REJECTED) {
            throw new BadRequestException("Cannot upvote a closed complaint");
        }

        User citizen = userRepository.findById(principal.id())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.id()));

        Optional<ComplaintUpvote> existingUpvote = upvoteRepository.findByComplaintIdAndCitizenId(complaintId, citizen.getId());

        if (existingUpvote.isPresent()) {
            upvoteRepository.delete(existingUpvote.get());
            complaint.setUpvotesCount(Math.max(0, complaint.getUpvotesCount() - 1));
        } else {
            ComplaintUpvote newUpvote = ComplaintUpvote.builder()
                    .complaint(complaint)
                    .citizen(citizen)
                    .build();
            upvoteRepository.save(newUpvote);
            complaint.setUpvotesCount(complaint.getUpvotesCount() + 1);
        }

        // Recalculate priority score with new upvote count
        double newScore = priorityEngine.calculateScore(complaint, Instant.now());
        complaint.setPriorityScore(newScore);
        Complaint saved = complaintRepository.save(complaint);

        return mapToSummaryResponse(saved);
    }

    @Transactional
    public ComplaintResponse cancelComplaint(Long complaintId, UserPrincipal principal) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", complaintId));

        if (principal == null || !complaint.getCitizen().getId().equals(principal.id())) {
            log.warn("Citizen {} attempted to cancel complaint #{} owned by citizen {}",
                    principal != null ? principal.id() : null,
                    complaint.getTrackingNumber(),
                    complaint.getCitizen().getId());
            throw new UnauthorizedActionException("Access denied: You can only cancel your own complaints");
        }

        if (complaint.getStatus() != ComplaintStatus.SUBMITTED) {
            log.warn("Citizen {} attempted to cancel complaint #{} which is already in status {}",
                    principal.id(), complaint.getTrackingNumber(), complaint.getStatus());
            throw new BadRequestException("Complaint cannot be cancelled once it has been processed (current status: " + complaint.getStatus() + ")");
        }

        User citizen = userRepository.findById(principal.id()).orElse(null);
        Instant now = Instant.now();

        complaint.setStatus(ComplaintStatus.CANCELLED);
        complaint.setUpdatedAt(now);

        ComplaintStatusHistory history = ComplaintStatusHistory.builder()
                .complaint(complaint)
                .changedByUser(citizen)
                .fromStatus(ComplaintStatus.SUBMITTED)
                .toStatus(ComplaintStatus.CANCELLED)
                .remarks("Cancelled by citizen")
                .createdAt(now)
                .build();
        historyRepository.save(history);

        Complaint saved = complaintRepository.save(complaint);
        log.info("Complaint #{} cancelled by owner {}", saved.getTrackingNumber(), principal.id());
        return mapToComplaintResponse(saved);
    }

    private String generateTrackingNumber() {
        int currentYear = Year.now().getValue();
        String randomSuffix = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        return String.format("CP-%d-%s", currentYear, randomSuffix);
    }

    public ComplaintResponse mapToComplaintResponse(Complaint c) {
        List<AttachmentDto> attachments = c.getAttachments().stream()
                .map(a -> new AttachmentDto(
                        a.getId(), a.getFileName(), a.getFileUrl(), a.getFileType(),
                        a.getFileSizeBytes(), a.getAttachmentType(), a.getUploadedAt()))
                .toList();

        List<StatusHistoryDto> history = c.getStatusHistory().stream()
                .map(h -> new StatusHistoryDto(
                        h.getId(), h.getFromStatus(), h.getToStatus(), h.getRemarks(),
                        h.getChangedByUser() != null ? h.getChangedByUser().getFullName() : "System",
                        h.getCreatedAt()))
                .toList();

        return new ComplaintResponse(
                c.getId(),
                c.getTrackingNumber(),
                c.getTitle(),
                c.getDescription(),
                c.getCategory().getId(),
                c.getCategory().getName(),
                c.getDepartment().getId(),
                c.getDepartment().getName(),
                c.getCitizen().getId(),
                c.getCitizen().getFullName(),
                c.getAssignedOfficial() != null ? c.getAssignedOfficial().getId() : null,
                c.getAssignedOfficial() != null ? c.getAssignedOfficial().getFullName() : null,
                c.getSeverity(),
                c.getStatus(),
                c.getLatitude(),
                c.getLongitude(),
                c.getAddressLandmark(),
                c.getPriorityScore(),
                c.getEscalationLevel(),
                c.getIsEscalated(),
                c.getUpvotesCount(),
                c.getMergedDuplicatesCount(),
                c.getDueDate(),
                c.getResolvedAt(),
                c.getResolutionRemarks(),
                c.getParentComplaint() != null ? c.getParentComplaint().getId() : null,
                c.getParentComplaint() != null ? c.getParentComplaint().getTrackingNumber() : null,
                c.getCreatedAt(),
                c.getUpdatedAt(),
                attachments,
                history
        );
    }

    public ComplaintSummaryResponse mapToSummaryResponse(Complaint c) {
        return new ComplaintSummaryResponse(
                c.getId(),
                c.getTrackingNumber(),
                c.getTitle(),
                c.getCategory().getName(),
                c.getDepartment().getName(),
                c.getSeverity(),
                c.getStatus(),
                c.getLatitude(),
                c.getLongitude(),
                c.getAddressLandmark(),
                c.getPriorityScore(),
                c.getEscalationLevel(),
                c.getIsEscalated(),
                c.getUpvotesCount(),
                c.getDueDate(),
                c.getCreatedAt()
        );
    }
}
