package com.healthlink.controller;

import com.healthlink.dto.ResponseEnvelope;
import com.healthlink.export.DataExportRequest;
import com.healthlink.export.DataExportService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/exports")
@RequiredArgsConstructor
public class DataExportController {
    private final DataExportService service;

    @PostMapping("/full")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEnvelope<DataExportRequest> requestFull(Authentication auth) {
        var req = service.requestFullExport(auth.getName());
        return wrap(req, "export-full-request");
    }

    @PostMapping("/patient/{patientId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEnvelope<DataExportRequest> requestPatient(Authentication auth, @PathVariable UUID patientId) {
        var req = service.requestPatientExport(auth.getName(), patientId);
        return wrap(req, "export-patient-request");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEnvelope<DataExportRequest> get(@PathVariable UUID id) {
        return wrap(service.get(id), "export-get");
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEnvelope<java.util.List<DataExportRequest>> recent() {
        return ResponseEnvelope.<java.util.List<DataExportRequest>>builder()
                .data(service.recent())
                .meta(ResponseEnvelope.Meta.builder().version("v1").build())
                .traceId("export-recent")
                .build();
    }

    private <T> ResponseEnvelope<T> wrap(T data, String trace) {
        return ResponseEnvelope.<T>builder()
                .data(data)
                .meta(ResponseEnvelope.Meta.builder().version("v1").build())
                .traceId(trace)
                .build();
    }

    @Data
    public static class ExportRequest { private String scope; private UUID patientId; }
}
