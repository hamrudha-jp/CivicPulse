package com.civicpulse.department.repository;

import com.civicpulse.department.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByDepartmentIdAndIsActiveTrue(Long departmentId);
    List<Category> findByIsActiveTrue();
}
