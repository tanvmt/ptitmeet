package com.ptithcm.ptitmeet.dto.meeting;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class MeetingHistoryResponse {
    private String meetingCode;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private boolean isHost;
}