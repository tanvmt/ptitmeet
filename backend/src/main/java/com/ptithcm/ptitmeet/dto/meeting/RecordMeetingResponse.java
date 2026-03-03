package com.ptithcm.ptitmeet.dto.meeting;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level=AccessLevel.PRIVATE)
public class RecordMeetingResponse {
  String recordMeetingResponse;
  String egressId;
  boolean isStopSuccess;
  
  
}
