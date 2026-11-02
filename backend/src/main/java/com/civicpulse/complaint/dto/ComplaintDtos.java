package com.civicpulse.complaint.dto;

import com.civicpulse.complaint.model.AttachmentType;
import com.civicpulse.complaint.model.ComplaintStatus;
import com.civicpulse.complaint.model.DuplicateMatchStatus;
import com.civicpulse.complaint.model.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public class ComplaintDtos {

    public record CreateComplaintRequest(
            @NotNull(message = "Category ID is required")
            Long categoryId,

            @NotBlank(message = "Title is required")
            @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
            String title,

            @NotBlank(message = "Description is required")
            @Size(min = 10, message = "Description must provide at least 10 characters of detail")
            String description,

            Severity severity,

            @NotNull(message = "Latitude is required")
            Double latitude,

            @NotNull(message = "Longitude is required")
            Double longitude,

            @NotBlank(message = "Address or landmark is required")
            String addressLandmark
    ) {}

    public record AttachmentDto(
            Long id,
            String fileName,
            String fileUrl,
            String fileType,
            Long fileSizeBytes,
            AttachmentType attachmentType,
            Instant uploadedAt
    ) {}

    public record StatusHistoryDto(
            Long id,
            ComplaintStatus fromStatus,
            ComplaintStatus toStatus,
            String remarks,
            String changedByUserName,
            Instant createdAt
    ) {}

    public record ComplaintSummaryResponse(
            Long id,
            String trackingNumber,
            String title,
            String categoryName,
            String departmentName,
            Severity severity,
            ComplaintStatus status,
            Double latitude,
            Double longitude,
            String addressLandmark,
            Double priorityScore,
            Integer escalationLevel,
            Boolean isEscalated,
            Integer upvotesCount,
            Instant dueDate,
            Instant createdAt
    ) {}

    public record ComplaintResponse(
            Long id,
            String trackingNumber,
            String title,
            String description,
            Long categoryId,
            String categoryName,
            Long departmentId,
            String departmentName,
            Long citizenId,
            String citizenName,
            Long assignedOfficialId,
            String assignedOfficialName,
            Severity severity,
            ComplaintStatus status,
            Double latitude,
            Double longitude,
            String addressLandmark,
            Double priorityScore,
            Integer escalationLevel,
            Boolean isEscalated,
            Integer upvotesCount,
            Integer mergedDuplicatesCount,
            Instant dueDate,
            Instant resolvedAt,
            String resolutionRemarks,
            Long parentComplaintId,
            String parentComplaintTrackingNumber,
            Instant createdAt,
            Instant updatedAt,
            List<AttachmentDto> attachments,
            List<StatusHistoryDto> statusHistory
    ) {}

    public record UpdateStatusRequest(
            @NotNull(message = "Target status is required")
            ComplaintStatus status,
            String remarks
    ) {}

    public record AssignComplaintRequest(
            @NotNull(message = "Official ID is required")
            Long officialId
    ) {}

    public record ResolveComplaintRequest(
            @NotBlank(message = "Resolution remarks are required")
            String remarks
    ) {}

    public record DuplicateMatchDto(
            Long id,
            Long sourceComplaintId,
            String sourceTrackingNumber,
            String sourceTitle,
            String sourceCategory,
            Double sourceLat,
            Double sourceLon,

            Long matchedComplaintId,
            String matchedTrackingNumber,
            String matchedTitle,
            String matchedCategory,
            Double matchedLat,
            Double matchedLon,

            Double distanceMeters,
            Double textSimilarityScore,
            Double compositeConfidence,
            DuplicateMatchStatus status,
            Instant createdAt
    ) {}

    public record ReviewDuplicateRequest(
            @NotNull(message = "Action is required")
            DuplicateMatchStatus action, // CONFIRMED_MERGED or DISMISSED
            String remarks
    ) {}
}
