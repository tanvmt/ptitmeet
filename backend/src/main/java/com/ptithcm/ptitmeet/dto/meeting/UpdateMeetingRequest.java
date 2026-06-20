package com.ptithcm.ptitmeet.dto.meeting;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ptithcm.ptitmeet.entity.enums.MeetingAccessType;
import jakarta.validation.constraints.Future;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateMeetingRequest {

    // Tất cả field đều optional — chỉ update field nào được gửi lên (non-null)
    private String title;

    @JsonProperty("start_time")
    @Future(message = "Thời gian bắt đầu phải ở tương lai")
    private LocalDateTime startTime;

    @JsonProperty("end_time")
    private LocalDateTime endTime;

    @JsonProperty("access_type")
    private MeetingAccessType accessType;
}
