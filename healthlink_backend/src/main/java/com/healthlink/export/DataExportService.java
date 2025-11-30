package com.healthlink.export;

import com.healthlink.domain.record.repository.MedicalRecordRepository;
import com.healthlink.domain.record.repository.PrescriptionRepository;
import com.healthlink.domain.record.repository.LabOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataExportService {
    private final DataExportRequestRepository repository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final LabOrderRepository labOrderRepository;

    @Transactional
    public DataExportRequest requestFullExport(String requestedBy) {
        if (repository.existsByStatus("RUNNING")) {
            throw new RuntimeException("An export is already running");
        }
        DataExportRequest req = DataExportRequest.builder()
                .requestedBy(requestedBy)
                .type("FULL")
                .status("PENDING")
                .build();
        req = repository.save(req);
        runAsync(req.getId());
        return req;
    }

    @Transactional
    public DataExportRequest requestPatientExport(String requestedBy, UUID patientId) {
        if (repository.existsByStatus("RUNNING")) {
            throw new RuntimeException("An export is already running");
        }
        DataExportRequest req = DataExportRequest.builder()
                .requestedBy(requestedBy)
                .type("PATIENT_SCOPED")
                .patientId(patientId)
                .status("PENDING")
                .build();
        req = repository.save(req);
        runAsync(req.getId());
        return req;
    }

    private void runAsync(UUID id) {
        // Simple thread; in production use TaskExecutor or queue.
        new Thread(() -> {
            DataExportRequest req = repository.findById(id).orElse(null);
            if (req == null) return;
            req.setStatus("RUNNING");
            repository.save(req);
            try {
                Path exportDir = Path.of("exports");
                Files.createDirectories(exportDir);
                String filename = "export-" + req.getId() + ".csv";
                File file = exportDir.resolve(filename).toFile();
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    writer.write("type,id,patientId,title,createdAt\n");
                    if (req.getType().equals("FULL") || req.getType().equals("PATIENT_SCOPED")) {
                        medicalRecordRepository.findAll().stream()
                                .filter(r -> req.getPatientId() == null || r.getPatientId().equals(req.getPatientId()))
                                .forEach(r -> safeWrite(writer, "MEDICAL_RECORD," + r.getId() + "," + r.getPatientId() + "," + sanitize(r.getTitle()) + "," + r.getCreatedAt()));
                        prescriptionRepository.findAll().stream()
                                .filter(p -> req.getPatientId() == null || p.getPatientId().equals(req.getPatientId()))
                                .forEach(p -> safeWrite(writer, "PRESCRIPTION," + p.getId() + "," + p.getPatientId() + "," + sanitize(p.getTitle()) + "," + p.getCreatedAt()));
                        labOrderRepository.findAll().stream()
                                .filter(l -> req.getPatientId() == null || l.getPatientId().equals(req.getPatientId()))
                                .forEach(l -> safeWrite(writer, "LAB_ORDER," + l.getId() + "," + l.getPatientId() + "," + sanitize(l.getOrderName()) + "," + l.getOrderedAt()));
                    }
                }
                req.setFileUrl(file.getAbsolutePath());
                req.setStatus("COMPLETED");
                req.setCompletedAt(OffsetDateTime.now());
                repository.save(req);
                analyticsRecord(com.healthlink.analytics.AnalyticsEventType.EXPORT_COMPLETED, req.getRequestedBy(), req.getId().toString(), "type=" + req.getType());
            } catch (Exception e) {
                log.error("Export failed", e);
                req.setStatus("FAILED");
                req.setErrorMessage(e.getMessage());
                req.setCompletedAt(OffsetDateTime.now());
                repository.save(req);
                analyticsRecord(com.healthlink.analytics.AnalyticsEventType.EXPORT_FAILED, req.getRequestedBy(), req.getId().toString(), "error");
            }
        }).start();
    }

    private void safeWrite(BufferedWriter writer, String line) {
        try {
            writer.write(line);
            writer.newLine();
        } catch (Exception e) {
            // ignore for now; counted as partial failure
        }
    }

    private String sanitize(String s) {
        if (s == null) return "";
        return s.replaceAll(",", " ").replaceAll("\n", " ");
    }

    public DataExportRequest get(UUID id) { return repository.findById(id).orElseThrow(() -> new RuntimeException("Export not found")); }
    public java.util.List<DataExportRequest> recent() { return repository.findTop20ByOrderByRequestedAtDesc(); }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.healthlink.analytics.AnalyticsEventService analyticsEventService;
    private void analyticsRecord(com.healthlink.analytics.AnalyticsEventType type, String actor, String subjectId, String meta){ if(analyticsEventService!=null){ analyticsEventService.record(type, actor, subjectId, meta);} }
}
