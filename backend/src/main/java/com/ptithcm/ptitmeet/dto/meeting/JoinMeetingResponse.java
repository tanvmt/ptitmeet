package com.ptithcm.ptitmeet.dto.meeting;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JoinMeetingResponse {
    private String token;            
    private String serverUrl;        
    private String status;           
    private String role;            
    private String message;
}