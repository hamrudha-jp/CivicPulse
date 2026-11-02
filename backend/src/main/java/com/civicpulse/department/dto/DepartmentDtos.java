package com.civicpulse.department.dto;

import com.civicpulse.complaint.model.Severity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class DepartmentDtos {

    public record DepartmentResponse(
            Long id,
            String name,
            String description,
            String contactEmail,
            List<CategoryResponse> categories
    ) {}

    public record CategoryResponse(
            Long id,
            Long departmentId,
            String departmentName,
            String name,
            Severity defaultSeverity,
            Integer slaHours,
            Boolean isActive
    ) {}

    public record CreateDepartmentRequest(
            @NotBlank(message = "Department name is required")
            String name,

            String description,

            @Email(message = "Invalid contact email")
            String contactEmail
    ) {}

    public record CreateCategoryRequest(
            @NotNull(message = "Department ID is required")
            Long departmentId,

            @NotBlank(message = "Category name is required")
            String name,

            Severity defaultSeverity,

            @NotNull(message = "SLA hours are required")
            @Min(value = 1, message = "SLA hours must be at least 1")
            Integer slaHours
    ) {}
}
