package com.civicpulse.engine.sla;

import com.civicpulse.complaint.model.Complaint;
import com.civicpulse.complaint.model.ComplaintStatusHistory;
import com.civicpulse.complaint.repository.ComplaintRepository;
import com.civicpulse.complaint.repository.ComplaintStatusHistoryRepository;
import com.civicpulse.engine.priority.PriorityCalculationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class SlaEscalationScheduler {

    private static final Logger log = LoggerFactory.getLogger(SlaEscalationScheduler.class);

    private final ComplaintRepository complaintRepository;
    private final ComplaintStatusHistoryRepository historyRepository;
    private final PriorityCalculationEngine priorityEngine;

    public SlaEscalationScheduler(
            ComplaintRepository complaintRepository,
            ComplaintStatusHistoryRepository historyRepository,
            PriorityCalculationEngine priorityEngine
    ) {
        this.complaintRepository = complaintRepository;
        this.historyRepository = historyRepository;
        this.priorityEngine = priorityEngine;
    }

    @Scheduled(cron = "${app.sla.escalation-cron:0 */15 * * * *}")
    @Transactional
    public void runEscalationAndScoreRefresh() {
        Instant now = Instant.now();
        log.info("Starting SLA escalation and priority score aging refresh cycle at {}", now);

        // 1. Escalate overdue complaints
        List<Complaint> overdueComplaints = complaintRepository.findOverdueUnescalatedComplaints(now);
        for (Complaint complaint : overdueComplaints) {
            complaint.setIsEscalated(true);
            complaint.setEscalationLevel(complaint.getEscalationLevel() + 1);

            ComplaintStatusHistory history = ComplaintStatusHistory.builder()
                    .complaint(complaint)
                    .fromStatus(complaint.getStatus())
                    .toStatus(complaint.getStatus())
                    .remarks("Automated SLA deadline breach escalation. Escalation Level set to " + complaint.getEscalationLevel())
                    .build();
            historyRepository.save(history);

            log.warn("Escalated complaint #{} ({}) due to SLA breach past {}",
                    complaint.getTrackingNumber(), complaint.getTitle(), complaint.getDueDate());
        }

        // 2. Refresh priority scores for all active complaints to reflect aging time
        List<Complaint> activeComplaints = complaintRepository.findAllActiveComplaints();
        int updatedCount = 0;
        for (Complaint complaint : activeComplaints) {
            double newScore = priorityEngine.calculateScore(complaint, now);
            if (Double.compare(newScore, complaint.getPriorityScore()) != 0) {
                complaint.setPriorityScore(newScore);
                updatedCount++;
            }
        }

        complaintRepository.saveAll(activeComplaints);
        log.info("Escalation cycle completed: {} complaints escalated, {} priority scores recalculated",
                overdueComplaints.size(), updatedCount);
    }
}
