package com.ptithcm.ptitmeet.dto.meeting;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
public class ApprovalRequest {
    private UUID participantId; 
    private String action;    
}