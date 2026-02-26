package com.ptithcm.ptitmeet.dto.meeting;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ptithcm.ptitmeet.entity.enums.MeetingAccessType;
import lombok.Data;

import jakarta.validation.constraints.Future;
import java.time.LocalDateTime;

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
    
    private String settings; 
}