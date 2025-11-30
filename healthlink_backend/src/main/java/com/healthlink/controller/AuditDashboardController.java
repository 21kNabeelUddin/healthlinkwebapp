package com.healthlink.controller;

import com.healthlink.dto.ResponseEnvelope;
import com.healthlink.security.audit.PhiAccessLogRepository;
import com.healthlink.security.audit.PhiAccessLog;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/audit")
@RequiredArgsConstructor
public class AuditDashboardController {
    private final PhiAccessLogRepository repository;

    @GetMapping("/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEnvelope<java.util.List<PhiAccessLog>> recent() {
        var logs = repository.findAll().stream()
                .sorted((a,b) -> b.getAccessedAt().compareTo(a.getAccessedAt()))
                .limit(100)
                .toList();
        return ResponseEnvelope.<java.util.List<PhiAccessLog>>builder()
                .data(logs)
                .meta(ResponseEnvelope.Meta.builder().version("v1").build())
                .traceId("audit-recent")
                .build();
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEnvelope<AuditStats> stats() {
        var now = Instant.now();
        var last24h = now.minus(24, ChronoUnit.HOURS);
        var logs = repository.findAll();
        long total = logs.size();
        long lastDayCount = logs.stream().filter(l -> l.getAccessedAt().isAfter(last24h)).count();
        Map<String, Long> byRole = logs.stream().collect(Collectors.groupingBy(PhiAccessLog::getRole, Collectors.counting()));
        Map<String, Long> byEntity = logs.stream().collect(Collectors.groupingBy(PhiAccessLog::getEntityType, Collectors.counting()));
        AuditStats stats = new AuditStats();
        stats.setTotal(total);
        stats.setLast24h(lastDayCount);
        stats.setByRole(byRole);
        stats.setByEntityType(byEntity);
        return ResponseEnvelope.<AuditStats>builder()
                .data(stats)
                .meta(ResponseEnvelope.Meta.builder().version("v1").build())
                .traceId("audit-stats")
                .build();
    }

    public static class AuditStats {
        private long total; private long last24h; private Map<String,Long> byRole; private Map<String,Long> byEntityType;
        public long getTotal() { return total; } public void setTotal(long v){total=v;}
        public long getLast24h(){return last24h;} public void setLast24h(long v){last24h=v;}
        public Map<String,Long> getByRole(){return byRole;} public void setByRole(Map<String,Long> v){byRole=v;}
        public Map<String,Long> getByEntityType(){return byEntityType;} public void setByEntityType(Map<String,Long> v){byEntityType=v;}
    }
}
