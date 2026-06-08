package com.ptithcm.ptitmeet.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptithcm.ptitmeet.dto.meeting.ApprovalRequest;
import com.ptithcm.ptitmeet.dto.meeting.CreateMeetingRequest;
import com.ptithcm.ptitmeet.dto.meeting.JoinMeetingRequest;
import com.ptithcm.ptitmeet.dto.meeting.JoinMeetingResponse;
import com.ptithcm.ptitmeet.entity.mongodb.ChatMessage;
import com.ptithcm.ptitmeet.entity.mysql.Meeting;
import com.ptithcm.ptitmeet.services.MeetingService;

@ExtendWith(MockitoExtension.class)
class MeetingControllerTest {

    @Mock
    private MeetingService meetingService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MeetingController meetingController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createInstantShouldUseAuthenticatedUserAndDefaultRequestWhenNull() {
        UUID userId = UUID.randomUUID();
        setAuthenticatedUser(userId);
        Meeting meeting = Meeting.builder()
                .meetingCode("abc-defg-hij")
                .hostId(userId)
                .build();

        when(meetingService.createInstantMeeting(any(UUID.class), any(CreateMeetingRequest.class))).thenReturn(meeting);

        var response = meetingController.createInstant(null);

        assertEquals(1000, response.getBody().getCode());
        assertEquals(meeting, response.getBody().getData());
        verify(meetingService).createInstantMeeting(any(UUID.class), any(CreateMeetingRequest.class));
    }

    @Test
    void joinMeetingShouldWrapServiceResponse() {
        UUID userId = UUID.randomUUID();
        setAuthenticatedUser(userId);
        JoinMeetingRequest request = JoinMeetingRequest.builder().password("secret").build();
        JoinMeetingResponse joinResponse = JoinMeetingResponse.builder()
                .status("APPROVED")
                .role("ATTENDEE")
                .token("token")
                .serverUrl("ws://server")
                .build();

        when(meetingService.joinMeeting("room-123", request, userId)).thenReturn(joinResponse);

        var response = meetingController.joinMeeting("room-123", request);

        assertEquals(1000, response.getCode());
        assertEquals(joinResponse, response.getData());
        verify(meetingService).joinMeeting("room-123", request, userId);
    }

    @Test
    void approveParticipantShouldReturnActionSpecificMessage() {
        UUID userId = UUID.randomUUID();
        setAuthenticatedUser(userId);
        ApprovalRequest request = new ApprovalRequest();
        request.setAction("REJECTED");

        var response = meetingController.approveParticipant("room-123", request);

        assertEquals("Đã từ chối thành viên", response.getBody().getMessage());
        verify(meetingService).processParticipantApproval("room-123", userId, request);
    }

    @Test
    void getChatHistoryShouldReturnRepositoryData() {
        UUID userId = UUID.randomUUID();
        setAuthenticatedUser(userId);
        ChatMessage chatMessage = ChatMessage.builder()
                .id("msg-1")
                .meetingCode("room-123")
                .content("hello")
                .build();
        when(meetingService.getChatHistory("room-123", userId))
                .thenReturn(List.of(chatMessage));

        var response = meetingController.getChatHistory("room-123");

        assertEquals(1, response.getBody().getData().size());
        assertEquals("hello", response.getBody().getData().get(0).getContent());
        verify(meetingService).getChatHistory("room-123", userId);
    }

    private void setAuthenticatedUser(UUID userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null));
    }
}
