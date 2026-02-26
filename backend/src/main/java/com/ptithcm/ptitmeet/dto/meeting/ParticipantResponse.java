package com.ptithcm.ptitmeet.dto.meeting;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class ParticipantResponse {
    private UUID participantId;      
    private UUID userId;             
    private String displayName;      
    private String email;            
    private String avatarUrl;        
    private String status;           
    private String requestTime;     
}