package com.civicpulse.complaint.controller;

import com.civicpulse.common.ApiResponse;
import com.civicpulse.complaint.service.OfficialComplaintService;
import com.civicpulse.security.UserPrincipal;
import com.civicpulse.user.dto.UserDtos.UserProfileResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/official")
@PreAuthorize("hasAnyAuthority('ROLE_OFFICIAL', 'ROLE_ADMIN')")
public class OfficialController {

    private final OfficialComplaintService officialComplaintService;

    public OfficialController(OfficialComplaintService officialComplaintService) {
        this.officialComplaintService = officialComplaintService;
    }

    /**
     * Retrieve officials eligible for assignment.
     * Scoped strictly to the caller's department for ROLE_OFFICIAL.
     * Admins may retrieve officials across any department.
     */
    @GetMapping({"/assignees", "/officials"})
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getAssignees(
            @RequestParam(required = false) Long departmentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<UserProfileResponse> assignees = officialComplaintService.getAssignmentTargets(departmentId, principal);
        return ResponseEntity.ok(ApiResponse.ok(assignees));
    }
}
