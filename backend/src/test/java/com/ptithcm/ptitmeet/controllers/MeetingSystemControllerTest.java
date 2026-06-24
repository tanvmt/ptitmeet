package com.ptithcm.ptitmeet.controllers;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptithcm.ptitmeet.services.MeetingService;

@ExtendWith(MockitoExtension.class)
class MeetingSystemControllerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MeetingService meetingService;

    private MeetingSystemController meetingSystemController;

    @BeforeEach
    void setUp() {
        meetingSystemController = new MeetingSystemController(messagingTemplate, new ObjectMapper(), meetingService);
    }

    @Test
    void shouldBroadcastSupportedPlainAction() {
        meetingSystemController.broadcastSystemAction("room-123", "MUTE_ALL");

        verify(meetingService).handleSystemAction("room-123", "MUTE_ALL");
        verify(messagingTemplate).convertAndSend("/topic/meeting/room-123/system", "MUTE_ALL");
    }

    @Test
    void shouldBroadcastSupportedJsonAction() {
        String payload = "{\"type\":\"KICK_PARTICIPANT\",\"targetParticipantId\":\"user-1\"}";

        meetingSystemController.broadcastSystemAction("room-123", payload);

        verify(meetingService).handleSystemAction("room-123", payload);
        verify(messagingTemplate).convertAndSend("/topic/meeting/room-123/system", payload);
    }

    @Test
    void shouldIgnoreUnsupportedPayload() {
        meetingSystemController.broadcastSystemAction("room-123", "{\"type\":\"UNKNOWN\"}");

        verify(messagingTemplate, never()).convertAndSend(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.<Object>any());
        verify(meetingService, never()).handleSystemAction(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString());
    }
}
