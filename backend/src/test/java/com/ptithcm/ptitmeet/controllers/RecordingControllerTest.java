package com.ptithcm.ptitmeet.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ptithcm.ptitmeet.entity.mysql.MeetingRecording;
import com.ptithcm.ptitmeet.exception.AppException;
import com.ptithcm.ptitmeet.exception.ErrorCode;
import com.ptithcm.ptitmeet.services.LiveKitService;

@ExtendWith(MockitoExtension.class)
class RecordingControllerTest {

    @Mock
    private LiveKitService recordingService;

    @InjectMocks
    private RecordingController recordingController;

    @Test
    void startRecordingShouldWrapRecordingInApiResponse() {
        MeetingRecording recording = new MeetingRecording();
        recording.setEgressId("egress-1");

        when(recordingService.startRoomRecording("room-123")).thenReturn(recording);

        var response = recordingController.startRecording("room-123");

        assertEquals(1000, response.getBody().getCode());
        assertEquals(recording, response.getBody().getData());
    }

    @Test
    void startRecordingShouldTranslateUnexpectedErrors() {
        when(recordingService.startRoomRecording("room-123")).thenThrow(new RuntimeException("boom"));

        AppException exception = assertThrows(AppException.class, () -> recordingController.startRecording("room-123"));

        assertEquals(ErrorCode.UN_START_RECORD_MEETING_ROOM, exception.getErrorCode());
    }

    @Test
    void stopRecordingShouldReturnBadRequestWhenServiceFails() {
        when(recordingService.stopRecording("egress-1")).thenThrow(new RuntimeException("stop failed"));

        var response = recordingController.stopRecording("egress-1");

        assertEquals(400, response.getStatusCode().value());
        assertEquals("stop failed", response.getBody());
    }
}
