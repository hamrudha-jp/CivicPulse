package com.civicpulse.complaint.controller;

import com.civicpulse.common.ApiResponse;
import com.civicpulse.complaint.dto.ComplaintDtos.*;
import com.civicpulse.complaint.service.ComplaintService;
import com.civicpulse.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    /**
     * Submit complaint with optional photo (multipart/form-data)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_CITIZEN')")
    public ResponseEntity<ApiResponse<ComplaintResponse>> createComplaintWithFile(
            @RequestPart("data") @Valid CreateComplaintRequest request,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ComplaintResponse created = complaintService.createComplaint(request, photo, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Complaint registered successfully", created));
    }

    /**
     * Submit complaint directly as JSON (convenient for API clients/Postman)
     */
    @PostMapping(value = "/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ROLE_CITIZEN')")
    public ResponseEntity<ApiResponse<ComplaintResponse>> createComplaintJson(
            @Valid @RequestBody CreateComplaintRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ComplaintResponse created = complaintService.createComplaint(request, null, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Complaint registered successfully", created));
    }

    /**
     * Public tracking by tracking number (e.g. CP-2026-A1B2C)
     */
    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<ApiResponse<ComplaintResponse>> trackComplaint(@PathVariable String trackingNumber) {
        ComplaintResponse complaint = complaintService.getComplaintByTrackingNumber(trackingNumber);
        return ResponseEntity.ok(ApiResponse.ok(complaint));
    }

    /**
     * Full detail by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ComplaintResponse>> getComplaintById(@PathVariable Long id) {
        ComplaintResponse complaint = complaintService.getComplaintById(id);
        return ResponseEntity.ok(ApiResponse.ok(complaint));
    }

    /**
     * Authenticated citizen's submitted complaints
     */
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ROLE_CITIZEN')")
    public ResponseEntity<ApiResponse<Page<ComplaintSummaryResponse>>> getMyComplaints(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ComplaintSummaryResponse> complaints = complaintService.getCitizenComplaints(principal.id(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(complaints));
    }

    /**
     * Public feed for civic transparency and map visualization
     */
    @GetMapping("/public-feed")
    public ResponseEntity<ApiResponse<List<ComplaintSummaryResponse>>> getPublicFeed(
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Double minLon,
            @RequestParam(required = false) Double maxLon
    ) {
        List<ComplaintSummaryResponse> feed = complaintService.getPublicFeed(minLat, maxLat, minLon, maxLon);
        return ResponseEntity.ok(ApiResponse.ok(feed));
    }

    /**
     * Citizen upvote toggle (amplifies community impact score)
     */
    @PostMapping("/{id}/upvote")
    @PreAuthorize("hasAuthority('ROLE_CITIZEN')")
    public ResponseEntity<ApiResponse<ComplaintSummaryResponse>> toggleUpvote(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ComplaintSummaryResponse updated = complaintService.toggleUpvote(id, principal);
        return ResponseEntity.ok(ApiResponse.ok("Upvote updated successfully", updated));
    }

    /**
     * Cancel complaint (Citizen can cancel only their own complaint while in SUBMITTED status)
     */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('ROLE_CITIZEN')")
    public ResponseEntity<ApiResponse<ComplaintResponse>> cancelComplaint(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ComplaintResponse cancelled = complaintService.cancelComplaint(id, principal);
        return ResponseEntity.ok(ApiResponse.ok("Complaint cancelled successfully", cancelled));
    }
}
