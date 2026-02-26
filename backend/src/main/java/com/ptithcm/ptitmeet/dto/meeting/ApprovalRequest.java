package com.ptithcm.ptitmeet.dto.meeting;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ApprovalRequest {
    private Long participantId; 
    private String action;    
}