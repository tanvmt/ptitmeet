package com.ptithcm.ptitmeet.dto.meeting;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ptithcm.ptitmeet.entity.enums.MeetingAccessType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MeetingInfoResponse {
    private String title;
    
    @JsonProperty("meeting_code")
    private String meetingCode;
    
    @JsonProperty("host_name")
    private String hostName;
    
    @JsonProperty("is_password_protected")
    private boolean isPasswordProtected;
    
    @JsonProperty("access_type")
    private MeetingAccessType accessType;
    
    private String status;
}