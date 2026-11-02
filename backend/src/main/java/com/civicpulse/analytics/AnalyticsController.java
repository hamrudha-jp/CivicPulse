package com.civicpulse.analytics;

import com.civicpulse.analytics.dto.DashboardMetricsResponse;
import com.civicpulse.common.ApiResponse;
import com.civicpulse.security.UserPrincipal;
import com.civicpulse.user.Role;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@PreAuthorize("hasAnyAuthority('ROLE_OFFICIAL', 'ROLE_ADMIN')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardMetricsResponse>> getDashboardMetrics(
            @RequestParam(required = false) Long departmentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long effectiveDeptId = departmentId;
        if (principal != null && principal.role() == Role.ROLE_OFFICIAL) {
            effectiveDeptId = principal.departmentId();
        }
        DashboardMetricsResponse metrics = analyticsService.getDashboardMetrics(effectiveDeptId);
        return ResponseEntity.ok(ApiResponse.ok(metrics));
    }
}
