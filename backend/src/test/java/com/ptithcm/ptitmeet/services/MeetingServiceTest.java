package com.ptithcm.ptitmeet.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptithcm.ptitmeet.dto.meeting.ApprovalRequest;
import com.ptithcm.ptitmeet.dto.meeting.CreateMeetingRequest;
import com.ptithcm.ptitmeet.dto.meeting.JoinMeetingRequest;
import com.ptithcm.ptitmeet.dto.meeting.JoinMeetingResponse;
import com.ptithcm.ptitmeet.entity.enums.MeetingAccessType;
import com.ptithcm.ptitmeet.entity.enums.MeetingStatus;
import com.ptithcm.ptitmeet.entity.enums.ParticipantApprovalStatus;
import com.ptithcm.ptitmeet.entity.mysql.Meeting;
import com.ptithcm.ptitmeet.entity.mysql.Participant;
import com.ptithcm.ptitmeet.entity.mysql.ParticipantSession;
import com.ptithcm.ptitmeet.entity.mysql.User;
import com.ptithcm.ptitmeet.exception.AppException;
import com.ptithcm.ptitmeet.exception.ErrorCode;
import com.ptithcm.ptitmeet.repositories.ChatMessageRepository;
import com.ptithcm.ptitmeet.repositories.MeetingFeedbackRepository;
import com.ptithcm.ptitmeet.repositories.MeetingInvitationRepository;
import com.ptithcm.ptitmeet.repositories.MeetingRepository;
import com.ptithcm.ptitmeet.repositories.ParticipantRepository;
import com.ptithcm.ptitmeet.repositories.ParticipantSessionRepository;
import com.ptithcm.ptitmeet.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @Mock
    private MeetingInvitationRepository meetingInvitationRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private ParticipantSessionRepository sessionRepository;

    @Mock
    private LiveKitService liveKitService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private EmailService emailService;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private MeetingFeedbackRepository feedbackRepository;

    @InjectMocks
    private MeetingService meetingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(meetingService, "objectMapper", new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "JUnit");
        ReflectionTestUtils.setField(meetingService, "request", request);
    }

    @Test
    void createInstantMeetingShouldApplyDefaultsWhenSettingsMissing() {
        UUID hostId = UUID.randomUUID();
        CreateMeetingRequest request = new CreateMeetingRequest();
        request.setTitle("");

        when(userRepository.existsById(hostId)).thenReturn(true);
        when(meetingRepository.existsByMeetingCode(any(String.class))).thenReturn(false);
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Meeting meeting = meetingService.createInstantMeeting(hostId, request);

        assertEquals(hostId, meeting.getHostId());
        assertEquals("Cuộc họp nhanh", meeting.getTitle());
        assertEquals(MeetingStatus.ACTIVE, meeting.getStatus());
        assertFalse(meeting.getSettings().isEmpty());
    }

    @Test
    void scheduleMeetingShouldRejectInvalidTimeRange() {
        UUID hostId = UUID.randomUUID();
        CreateMeetingRequest request = new CreateMeetingRequest();
        request.setStartTime(LocalDateTime.now().plusHours(2));
        request.setEndTime(LocalDateTime.now().plusHours(1));

        when(userRepository.findById(hostId)).thenReturn(Optional.of(User.builder().userId(hostId).build()));

        AppException exception = assertThrows(AppException.class,
                () -> meetingService.scheduleMeeting(hostId, request));

        assertEquals(ErrorCode.INVALID_TIME_RANGE, exception.getErrorCode());
        verify(meetingRepository, never()).save(any(Meeting.class));
    }

    @Test
    void joinMeetingShouldReturnPendingWhenScheduledMeetingHasNotStarted() {
        UUID hostId = UUID.randomUUID();
        UUID attendeeId = UUID.randomUUID();
        Meeting meeting = Meeting.builder()
                .meetingId(UUID.randomUUID())
                .hostId(hostId)
                .meetingCode("room-123")
                .status(MeetingStatus.SCHEDULED)
                .accessType(MeetingAccessType.OPEN)
                .settings("{\"waitingRoom\":false}")
                .build();
        User attendee = User.builder()
                .userId(attendeeId)
                .email("guest@example.com")
                .fullName("Guest User")
                .build();

        when(meetingRepository.findByMeetingCode("room-123")).thenReturn(Optional.of(meeting));
        when(userRepository.findByUserId(attendeeId)).thenReturn(Optional.of(attendee));
        when(participantRepository.findByMeetingAndUser(meeting, attendee)).thenReturn(Optional.empty());
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> {
            Participant participant = invocation.getArgument(0);
            if (participant.getParticipantId() == null) {
                participant.setParticipantId(UUID.randomUUID());
            }
            return participant;
        });

        JoinMeetingResponse response = meetingService.joinMeeting(
                "room-123",
                JoinMeetingRequest.builder().displayName("Guest").build(),
                attendeeId);

        assertEquals("PENDING", response.getStatus());
        assertEquals("The meeting has not started yet. Please wait for the host to join.", response.getMessage());
        verify(liveKitService, never()).generateJoinToken(any(String.class), any(String.class), any(String.class));
    }

    @Test
    void processParticipantApprovalShouldSendApprovedResponseAndCreateSession() {
        UUID hostId = UUID.randomUUID();
        UUID guestId = UUID.randomUUID();
        Meeting meeting = Meeting.builder()
                .meetingId(UUID.randomUUID())
                .hostId(hostId)
                .meetingCode("room-123")
                .status(MeetingStatus.ACTIVE)
                .build();
        User host = User.builder().userId(hostId).build();
        User guest = User.builder().userId(guestId).fullName("Guest User").build();
        Participant participant = Participant.builder()
                .participantId(UUID.randomUUID())
                .meeting(meeting)
                .user(guest)
                .displayName("Guest User")
                .approvalStatus(ParticipantApprovalStatus.PENDING)
                .build();
        ApprovalRequest request = new ApprovalRequest();
        request.setParticipantId(participant.getParticipantId());
        request.setAction("APPROVED");

        when(meetingRepository.findByMeetingCode("room-123")).thenReturn(Optional.of(meeting));
        when(userRepository.findByUserId(hostId)).thenReturn(Optional.of(host));
        when(participantRepository.findById(participant.getParticipantId())).thenReturn(Optional.of(participant));
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.findActiveSessionsByMeetingCode("room-123")).thenReturn(List.of());
        when(sessionRepository.save(any(ParticipantSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(liveKitService.generateJoinToken("room-123", "Guest User", guestId.toString())).thenReturn("join-token");
        when(liveKitService.getLivekitUrl()).thenReturn("wss://livekit.example");

        meetingService.processParticipantApproval("room-123", hostId, request);

        ArgumentCaptor<JoinMeetingResponse> responseCaptor = ArgumentCaptor.forClass(JoinMeetingResponse.class);
        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/meeting/room-123/user/" + guestId),
                responseCaptor.capture());
        assertEquals("APPROVED", responseCaptor.getValue().getStatus());
        assertEquals("join-token", responseCaptor.getValue().getToken());
    }
}
