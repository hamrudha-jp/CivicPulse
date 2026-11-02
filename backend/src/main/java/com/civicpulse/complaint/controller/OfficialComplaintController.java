package com.civicpulse.complaint.controller;

import com.civicpulse.common.ApiResponse;
import com.civicpulse.complaint.dto.ComplaintDtos.*;
import com.civicpulse.complaint.model.ComplaintStatus;
import com.civicpulse.complaint.model.Severity;
import com.civicpulse.complaint.service.OfficialComplaintService;
import com.civicpulse.security.UserPrincipal;
import com.civicpulse.user.dto.UserDtos.UserProfileResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/official/complaints")
@PreAuthorize("hasAnyAuthority('ROLE_OFFICIAL', 'ROLE_ADMIN')")
public class OfficialComplaintController {

    private final OfficialComplaintService officialComplaintService;

    public OfficialComplaintController(OfficialComplaintService officialComplaintService) {
        this.officialComplaintService = officialComplaintService;
    }

    /**
     * Official prioritization queue: Sorted by priorityScore DESC by default
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ComplaintSummaryResponse>>> getOfficialQueue(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) Boolean isEscalated,
            @PageableDefault(sort = "priorityScore", direction = Sort.Direction.DESC, size = 20) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Page<ComplaintSummaryResponse> queue = officialComplaintService.getOfficialQueue(
                departmentId, status, severity, isEscalated, pageable, principal
        );
        return ResponseEntity.ok(ApiResponse.ok(queue));
    }

    /**
     * Get candidate officials for complaint assignment.
     * Enforces caller's department scoping for ROLE_OFFICIAL.
     */
    @GetMapping("/assignees")
    public ResponseEntity<ApiResponse<java.util.List<UserProfileResponse>>> getAssignmentTargets(
            @RequestParam(required = false) Long departmentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        java.util.List<UserProfileResponse> officials = officialComplaintService.getAssignmentTargets(departmentId, principal);
        return ResponseEntity.ok(ApiResponse.ok(officials));
    }

    /**
     * Assign complaint to an official
     */
    @PostMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<ComplaintResponse>> assignComplaint(
            @PathVariable Long id,
            @Valid @RequestBody AssignComplaintRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ComplaintResponse updated = officialComplaintService.assignComplaint(id, request.officialId(), principal);
        return ResponseEntity.ok(ApiResponse.ok("Complaint assigned successfully", updated));
    }

    /**
     * Update operational status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ComplaintResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ComplaintResponse updated = officialComplaintService.updateStatus(id, request, principal);
        return ResponseEntity.ok(ApiResponse.ok("Status updated successfully", updated));
    }

    /**
     * Resolve complaint with mandatory remarks and optional proof photo
     */
    @PostMapping(value = "/{id}/resolve", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ComplaintResponse>> resolveComplaintWithFile(
            @PathVariable Long id,
            @RequestPart("data") @Valid ResolveComplaintRequest request,
            @RequestPart(value = "proofPhoto", required = false) MultipartFile proofPhoto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ComplaintResponse resolved = officialComplaintService.resolveComplaint(id, request, proofPhoto, principal);
        return ResponseEntity.ok(ApiResponse.ok("Complaint resolved successfully", resolved));
    }

    /**
     * Resolve complaint directly with JSON remarks (convenient for API clients/Postman)
     */
    @PostMapping(value = "/{id}/resolve/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ComplaintResponse>> resolveComplaintJson(
            @PathVariable Long id,
            @Valid @RequestBody ResolveComplaintRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ComplaintResponse resolved = officialComplaintService.resolveComplaint(id, request, null, principal);
        return ResponseEntity.ok(ApiResponse.ok("Complaint resolved successfully", resolved));
    }
}
