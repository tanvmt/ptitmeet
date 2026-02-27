package com.ptithcm.ptitmeet.dto.meeting;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ptithcm.ptitmeet.entity.enums.MeetingAccessType;
import lombok.Data;

import jakarta.validation.constraints.Future;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateMeetingRequest {
    private String title;
    
    private String password;
    
    @JsonProperty("start_time")
    @Future(message = "Thời gian bắt đầu phải ở tương lai")
    private LocalDateTime startTime;
    
    @JsonProperty("end_time")
    private LocalDateTime endTime;
    
    @JsonProperty("access_type")
    private MeetingAccessType accessType; 
    
    @JsonProperty("participant_emails")
    private List<String> participantEmails;

    private String settings; 
}