package com.healthlink.domain.record.controller;

import com.healthlink.domain.record.dto.MedicalRecordRequest;
import com.healthlink.domain.record.dto.MedicalRecordResponse;
import com.healthlink.domain.record.service.MedicalRecordService;
import com.healthlink.security.annotation.PhiAccess;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * MedicalRecordController
 * CRUD + PDF export for encrypted medical records.
 * HIPAA: Methods returning PHI annotated with {@link PhiAccess} for audit.
 */
@RestController
@RequestMapping("/api/v1/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService service;

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public MedicalRecordResponse create(@Valid @RequestBody MedicalRecordRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN') or (hasRole('PATIENT') and @ownershipGuard.isRecordOwner(#id))")
    @PhiAccess(reason = "medical_record_view", entityType = MedicalRecordResponse.class, idParam = "id")
    public MedicalRecordResponse get(@PathVariable UUID id) { return service.get(id); }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN') or (hasRole('PATIENT') and principal.id == #patientId)")
    @PhiAccess(reason = "medical_record_list", entityType = MedicalRecordResponse.class, idParam = "patientId")
    public List<MedicalRecordResponse> list(@PathVariable UUID patientId) { return service.listForPatient(patientId); }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PATIENT')")
    public MedicalRecordResponse update(@PathVariable UUID id, @Valid @RequestBody MedicalRecordRequest request) { return service.update(id, request); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) { service.delete(id); return ResponseEntity.noContent().build(); }

    @GetMapping("/{id}/export")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    @PhiAccess(reason = "medical_record_export", entityType = MedicalRecordResponse.class, idParam = "id")
    public ResponseEntity<byte[]> exportPdf(@PathVariable UUID id) {
        MedicalRecordResponse record = service.get(id);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, baos);
            doc.open();
            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font bodyFont = new Font(Font.HELVETICA, 11);
            doc.add(new Paragraph("Medical Record Export", titleFont));
            doc.add(new Paragraph("Record ID: " + record.getId(), bodyFont));
            doc.add(new Paragraph("Patient ID: " + record.getPatientId(), bodyFont));
            doc.add(new Paragraph("Title: " + record.getTitle(), bodyFont));
            if (record.getSummary() != null) { doc.add(new Paragraph("Summary: " + record.getSummary(), bodyFont)); }
            doc.add(new Paragraph("Created At: " + record.getCreatedAt(), bodyFont));
            doc.add(new Paragraph("Updated At: " + record.getUpdatedAt(), bodyFont));
            doc.add(new Paragraph("Details:", bodyFont));
            doc.add(new Paragraph(record.getDetails(), bodyFont));
            if (record.getAttachmentUrl() != null) { doc.add(new Paragraph("Attachment URL: " + record.getAttachmentUrl(), bodyFont)); }
            PdfPTable meta = new PdfPTable(2);
            meta.addCell("Exported");
            meta.addCell(java.time.OffsetDateTime.now().toString());
            doc.add(meta);
            doc.close();
            byte[] pdfBytes = baos.toByteArray();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "medical-record-" + record.getId() + ".pdf");
            headers.setContentLength(pdfBytes.length);
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
