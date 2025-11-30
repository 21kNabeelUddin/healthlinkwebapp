package com.healthlink.domain.video.entity;

import com.healthlink.common.entity.BaseEntity;
import com.healthlink.domain.appointment.entity.Appointment;
import com.healthlink.domain.user.entity.Staff;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "video_calls")
@Getter
@Setter
public class VideoCall extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_staff_id")
    private Staff assignedStaff;

    @Column(name = "janus_session_id")
    private Long janusSessionId;

    @Column(name = "janus_handle_id")
    private Long janusHandleId;

    @Column(name = "room_secret")
    private String roomSecret;

    @Column(name = "recording_url")
    private String recordingUrl;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "staff_joined_at")
    private LocalDateTime staffJoinedAt;
}
