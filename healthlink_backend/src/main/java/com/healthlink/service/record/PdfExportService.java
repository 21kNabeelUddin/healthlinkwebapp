package com.healthlink.service.record;

import com.healthlink.domain.record.entity.MedicalRecord;
import com.itextpdf.html2pdf.HtmlConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * PDF Export Service for Medical Records
 * Generates PDF documents for prescriptions, lab results, and medical summaries
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfExportService {

    private final TemplateEngine templateEngine;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    /**
     * Export medical record to PDF
     */
    public byte[] exportMedicalRecordToPdf(MedicalRecord record, Map<String, Object> additionalData) {
        try {
            Context context = new Context();
            context.setVariable("record", record);
            context.setVariable("patientName", additionalData.get("patientName"));
            context.setVariable("doctorName", additionalData.get("doctorName"));
            context.setVariable("generatedDate", DATE_FORMATTER.format(java.time.LocalDate.now()));

            if (additionalData != null) {
                additionalData.forEach(context::setVariable);
            }

            String htmlContent = templateEngine.process("pdf/medical-record", context);

            return convertHtmlToPdf(htmlContent);

        } catch (Exception e) {
            log.error("Failed to export medical record to PDF", e);
            throw new RuntimeException("PDF export failed", e);
        }
    }

    /**
     * Export prescription to PDF
     */
    public byte[] exportPrescriptionToPdf(Map<String, Object> prescriptionData) {
        try {
            Context context = new Context();
            context.setVariables(prescriptionData);
            context.setVariable("generatedDate", DATE_FORMATTER.format(java.time.LocalDate.now()));

            String htmlContent = templateEngine.process("pdf/prescription", context);

            return convertHtmlToPdf(htmlContent);

        } catch (Exception e) {
            log.error("Failed to export prescription to PDF", e);
            throw new RuntimeException("PDF export failed", e);
        }
    }

    /**
     * Export lab result to PDF
     */
    public byte[] exportLabResultToPdf(Map<String, Object> labResultData) {
        try {
            Context context = new Context();
            context.setVariables(labResultData);
            context.setVariable("generatedDate", DATE_FORMATTER.format(java.time.LocalDate.now()));

            String htmlContent = templateEngine.process("pdf/lab-result", context);

            return convertHtmlToPdf(htmlContent);

        } catch (Exception e) {
            log.error("Failed to export lab result to PDF", e);
            throw new RuntimeException("PDF export failed", e);
        }
    }

    /**
     * Export patient medical summary to PDF
     */
    public byte[] exportPatientSummaryToPdf(Map<String, Object> summaryData) {
        try {
            Context context = new Context();
            context.setVariables(summaryData);
            context.setVariable("generatedDate", DATE_FORMATTER.format(java.time.LocalDate.now()));

            String htmlContent = templateEngine.process("pdf/patient-summary", context);

            return convertHtmlToPdf(htmlContent);

        } catch (Exception e) {
            log.error("Failed to export patient summary to PDF", e);
            throw new RuntimeException("PDF export failed", e);
        }
    }

    /**
     * Convert HTML content to PDF bytes
     */
    private byte[] convertHtmlToPdf(String htmlContent) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // iText HTML2PDF conversion
            HtmlConverter.convertToPdf(htmlContent, outputStream);

            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Failed to convert HTML to PDF", e);
            throw new RuntimeException("HTML to PDF conversion failed", e);
        }
    }
}
