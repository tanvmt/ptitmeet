package com.ptithcm.ptitmeet.controllers;

import com.ptithcm.ptitmeet.dto.ApiResponse;
import com.ptithcm.ptitmeet.entity.mysql.MeetingRecording;
import com.ptithcm.ptitmeet.services.LiveKitService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ptithcm.ptitmeet.exception.AppException;
import com.ptithcm.ptitmeet.exception.ErrorCode;

@RestController
@RequestMapping("/api/livekit/recordings")
@Slf4j
public class RecordingController {

    @Autowired
    private LiveKitService recordingService;

    private java.util.UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return java.util.UUID.fromString(authentication.getName());
    }

    // API: Bắt đầu ghi hình
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<MeetingRecording>> startRecording(@RequestParam String meetingCode) {
        try {
            MeetingRecording recording = recordingService.startRoomRecording(meetingCode, getCurrentUserId());

            return ResponseEntity.ok(ApiResponse.success(recording, "")); // Trả về object có chứa egressId cho React
        } catch (Exception e) {

            throw new AppException(ErrorCode.UN_START_RECORD_MEETING_ROOM);
        }
    }

    // API: Dừng ghi hình
    @PostMapping("/stop")
    public ResponseEntity<?> stopRecording(@RequestParam String egressId) {
        try {
            MeetingRecording recording = recordingService.stopRecording(egressId, getCurrentUserId());
            return ResponseEntity.ok(recording);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // API: Kiểm tra trạng thái recording (frontend poll sau khi stop)
    @GetMapping("/status")
    public ResponseEntity<?> getRecordingStatus(@RequestParam String egressId) {
        try {
            MeetingRecording recording = recordingService.getRecordingByEgressId(egressId, getCurrentUserId());
            return ResponseEntity.ok(recording);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<java.util.List<MeetingRecording>>> getMyRecordings() {
        java.util.List<MeetingRecording> recordings = recordingService.getRecordingsByOwnerId(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(recordings, "Lấy danh sách recordings thành công"));
    }
}
