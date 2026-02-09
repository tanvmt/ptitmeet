package com.ptithcm.ptitmeet.entity.mysql;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import java.time.LocalDateTime;
import com.ptithcm.ptitmeet.entity.enums.MeetingStatus;
import com.ptithcm.ptitmeet.entity.enums.MeetingAccessType;

@Entity
@Table(name = "meetings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meeting {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "meeting_id")
    private UUID meetingId;

    @Column(name = "host_id", nullable = false)
    private UUID hostId;

    @Column(name = "meeting_code", unique = true, nullable = false, length = 20)
    private String meetingCode;

    private String title;
    private String password;

    @Column(name = "is_instant")
    private boolean isInstant;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "allowed_domain")
    private String allowedDomain;

    @Column(name = "access_type")
    @Enumerated(EnumType.STRING)
    private MeetingAccessType accessType;

    @Enumerated(EnumType.STRING)
    private MeetingStatus status; 

    @Column(columnDefinition = "TEXT") 
    private String settings;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (status == null)
            status = MeetingStatus.ACTIVE;
        if (accessType == null)
            accessType = MeetingAccessType.TRUSTED;
        createdAt = LocalDateTime.now();
    }
}