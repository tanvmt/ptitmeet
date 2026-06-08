package com.ptithcm.ptitmeet.controllers;

import java.util.Set;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptithcm.ptitmeet.services.MeetingService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MeetingSystemController {

    private static final Set<String> SUPPORTED_ACTIONS = Set.of(
            "MUTE_ALL",
            "STOP_CAMERA_ALL",
            "KICK_ALL",
            "MUTE_PARTICIPANT",
            "STOP_CAMERA_PARTICIPANT",
            "KICK_PARTICIPANT",
            "RECORDING_STARTED",
            "RECORDING_STOPPED");

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final MeetingService meetingService;

    @MessageMapping("/meeting/{code}/system")
    public void broadcastSystemAction(@DestinationVariable String code, @Payload String payload) {
        if (payload == null) {
            return;
        }

        String normalizedPayload = payload.trim();
        if (normalizedPayload.isEmpty()) {
            return;
        }

        if (!isSupportedPayload(normalizedPayload)) {
            return;
        }

        meetingService.handleSystemAction(code, normalizedPayload);
        messagingTemplate.convertAndSend("/topic/meeting/" + code + "/system", normalizedPayload);
    }

    private boolean isSupportedPayload(String payload) {
        if (!payload.startsWith("{")) {
            return SUPPORTED_ACTIONS.contains(payload);
        }

        try {
            JsonNode node = objectMapper.readTree(payload);
            return SUPPORTED_ACTIONS.contains(node.path("type").asText());
        } catch (Exception exception) {
            return false;
        }
    }
}
