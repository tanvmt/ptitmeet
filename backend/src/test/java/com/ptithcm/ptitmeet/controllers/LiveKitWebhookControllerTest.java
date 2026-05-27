package com.ptithcm.ptitmeet.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ptithcm.ptitmeet.services.LiveKitService;

@ExtendWith(MockitoExtension.class)
class LiveKitWebhookControllerTest {

    @Mock
    private LiveKitService liveKitService;

    @Mock
    private com.ptithcm.ptitmeet.services.MeetingService meetingService;

    @Test
    void handleWebhookShouldReturnOkEvenForInvalidPayload() {
        LiveKitWebhookController controller = new LiveKitWebhookController(liveKitService, meetingService, "api-key", "api-secret");

        var response = controller.handleWebhook(null, "{}");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("OK", response.getBody());
        verifyNoInteractions(liveKitService);
        verifyNoInteractions(meetingService);
    }
}
