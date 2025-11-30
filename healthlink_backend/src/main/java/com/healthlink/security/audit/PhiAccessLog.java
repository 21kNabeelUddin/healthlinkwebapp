package com.healthlink.security.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.Instant;

@Entity
@Table(name = "phi_access_logs")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhiAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private String entityType;

    @Column(nullable = false)
    private String entityId;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private Instant accessedAt;

    @Column(nullable = false)
    private String ipAddress;

    @Column(nullable = false)
    private String userAgent;

    @Column(nullable = false)
    private String traceId;

    private String requestId;
}
