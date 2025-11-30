package com.healthlink.domain.notification.entity;

import com.healthlink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "push_device_tokens", uniqueConstraints = {
        @UniqueConstraint(name = "uk_push_token", columnNames = {"token"})
})
@Getter
@Setter
@NoArgsConstructor
public class PushDeviceToken extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token", nullable = false, length = 512)
    private String token;

    @Column(name = "platform", nullable = false, length = 30)
    private String platform; // ANDROID / IOS / WEB

    @Column(name = "last_seen_at")
    private java.time.Instant lastSeenAt;
}
