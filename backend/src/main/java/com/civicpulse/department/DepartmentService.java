package com.civicpulse.department;

import com.civicpulse.common.exception.BadRequestException;
import com.civicpulse.common.exception.ResourceNotFoundException;
import com.civicpulse.complaint.model.Severity;
import com.civicpulse.department.dto.DepartmentDtos.*;
import com.civicpulse.department.model.Category;
import com.civicpulse.department.model.Department;
import com.civicpulse.department.repository.CategoryRepository;
import com.civicpulse.department.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;

    public DepartmentService(DepartmentRepository departmentRepository, CategoryRepository categoryRepository) {
        this.departmentRepository = departmentRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::mapToDepartmentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllActiveCategories() {
        return categoryRepository.findByIsActiveTrue().stream()
                .map(this::mapToCategoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoriesByDepartment(Long departmentId) {
        return categoryRepository.findByDepartmentIdAndIsActiveTrue(departmentId).stream()
                .map(this::mapToCategoryResponse)
                .toList();
    }

    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        String trimmedName = request.name().trim();
        if (departmentRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new BadRequestException("Department with name '" + trimmedName + "' already exists");
        }

        Department department = Department.builder()
                .name(trimmedName)
                .description(request.description())
                .contactEmail(request.contactEmail())
                .build();

        return mapToDepartmentResponse(departmentRepository.save(department));
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.departmentId()));

        Category category = Category.builder()
                .department(department)
                .name(request.name().trim())
                .defaultSeverity(request.defaultSeverity() != null ? request.defaultSeverity() : Severity.MEDIUM)
                .slaHours(request.slaHours())
                .isActive(true)
                .build();

        return mapToCategoryResponse(categoryRepository.save(category));
    }

    private DepartmentResponse mapToDepartmentResponse(Department dept) {
        List<CategoryResponse> categoryResponses = dept.getCategories().stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .map(this::mapToCategoryResponse)
                .toList();

        return new DepartmentResponse(
                dept.getId(),
                dept.getName(),
                dept.getDescription(),
                dept.getContactEmail(),
                categoryResponses
        );
    }

    private CategoryResponse mapToCategoryResponse(Category cat) {
        return new CategoryResponse(
                cat.getId(),
                cat.getDepartment().getId(),
                cat.getDepartment().getName(),
                cat.getName(),
                cat.getDefaultSeverity(),
                cat.getSlaHours(),
                cat.getIsActive()
        );
    }
}
