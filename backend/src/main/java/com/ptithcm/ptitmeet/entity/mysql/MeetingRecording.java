package com.ptithcm.ptitmeet.entity.mysql;


import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_recordings")
@Data
public class MeetingRecording {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String roomName; // Tên phòng họp

    @Column(nullable = false, unique = true)
    private String egressId; 

    private String status; // Các trạng thái: STARTING, RECORDING, STOPPING, COMPLETED, FAILED

    private String fileUrl; // Đường dẫn file sau khi hoàn thành

    private LocalDateTime createdAt;
} 