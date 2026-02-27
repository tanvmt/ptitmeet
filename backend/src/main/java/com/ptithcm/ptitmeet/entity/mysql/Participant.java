package com.ptithcm.ptitmeet.entity.mysql;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import java.time.LocalDateTime;
import com.ptithcm.ptitmeet.entity.enums.ParticipantRole;
import com.ptithcm.ptitmeet.entity.enums.ParticipantApprovalStatus;

@Entity
@Table(name = "participants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Participant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "participant_id")
    private UUID participantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "guest_identity")
    private String guestIdentity;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private ParticipantRole role;

    @Column(name = "approval_status")
    @Enumerated(EnumType.STRING)
    private ParticipantApprovalStatus approvalStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (approvalStatus == null) {
            approvalStatus = ParticipantApprovalStatus.PENDING;
        }
        if (role == null) {
            role = ParticipantRole.ATTENDEE;
        }
    }
}