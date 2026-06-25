package com.ptithcm.ptitmeet.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ptithcm.ptitmeet.entity.mysql.Meeting;
import com.ptithcm.ptitmeet.exception.AppException;
import com.ptithcm.ptitmeet.exception.ErrorCode;
import com.ptithcm.ptitmeet.repositories.MeetingRecordingRepository;
import com.ptithcm.ptitmeet.repositories.MeetingRepository;

@ExtendWith(MockitoExtension.class)
class LiveKitServiceTest {

    @Mock
    private MeetingRecordingRepository recordingRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @InjectMocks
    private LiveKitService liveKitService;

    @Test
    void startRoomRecordingShouldNormalizeMeetingCodeBeforeLookup() {
        UUID ownerId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Meeting meeting = Meeting.builder()
                .meetingId(UUID.randomUUID())
                .hostId(ownerId)
                .ownerId(ownerId)
                .meetingCode("nv0-nzy3-7eo")
                .build();

        when(meetingRepository.findByMeetingCodeIgnoreCase("NV0-NZY3-7EO")).thenReturn(Optional.of(meeting));

        AppException exception = assertThrows(AppException.class,
                () -> liveKitService.startRoomRecording("  NV0-NZY3-7EO  ", requesterId));

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        verify(meetingRepository).findByMeetingCodeIgnoreCase("NV0-NZY3-7EO");
        verifyNoInteractions(recordingRepository);
    }

    @Test
    void startRoomRecordingShouldRejectBlankMeetingCode() {
        AppException exception = assertThrows(AppException.class,
                () -> liveKitService.startRoomRecording("   ", UUID.randomUUID()));

        assertEquals(ErrorCode.MEETING_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(meetingRepository, recordingRepository);
    }
}
