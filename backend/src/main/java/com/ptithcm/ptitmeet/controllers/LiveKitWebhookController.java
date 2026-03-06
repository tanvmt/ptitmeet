package com.ptithcm.ptitmeet.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ptithcm.ptitmeet.services.LiveKitService;

import io.livekit.server.WebhookReceiver;
import livekit.LivekitWebhook.WebhookEvent;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/livekit")
@Slf4j
public class LiveKitWebhookController {

    private final LiveKitService liveKitService;
    private final WebhookReceiver webhookReceiver;

    public LiveKitWebhookController(
            LiveKitService liveKitService,
            @Value("${livekit.api.key}") String apiKey,
            @Value("${livekit.api.secret}") String apiSecret) {
        this.liveKitService = liveKitService;
        this.webhookReceiver = new WebhookReceiver(apiKey, apiSecret);
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody String body) {
        try {
            log.info("Nhận webhook từ LiveKit, body length: {}", body.length());

            // Xác thực webhook từ LiveKit bằng API key/secret
            WebhookEvent event = webhookReceiver.receive(body, authHeader);

            String eventType = event.getEvent();
            log.info("Webhook event type: {}", eventType);

            if ("egress_ended".equals(eventType)) {
                // Recording đã hoàn tất - cập nhật fileUrl
                handleEgressEnded(event);
            } else if ("egress_started".equals(eventType)) {
                log.info("Egress started: {}", event.getEgressInfo().getEgressId());
            }

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Lỗi xử lý webhook từ LiveKit: {}", e.getMessage(), e);
            return ResponseEntity.ok("OK"); // Vẫn trả 200 để LiveKit không retry liên tục
        }
    }

    private void handleEgressEnded(WebhookEvent event) {
        try {
            String egressId = event.getEgressInfo().getEgressId();
            log.info("Egress ended cho egressId: {}", egressId);

            String fileUrl = null;

            if (event.getEgressInfo().getFileResultsCount() > 0) {
                // Đối với EncodedFileOutput, file URL nằm trong file_results
                fileUrl = event.getEgressInfo().getFileResults(0).getLocation();
                log.info("File URL từ file_results: {}", fileUrl);
            }

            if (fileUrl == null || fileUrl.isEmpty()) {
                // Thử lấy từ file field (tùy version LiveKit SDK)
                if (event.getEgressInfo().hasFile()) {
                    fileUrl = event.getEgressInfo().getFile().getLocation();
                    log.info("File URL từ file field: {}", fileUrl);
                }
            }

            if (fileUrl != null && !fileUrl.isEmpty()) {
                liveKitService.updateRecordingCompleted(egressId, fileUrl);
                log.info("Đã cập nhật fileUrl cho egressId: {}", egressId);
            } else {
                log.warn("Không tìm thấy fileUrl trong webhook event cho egressId: {}", egressId);
                liveKitService.updateRecordingFailed(egressId);
            }
        } catch (Exception e) {
            log.error("Lỗi khi xử lý egress_ended: {}", e.getMessage(), e);
        }
    }
}

