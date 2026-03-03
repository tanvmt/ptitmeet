package com.ptithcm.ptitmeet.dto.meeting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingSummaryResponse {
    private String duration;
    private int participants;
    private int messages;
}