package com.civicpulse.department.model;

import com.civicpulse.complaint.model.Severity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_severity", nullable = false, length = 20)
    @Builder.Default
    private Severity defaultSeverity = Severity.MEDIUM;

    @Column(name = "sla_hours", nullable = false)
    @Builder.Default
    private Integer slaHours = 48;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
