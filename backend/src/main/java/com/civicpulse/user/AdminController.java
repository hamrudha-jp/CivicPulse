package com.civicpulse.user;

import com.civicpulse.common.ApiResponse;
import com.civicpulse.common.exception.ResourceNotFoundException;
import com.civicpulse.department.model.Department;
import com.civicpulse.department.repository.DepartmentRepository;
import com.civicpulse.engine.sla.SlaEscalationScheduler;
import com.civicpulse.user.dto.UserDtos.UserProfileResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final SlaEscalationScheduler escalationScheduler;

    public AdminController(
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            SlaEscalationScheduler escalationScheduler
    ) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.escalationScheduler = escalationScheduler;
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getAllUsers() {
        List<UserProfileResponse> users = userRepository.findAll().stream()
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

        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @GetMapping("/officials")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getOfficials(
            @RequestParam(required = false) Long departmentId
    ) {
        List<User> officials = departmentId != null
                ? userRepository.findByDepartmentId(departmentId)
                : userRepository.findByRole(Role.ROLE_OFFICIAL);

        List<UserProfileResponse> dtos = officials.stream()
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

        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }

    public record UpdateUserRoleRequest(Role role, Long departmentId) {}

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserRole(
            @PathVariable Long id,
            @RequestBody UpdateUserRoleRequest request
    ) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (request.role() != null) {
            user.setRole(request.role());
        }

        if (request.departmentId() != null) {
            Department dept = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.departmentId()));
            user.setDepartment(dept);
        } else if (request.role() == Role.ROLE_CITIZEN) {
            user.setDepartment(null);
        }

        User updated = userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.ok(
                "User role updated successfully",
                new UserProfileResponse(
                        updated.getId(),
                        updated.getEmail(),
                        updated.getFullName(),
                        updated.getPhoneNumber(),
                        updated.getRole().name(),
                        updated.getDepartment() != null ? updated.getDepartment().getId() : null,
                        updated.getDepartment() != null ? updated.getDepartment().getName() : null
                )
        ));
    }

    @PostMapping("/escalation/trigger")
    public ResponseEntity<ApiResponse<String>> triggerEscalationCheck() {
        escalationScheduler.runEscalationAndScoreRefresh();
        return ResponseEntity.ok(ApiResponse.ok("Manual escalation and aging recalculation executed successfully", null));
    }
}
