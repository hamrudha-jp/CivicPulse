package com.civicpulse.complaint.controller;

import com.civicpulse.common.ApiResponse;
import com.civicpulse.complaint.dto.ComplaintDtos.*;
import com.civicpulse.complaint.service.OfficialComplaintService;
import com.civicpulse.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/duplicates")
@PreAuthorize("hasAnyAuthority('ROLE_OFFICIAL', 'ROLE_ADMIN')")
public class DuplicateController {

    private final OfficialComplaintService officialComplaintService;

    public DuplicateController(OfficialComplaintService officialComplaintService) {
        this.officialComplaintService = officialComplaintService;
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<DuplicateMatchDto>>> getPendingDuplicates(
            @RequestParam(required = false) Long departmentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<DuplicateMatchDto> pending = officialComplaintService.getPendingDuplicates(departmentId, principal);
        return ResponseEntity.ok(ApiResponse.ok(pending));
    }

    @PostMapping("/{matchId}/review")
    public ResponseEntity<ApiResponse<Void>> reviewDuplicate(
            @PathVariable Long matchId,
            @Valid @RequestBody ReviewDuplicateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        officialComplaintService.reviewDuplicate(matchId, request, principal);
        String actionMsg = request.action().name().equals("CONFIRMED_MERGED")
                ? "Complaints merged successfully. Duplicate closed and primary issue signal amplified."
                : "Match dismissed. Complaint remains independent.";
        return ResponseEntity.ok(ApiResponse.ok(actionMsg, null));
    }
}
