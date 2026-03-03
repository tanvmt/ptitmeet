package com.ptithcm.ptitmeet.controllers;

import com.ptithcm.ptitmeet.dto.ApiResponse;
import com.ptithcm.ptitmeet.entity.mysql.MeetingRecording;
import com.ptithcm.ptitmeet.services.LiveKitService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ptithcm.ptitmeet.exception.AppException;
import com.ptithcm.ptitmeet.exception.ErrorCode;

@RestController
@RequestMapping("/api/livekit/recordings")
@CrossOrigin(origins = "http://localhost:3000") // Cho phép React gọi API
@Slf4j
public class RecordingController {

    @Autowired
    private LiveKitService recordingService;

    // API: Bắt đầu ghi hình
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<MeetingRecording>> startRecording(@RequestParam String meetingCode) {
        try {
            MeetingRecording recording = recordingService.startRoomRecording(meetingCode);

            return ResponseEntity.ok(ApiResponse.success(recording, "")); // Trả về object có chứa egressId cho React
        } catch (Exception e) {

           throw  new  AppException(ErrorCode.UN_START_RECORD_MEETING_ROOM);
        }
    }

    // API: Dừng ghi hình
    @PostMapping("/stop")
    public ResponseEntity<?> stopRecording(@RequestParam String egressId) {
        try {
            MeetingRecording recording = recordingService.stopRecording(egressId);
            return ResponseEntity.ok(recording);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}