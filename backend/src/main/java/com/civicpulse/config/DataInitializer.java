package com.civicpulse.config;

import com.civicpulse.complaint.model.Complaint;
import com.civicpulse.complaint.model.ComplaintStatus;
import com.civicpulse.complaint.model.ComplaintStatusHistory;
import com.civicpulse.complaint.model.Severity;
import com.civicpulse.complaint.repository.ComplaintRepository;
import com.civicpulse.complaint.repository.ComplaintStatusHistoryRepository;
import com.civicpulse.department.model.Category;
import com.civicpulse.department.model.Department;
import com.civicpulse.department.repository.CategoryRepository;
import com.civicpulse.department.repository.DepartmentRepository;
import com.civicpulse.engine.duplicate.DuplicateDetectionEngine;
import com.civicpulse.engine.priority.PriorityCalculationEngine;
import com.civicpulse.user.Role;
import com.civicpulse.user.User;
import com.civicpulse.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;
    private final ComplaintRepository complaintRepository;
    private final ComplaintStatusHistoryRepository historyRepository;
    private final PasswordEncoder passwordEncoder;
    private final PriorityCalculationEngine priorityEngine;
    private final DuplicateDetectionEngine duplicateEngine;

    public DataInitializer(
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            CategoryRepository categoryRepository,
            ComplaintRepository complaintRepository,
            ComplaintStatusHistoryRepository historyRepository,
            PasswordEncoder passwordEncoder,
            PriorityCalculationEngine priorityEngine,
            DuplicateDetectionEngine duplicateEngine
    ) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.categoryRepository = categoryRepository;
        this.complaintRepository = complaintRepository;
        this.historyRepository = historyRepository;
        this.passwordEncoder = passwordEncoder;
        this.priorityEngine = priorityEngine;
        this.duplicateEngine = duplicateEngine;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already populated. Skipping seed initialization.");
            return;
        }

        log.info("Initializing CivicPulse demo dataset with realistic municipal entities...");

        // 1. Create Departments
        Department roads = departmentRepository.save(Department.builder()
                .name("Roads & Infrastructure")
                .description("Maintenance of arterial roads, flyovers, footpaths and stormwater bridges")
                .contactEmail("roads@civicpulse.gov")
                .build());

        Department water = departmentRepository.save(Department.builder()
                .name("Water Supply & Sewerage")
                .description("Management of potable water pipelines, sewage treatment and storm drainage")
                .contactEmail("water@civicpulse.gov")
                .build());

        Department sanitation = departmentRepository.save(Department.builder()
                .name("Solid Waste Management")
                .description("Garbage clearance, segregation, public sanitation and landfill operations")
                .contactEmail("sanitation@civicpulse.gov")
                .build());

        Department electricity = departmentRepository.save(Department.builder()
                .name("Electricity & Street Lighting")
                .description("Streetlight infrastructure, transformer maintenance and high-tension line safety")
                .contactEmail("electricity@civicpulse.gov")
                .build());

        // 2. Create Categories
        Category catPothole = categoryRepository.save(Category.builder()
                .department(roads)
                .name("Deep Pothole / Road Cave-in")
                .defaultSeverity(Severity.HIGH)
                .slaHours(24)
                .isActive(true)
                .build());

        Category catFootpath = categoryRepository.save(Category.builder()
                .department(roads)
                .name("Damaged Footpath / Missing Manhole Lid")
                .defaultSeverity(Severity.CRITICAL)
                .slaHours(12)
                .isActive(true)
                .build());

        Category catPipeBurst = categoryRepository.save(Category.builder()
                .department(water)
                .name("Water Main Pipeline Burst")
                .defaultSeverity(Severity.CRITICAL)
                .slaHours(18)
                .isActive(true)
                .build());

        Category catSewageOverflow = categoryRepository.save(Category.builder()
                .department(water)
                .name("Sewage Overflow on Street")
                .defaultSeverity(Severity.HIGH)
                .slaHours(36)
                .isActive(true)
                .build());

        Category catGarbageDump = categoryRepository.save(Category.builder()
                .department(sanitation)
                .name("Illegal Garbage Dump")
                .defaultSeverity(Severity.MEDIUM)
                .slaHours(48)
                .isActive(true)
                .build());

        Category catStreetlight = categoryRepository.save(Category.builder()
                .department(electricity)
                .name("Streetlight Outage / Dangling Live Wire")
                .defaultSeverity(Severity.HIGH)
                .slaHours(24)
                .isActive(true)
                .build());

        // 3. Create Users
        User admin = userRepository.save(User.builder()
                .email("admin@civicpulse.gov")
                .passwordHash(passwordEncoder.encode("admin123"))
                .fullName("Chief Administrative Officer")
                .phoneNumber("+91 98765 00001")
                .role(Role.ROLE_ADMIN)
                .isActive(true)
                .build());

        User roadsOfficial = userRepository.save(User.builder()
                .email("roads.officer@civicpulse.gov")
                .passwordHash(passwordEncoder.encode("official123"))
                .fullName("Rajesh Sharma (Exec. Engineer, Roads)")
                .phoneNumber("+91 98765 00002")
                .role(Role.ROLE_OFFICIAL)
                .department(roads)
                .isActive(true)
                .build());

        User waterOfficial = userRepository.save(User.builder()
                .email("water.officer@civicpulse.gov")
                .passwordHash(passwordEncoder.encode("official123"))
                .fullName("Pooja Nair (Zonal Engineer, Water)")
                .phoneNumber("+91 98765 00003")
                .role(Role.ROLE_OFFICIAL)
                .department(water)
                .isActive(true)
                .build());

        User citizenArun = userRepository.save(User.builder()
                .email("arun.kumar@gmail.com")
                .passwordHash(passwordEncoder.encode("citizen123"))
                .fullName("Arun Kumar")
                .phoneNumber("+91 91234 56789")
                .role(Role.ROLE_CITIZEN)
                .isActive(true)
                .build());

        User citizenDeepa = userRepository.save(User.builder()
                .email("deepa.s@gmail.com")
                .passwordHash(passwordEncoder.encode("citizen123"))
                .fullName("Deepa Sharma")
                .phoneNumber("+91 92345 67890")
                .role(Role.ROLE_CITIZEN)
                .isActive(true)
                .build());

        // 4. Seed Seed Complaints demonstrating different features
        Instant now = Instant.now();

        // Complaint 1: Urgent Road Pothole with high upvotes (High Priority)
        Complaint c1 = complaintRepository.save(Complaint.builder()
                .trackingNumber("CP-2026-R0001")
                .citizen(citizenArun)
                .category(catPothole)
                .department(roads)
                .title("Severe crater-sized pothole on MG Road near Metro Pillar 142")
                .description("Deep pothole causing sudden braking and two-wheeler accidents during evening peak hours.")
                .severity(Severity.HIGH)
                .status(ComplaintStatus.IN_PROGRESS)
                .assignedOfficial(roadsOfficial)
                .latitude(12.9716)
                .longitude(77.5946)
                .addressLandmark("MG Road, opposite Trinity Metro Station")
                .upvotesCount(14)
                .dueDate(now.plus(Duration.ofHours(12)))
                .priorityScore(7.2)
                .createdAt(now.minus(Duration.ofHours(10)))
                .updatedAt(now.minus(Duration.ofHours(2)))
                .build());

        recordHistory(c1, citizenArun, null, ComplaintStatus.SUBMITTED, "Initial submission");
        recordHistory(c1, roadsOfficial, ComplaintStatus.SUBMITTED, ComplaintStatus.IN_PROGRESS, "Inspection team dispatched with asphalt premix");

        // Complaint 2: Duplicate of Complaint 1 (Located only 45 meters away with similar title)
        Complaint c2 = complaintRepository.save(Complaint.builder()
                .trackingNumber("CP-2026-R0002")
                .citizen(citizenDeepa)
                .category(catPothole)
                .department(roads)
                .title("Huge dangerous pothole near Trinity Metro Pillar 142")
                .description("Cars are swerving sharply to avoid a 1-foot deep pothole on MG Road.")
                .severity(Severity.HIGH)
                .status(ComplaintStatus.SUBMITTED)
                .latitude(12.9719) // approx 35 meters away
                .longitude(77.5948)
                .addressLandmark("MG Road, near pillar 142")
                .upvotesCount(3)
                .dueDate(now.plus(Duration.ofHours(20)))
                .priorityScore(5.4)
                .createdAt(now.minus(Duration.ofHours(4)))
                .updatedAt(now.minus(Duration.ofHours(4)))
                .build());

        recordHistory(c2, citizenDeepa, null, ComplaintStatus.SUBMITTED, "Initial submission");

        // Complaint 3: Critical Overdue Water Main Burst (Overdue & Escalated)
        Complaint c3 = complaintRepository.save(Complaint.builder()
                .trackingNumber("CP-2026-W0001")
                .citizen(citizenArun)
                .category(catPipeBurst)
                .department(water)
                .title("Major underground water pipe burst flooding 4th Cross")
                .description("Drinking water pipe cracked and drinking water gushing onto the residential street since yesterday.")
                .severity(Severity.CRITICAL)
                .status(ComplaintStatus.ASSIGNED)
                .assignedOfficial(waterOfficial)
                .latitude(12.9780)
                .longitude(77.6400)
                .addressLandmark("4th Cross, Indiranagar, near Post Office")
                .upvotesCount(22)
                .dueDate(now.minus(Duration.ofHours(6))) // 6 hours OVERDUE!
                .isEscalated(true)
                .escalationLevel(1)
                .priorityScore(9.4)
                .createdAt(now.minus(Duration.ofHours(24)))
                .updatedAt(now.minus(Duration.ofHours(6)))
                .build());

        recordHistory(c3, citizenArun, null, ComplaintStatus.SUBMITTED, "Submitted");
        recordHistory(c3, null, ComplaintStatus.SUBMITTED, ComplaintStatus.ASSIGNED, "Assigned to zonal engineer");
        recordHistory(c3, null, ComplaintStatus.ASSIGNED, ComplaintStatus.ASSIGNED, "Automated SLA breach escalation to Level 1");

        // Complaint 4: Resolved Sanitation Complaint
        Complaint c4 = complaintRepository.save(Complaint.builder()
                .trackingNumber("CP-2026-S0001")
                .citizen(citizenDeepa)
                .category(catGarbageDump)
                .department(sanitation)
                .title("Unattended commercial garbage dumping at community park corner")
                .description("Bulk vegetable waste dumped outside playground causing severe foul odor and stray dogs.")
                .severity(Severity.MEDIUM)
                .status(ComplaintStatus.RESOLVED)
                .latitude(12.9352)
                .longitude(77.6245)
                .addressLandmark("Koramangala 5th Block, near Park entrance")
                .upvotesCount(8)
                .dueDate(now.plus(Duration.ofHours(24)))
                .resolvedAt(now.minus(Duration.ofHours(3)))
                .resolutionRemarks("Sanitation truck cleared 2.5 tons of debris. Daily ward patrol scheduled.")
                .priorityScore(4.2)
                .createdAt(now.minus(Duration.ofHours(30)))
                .updatedAt(now.minus(Duration.ofHours(3)))
                .build());

        recordHistory(c4, citizenDeepa, null, ComplaintStatus.SUBMITTED, "Submitted");
        recordHistory(c4, null, ComplaintStatus.SUBMITTED, ComplaintStatus.RESOLVED, "Cleaned and verified by ward supervisor");

        // Run duplicate detection scan to link c2 as candidate match for c1
        try {
            duplicateEngine.scanAndFlagDuplicates(c2);
        } catch (Exception e) {
            log.error("Duplicate scan error during initial seeding: ", e);
        }

        log.info("CivicPulse demo data initialized successfully!");
        log.info("Test Accounts -> Admin: admin@civicpulse.gov / admin123 | Official: roads.officer@civicpulse.gov / official123 | Citizen: arun.kumar@gmail.com / citizen123");
    }

    private void recordHistory(Complaint complaint, User user, ComplaintStatus from, ComplaintStatus to, String remarks) {
        historyRepository.save(ComplaintStatusHistory.builder()
                .complaint(complaint)
                .changedByUser(user)
                .fromStatus(from)
                .toStatus(to)
                .remarks(remarks)
                .build());
    }
}
