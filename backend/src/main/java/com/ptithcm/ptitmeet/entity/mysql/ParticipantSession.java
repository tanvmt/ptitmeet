package com.ptithcm.ptitmeet.entity.mysql;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import java.time.LocalDateTime;
import com.ptithcm.ptitmeet.entity.enums.SessionStatus;

@Entity
@Table(name = "participant_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipantSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt; 

    @Enumerated(EnumType.STRING)
    private SessionStatus status;

    @Column(name = "device_info")
    private String deviceInfo; 

    @Column(name = "ip_address")
    private String ipAddress;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
        status = SessionStatus.ACTIVE;
    }
}