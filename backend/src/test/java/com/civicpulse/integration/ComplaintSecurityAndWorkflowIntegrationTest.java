package com.civicpulse.integration;

import com.civicpulse.complaint.dto.ComplaintDtos.CreateComplaintRequest;
import com.civicpulse.complaint.dto.ComplaintDtos.UpdateStatusRequest;
import com.civicpulse.complaint.model.Complaint;
import com.civicpulse.complaint.model.ComplaintStatus;
import com.civicpulse.complaint.model.Severity;
import com.civicpulse.complaint.repository.ComplaintRepository;
import com.civicpulse.complaint.service.ComplaintService;
import com.civicpulse.department.model.Category;
import com.civicpulse.department.repository.CategoryRepository;
import com.civicpulse.security.JwtTokenProvider;
import com.civicpulse.security.UserPrincipal;
import com.civicpulse.user.User;
import com.civicpulse.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ComplaintSecurityAndWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ComplaintService complaintService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;
    private String roadsOfficialToken;
    private String waterOfficialToken;
    private String arunToken;
    private String deepaToken;

    private User admin;
    private User roadsOfficial;
    private User waterOfficial;
    private User citizenArun;
    private User citizenDeepa;

    @BeforeEach
    void setUp() {
        admin = userRepository.findByEmail("admin@civicpulse.gov").orElseThrow();
        roadsOfficial = userRepository.findByEmail("roads.officer@civicpulse.gov").orElseThrow();
        waterOfficial = userRepository.findByEmail("water.officer@civicpulse.gov").orElseThrow();
        citizenArun = userRepository.findByEmail("arun.kumar@gmail.com").orElseThrow();
        citizenDeepa = userRepository.findByEmail("deepa.s@gmail.com").orElseThrow();

        adminToken = jwtTokenProvider.generateTokenFromUser(UserPrincipal.create(admin));
        roadsOfficialToken = jwtTokenProvider.generateTokenFromUser(UserPrincipal.create(roadsOfficial));
        waterOfficialToken = jwtTokenProvider.generateTokenFromUser(UserPrincipal.create(waterOfficial));
        arunToken = jwtTokenProvider.generateTokenFromUser(UserPrincipal.create(citizenArun));
        deepaToken = jwtTokenProvider.generateTokenFromUser(UserPrincipal.create(citizenDeepa));
    }

    @Test
    @DisplayName("Unauthenticated request to official queue returns 401 Unauthorized")
    void testUnauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/official/complaints"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("Citizen token attempting official queue returns 403 Forbidden")
    void testCitizenAccessToOfficialEndpointsReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/official/complaints")
                        .header("Authorization", "Bearer " + arunToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("Cross-department access denial: Water official cannot update Roads complaint")
    void testCrossDepartmentAccessDeniedForOfficial() throws Exception {
        Complaint roadsComplaint = complaintRepository.findAll().stream()
                .filter(c -> c.getDepartment().getName().contains("Roads"))
                .findFirst()
                .orElseThrow();

        UpdateStatusRequest updateRequest = new UpdateStatusRequest(
                ComplaintStatus.UNDER_REVIEW,
                "Unauthorized cross-department attempt"
        );

        mockMvc.perform(patch("/api/v1/official/complaints/" + roadsComplaint.getId() + "/status")
                        .header("Authorization", "Bearer " + waterOfficialToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Access denied")));
    }

    @Test
    @DisplayName("Admin has cross-department authority across all departments")
    void testAdminCrossDepartmentAccessAllowed() throws Exception {
        Complaint roadsComplaint = complaintRepository.findAll().stream()
                .filter(c -> c.getDepartment().getName().contains("Roads") && c.getStatus() != ComplaintStatus.RESOLVED)
                .findFirst()
                .orElseThrow();

        UpdateStatusRequest updateRequest = new UpdateStatusRequest(
                ComplaintStatus.UNDER_REVIEW,
                "Admin operational review"
        );

        mockMvc.perform(patch("/api/v1/official/complaints/" + roadsComplaint.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("UNDER_REVIEW")));
    }

    @Test
    @DisplayName("Citizen cannot cancel another citizen's complaint")
    void testCitizenCannotCancelAnotherCitizensComplaint() throws Exception {
        // Deepa's complaint
        Complaint deepaComplaint = complaintRepository.findAll().stream()
                .filter(c -> c.getCitizen().getId().equals(citizenDeepa.getId()) && c.getStatus() == ComplaintStatus.SUBMITTED)
                .findFirst()
                .orElseThrow();

        // Arun attempts to cancel Deepa's complaint
        mockMvc.perform(patch("/api/v1/complaints/" + deepaComplaint.getId() + "/cancel")
                        .header("Authorization", "Bearer " + arunToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("only cancel your own")));
    }

    @Test
    @DisplayName("Citizen cannot cancel complaint that is already processed (IN_PROGRESS/ASSIGNED)")
    void testCitizenCannotCancelComplaintAlreadyInProgress() throws Exception {
        // Find an active complaint that is not SUBMITTED
        Complaint processedComplaint = complaintRepository.findAll().stream()
                .filter(c -> c.getCitizen().getId().equals(citizenArun.getId())
                        && (c.getStatus() == ComplaintStatus.IN_PROGRESS || c.getStatus() == ComplaintStatus.ASSIGNED))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(patch("/api/v1/complaints/" + processedComplaint.getId() + "/cancel")
                        .header("Authorization", "Bearer " + arunToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("cannot be cancelled once it has been processed")));
    }

    @Test
    @DisplayName("Citizen can cancel own SUBMITTED complaint, but cannot cancel it again once CANCELLED")
    void testCitizenCanCancelOwnSubmittedComplaint() throws Exception {
        Category category = categoryRepository.findAll().getFirst();

        // Create a new SUBMITTED complaint for Arun
        CreateComplaintRequest createRequest = new CreateComplaintRequest(
                category.getId(),
                "Temporary complaint to be cancelled",
                "Testing self cancellation endpoint",
                Severity.LOW,
                12.9716,
                77.5946,
                "MG Road landmark"
        );

        var created = complaintService.createComplaint(
                createRequest, null, UserPrincipal.create(citizenArun)
        );

        Long complaintId = created.id();
        assertEquals(ComplaintStatus.SUBMITTED, created.status());

        // Cancel the complaint
        mockMvc.perform(patch("/api/v1/complaints/" + complaintId + "/cancel")
                        .header("Authorization", "Bearer " + arunToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("CANCELLED")));

        // Verify status in repository
        Complaint cancelledComplaint = complaintRepository.findById(complaintId).orElseThrow();
        assertEquals(ComplaintStatus.CANCELLED, cancelledComplaint.getStatus());

        // Attempting to cancel again should fail because it is now CANCELLED
        mockMvc.perform(patch("/api/v1/complaints/" + complaintId + "/cancel")
                        .header("Authorization", "Bearer " + arunToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("Citizen duplicate upvote prevention via toggle")
    void testDuplicateUpvotePreventionAndToggle() throws Exception {
        Complaint target = complaintRepository.findAll().stream()
                .filter(c -> c.getStatus() == ComplaintStatus.SUBMITTED || c.getStatus() == ComplaintStatus.IN_PROGRESS)
                .findFirst()
                .orElseThrow();

        int initialUpvotes = target.getUpvotesCount();

        // 1st click: upvote added (+1)
        mockMvc.perform(post("/api/v1/complaints/" + target.getId() + "/upvote")
                        .header("Authorization", "Bearer " + arunToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // 2nd click: upvote removed (-1) preventing duplicate vote
        mockMvc.perform(post("/api/v1/complaints/" + target.getId() + "/upvote")
                        .header("Authorization", "Bearer " + arunToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        Complaint reloaded = complaintRepository.findById(target.getId()).orElseThrow();
        assertEquals(initialUpvotes, reloaded.getUpvotesCount());
    }

    @Test
    @DisplayName("Roads Official requesting official queue without departmentId only receives Roads complaints")
    void testRoadsOfficialQueueScopedWithoutDepartmentId() throws Exception {
        mockMvc.perform(get("/api/v1/official/complaints")
                        .header("Authorization", "Bearer " + roadsOfficialToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", not(empty())))
                .andExpect(jsonPath("$.data.content[*].departmentName", everyItem(containsString("Roads"))))
                .andExpect(jsonPath("$.data.content[*].departmentName", not(hasItem(containsString("Water")))));
    }

    @Test
    @DisplayName("Roads Official attempting to request Water department complaints is strictly constrained to Roads")
    void testRoadsOfficialAttemptingToRequestWaterDepartmentComplaintsIsRestricted() throws Exception {
        Long waterDeptId = waterOfficial.getDepartment().getId();

        mockMvc.perform(get("/api/v1/official/complaints?departmentId=" + waterDeptId)
                        .header("Authorization", "Bearer " + roadsOfficialToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content[*].departmentName", everyItem(containsString("Roads"))))
                .andExpect(jsonPath("$.data.content[*].departmentName", not(hasItem(containsString("Water")))));
    }

    @Test
    @DisplayName("Water Official requesting official queue without departmentId only receives Water complaints")
    void testWaterOfficialQueueScopedWithoutDepartmentId() throws Exception {
        mockMvc.perform(get("/api/v1/official/complaints")
                        .header("Authorization", "Bearer " + waterOfficialToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", not(empty())))
                .andExpect(jsonPath("$.data.content[*].departmentName", everyItem(containsString("Water"))))
                .andExpect(jsonPath("$.data.content[*].departmentName", not(hasItem(containsString("Roads")))));
    }

    @Test
    @DisplayName("Admin accessing complaints across departments receives issues from all departments")
    void testAdminAccessingOfficialQueueCrossDepartment() throws Exception {
        mockMvc.perform(get("/api/v1/official/complaints")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content[*].departmentName", hasItem(containsString("Roads"))))
                .andExpect(jsonPath("$.data.content[*].departmentName", hasItem(containsString("Water"))));
    }

    @Test
    @DisplayName("Officials retrieve assignment targets strictly from their own department")
    void testOfficialRetrievesAssignmentTargetsFromOwnDepartmentOnly() throws Exception {
        Long waterDeptId = waterOfficial.getDepartment().getId();

        // 1. Roads official requests assignees without param -> only Roads official returned
        mockMvc.perform(get("/api/v1/official/assignees")
                        .header("Authorization", "Bearer " + roadsOfficialToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[*].departmentName", everyItem(containsString("Roads"))))
                .andExpect(jsonPath("$.data[*].email", hasItem("roads.officer@civicpulse.gov")))
                .andExpect(jsonPath("$.data[*].email", not(hasItem("water.officer@civicpulse.gov"))));

        // 2. Roads official requests assignees with Water department ID -> still strictly forced to Roads
        mockMvc.perform(get("/api/v1/official/assignees?departmentId=" + waterDeptId)
                        .header("Authorization", "Bearer " + roadsOfficialToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[*].departmentName", everyItem(containsString("Roads"))))
                .andExpect(jsonPath("$.data[*].email", not(hasItem("water.officer@civicpulse.gov"))));

        // 3. Water official requests assignees -> only Water official returned
        mockMvc.perform(get("/api/v1/official/assignees")
                        .header("Authorization", "Bearer " + waterOfficialToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[*].departmentName", everyItem(containsString("Water"))))
                .andExpect(jsonPath("$.data[*].email", hasItem("water.officer@civicpulse.gov")))
                .andExpect(jsonPath("$.data[*].email", not(hasItem("roads.officer@civicpulse.gov"))));
    }

    @Test
    @DisplayName("Admin retrieves authorized assignment targets across all departments or filtered")
    void testAdminRetrievesAuthorizedAssignmentTargetsAcrossDepartments() throws Exception {
        Long waterDeptId = waterOfficial.getDepartment().getId();

        // 1. Admin without departmentId gets both Roads and Water officials
        mockMvc.perform(get("/api/v1/official/assignees")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[*].email", hasItem("roads.officer@civicpulse.gov")))
                .andExpect(jsonPath("$.data[*].email", hasItem("water.officer@civicpulse.gov")));

        // 2. Admin with specific departmentId gets only that department's officials
        mockMvc.perform(get("/api/v1/official/assignees?departmentId=" + waterDeptId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[*].email", hasItem("water.officer@civicpulse.gov")))
                .andExpect(jsonPath("$.data[*].email", not(hasItem("roads.officer@civicpulse.gov"))));
    }

    @Test
    @DisplayName("Official attempting to access admin-only /api/v1/admin/officials is blocked with 403")
    void testOfficialCannotAccessAdminOfficialsEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/officials")
                        .header("Authorization", "Bearer " + roadsOfficialToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
