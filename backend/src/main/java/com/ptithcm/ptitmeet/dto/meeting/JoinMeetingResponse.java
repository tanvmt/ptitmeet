package com.ptithcm.ptitmeet.dto.meeting;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String settings;
    @JsonProperty("isOwner")
    private boolean isOwner;
    private String currentHostId;
}
