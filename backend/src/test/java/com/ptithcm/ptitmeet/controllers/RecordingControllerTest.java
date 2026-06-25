package com.ptithcm.ptitmeet.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void startRecordingShouldWrapRecordingInApiResponse() {
        UUID userId = UUID.randomUUID();
        setAuthenticatedUser(userId);
        MeetingRecording recording = new MeetingRecording();
        recording.setEgressId("egress-1");

        when(recordingService.startRoomRecording("room-123", userId)).thenReturn(recording);

        var response = recordingController.startRecording("room-123");

        assertEquals(1000, response.getBody().getCode());
        assertEquals(recording, response.getBody().getData());
    }

    @Test
    void startRecordingShouldTranslateUnexpectedErrors() {
        UUID userId = UUID.randomUUID();
        setAuthenticatedUser(userId);
        when(recordingService.startRoomRecording("room-123", userId)).thenThrow(new RuntimeException("boom"));

        AppException exception = assertThrows(AppException.class, () -> recordingController.startRecording("room-123"));

        assertEquals(ErrorCode.UN_START_RECORD_MEETING_ROOM, exception.getErrorCode());
    }

    @Test
    void startRecordingShouldPreserveApplicationErrors() {
        UUID userId = UUID.randomUUID();
        setAuthenticatedUser(userId);
        when(recordingService.startRoomRecording("room-123", userId))
                .thenThrow(new AppException(ErrorCode.MEETING_NOT_FOUND));

        AppException exception = assertThrows(AppException.class, () -> recordingController.startRecording("room-123"));

        assertEquals(ErrorCode.MEETING_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void stopRecordingShouldReturnBadRequestWhenServiceFails() {
        UUID userId = UUID.randomUUID();
        setAuthenticatedUser(userId);
        when(recordingService.stopRecording("egress-1", userId)).thenThrow(new RuntimeException("stop failed"));

        var response = recordingController.stopRecording("egress-1");

        assertEquals(400, response.getStatusCode().value());
        assertEquals("stop failed", response.getBody());
    }

    private void setAuthenticatedUser(UUID userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null));
    }
}
