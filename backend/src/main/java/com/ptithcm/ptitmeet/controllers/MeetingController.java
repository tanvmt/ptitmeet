package com.ptithcm.ptitmeet.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.ptithcm.ptitmeet.dto.ApiResponse;
import com.ptithcm.ptitmeet.dto.meeting.ApprovalRequest;
import com.ptithcm.ptitmeet.dto.meeting.CreateMeetingRequest;
import com.ptithcm.ptitmeet.dto.meeting.JoinMeetingRequest;
import com.ptithcm.ptitmeet.dto.meeting.JoinMeetingResponse;
import com.ptithcm.ptitmeet.dto.meeting.FeedbackRequest;
import com.ptithcm.ptitmeet.dto.meeting.MeetingSummaryResponse;
import com.ptithcm.ptitmeet.dto.meeting.MeetingHistoryResponse;
import com.ptithcm.ptitmeet.dto.meeting.MeetingInfoResponse;
import com.ptithcm.ptitmeet.dto.meeting.ParticipantResponse;
import com.ptithcm.ptitmeet.dto.meeting.UpdateMeetingRequest;
import com.ptithcm.ptitmeet.entity.mysql.Meeting;
import com.ptithcm.ptitmeet.services.MeetingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    private final ObjectMapper objectMapper;

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(authentication.getName()); 
    }

    @PostMapping("/instant")
    public ResponseEntity<ApiResponse<Meeting>> createInstant(
            @RequestBody(required = false) CreateMeetingRequest request) {
        
        if (request == null) request = new CreateMeetingRequest();
        
        Meeting meeting = meetingService.createInstantMeeting(getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(meeting, "Instant meeting created successfully"));
    }

    @PostMapping("/schedule")
    public ResponseEntity<ApiResponse<Meeting>> schedule(
            @RequestBody @Valid CreateMeetingRequest request) { 
            
        Meeting meeting = meetingService.scheduleMeeting(getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(meeting, "Meeting scheduled successfully"));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<MeetingHistoryResponse>>> getHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(defaultValue = "ALL") String role,
            @RequestParam(defaultValue = "ALL") String status) {

        Page<MeetingHistoryResponse> historyPage = meetingService.getUserMeetingHistory(
                getCurrentUserId(), role, status, page - 1, size);

        return ResponseEntity.ok(ApiResponse.success(historyPage, "Meeting history retrieved successfully"));
    }

    @GetMapping("/up-next")
    public ResponseEntity<ApiResponse<MeetingHistoryResponse>> getUpNext() {
        MeetingHistoryResponse upNext = meetingService.getUpNextMeeting(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(upNext, "Up-next meeting retrieved successfully"));
    }

    @GetMapping("/my-meetings")
    public ResponseEntity<ApiResponse<List<Meeting>>> getMyMeetings() {
        List<Meeting> meetings = meetingService.getMyMeetings(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(meetings, "Meetings retrieved successfully"));
    }

    @GetMapping("/{code}/info")
    public ResponseEntity<ApiResponse<MeetingInfoResponse>> getInfo(@PathVariable String code) {
        MeetingInfoResponse info = meetingService.getMeetingInfo(code);
        return ResponseEntity.ok(ApiResponse.success(info, "Meeting info retrieved"));
    }

    @PutMapping("/{code}")
    public ResponseEntity<ApiResponse<Meeting>> updateMeeting(
            @PathVariable String code,
            @RequestBody @Valid UpdateMeetingRequest request) {
        Meeting meeting = meetingService.updateMeeting(getCurrentUserId(), code, request);
        return ResponseEntity.ok(ApiResponse.success(meeting, "Meeting updated successfully"));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable String code) {
        meetingService.cancelMeeting(getCurrentUserId(), code);
        return ResponseEntity.ok(ApiResponse.success(null, "Meeting cancelled successfully"));
    }

    @PostMapping("/{code}/join")
    public ApiResponse<JoinMeetingResponse> joinMeeting(
            @PathVariable String code,
            @RequestBody(required = false) JoinMeetingRequest request
    ) {
        if (request == null) {
            request = new JoinMeetingRequest();
        }

        JoinMeetingResponse response = meetingService.joinMeeting(code, request, getCurrentUserId());
        return ApiResponse.success(response, "Joined meeting successfully");
    }

    @GetMapping("/{code}/waiting-room")
    public ResponseEntity<ApiResponse<List<ParticipantResponse>>> getWaitingRoom(
            @PathVariable String code
    ) {
        List<ParticipantResponse> list = meetingService.getWaitingParticipants(code, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(list, "Waiting room list retrieved successfully"));
    }

    @PostMapping("/{code}/approval")
    public ResponseEntity<ApiResponse<Void>> approveParticipant(
            @PathVariable String code,
            @RequestBody ApprovalRequest request
    ) {
        meetingService.processParticipantApproval(code, getCurrentUserId(), request);
        
        String msg = "APPROVED".equalsIgnoreCase(request.getAction()) ? "Participant approved" : "Participant rejected";
        return ResponseEntity.ok(ApiResponse.success(null, msg));
    }

    @GetMapping("/{code}/chat/history")
    public ResponseEntity<ApiResponse<List<com.ptithcm.ptitmeet.entity.mongodb.ChatMessage>>> getChatHistory(@PathVariable String code) {
        List<com.ptithcm.ptitmeet.entity.mongodb.ChatMessage> history = meetingService.getChatHistory(code, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(history, "Chat history retrieved successfully"));
    }

    @PostMapping("/{code}/leave")
    public ResponseEntity<ApiResponse<Void>> leave(@PathVariable String code) {
        meetingService.leaveMeeting(code, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Left meeting successfully"));
    }

    @PostMapping("/{code}/end")
    public ResponseEntity<ApiResponse<Void>> endForAll(@PathVariable String code) {
        meetingService.endMeetingForAll(code, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Meeting ended for all participants"));
    }

    @GetMapping("/{code}/summary")
    public ResponseEntity<ApiResponse<MeetingSummaryResponse>> getSummary(
            @PathVariable String code,
            @RequestParam(defaultValue = "LEAVE") String action) {
        MeetingSummaryResponse summary = meetingService.getMeetingSummary(code, getCurrentUserId(), action);
        return ResponseEntity.ok(ApiResponse.success(summary, "Meeting summary retrieved successfully"));
    }

    @PostMapping("/{code}/feedback")
    public ResponseEntity<ApiResponse<Void>> submitFeedback(
            @PathVariable String code,
            @RequestBody FeedbackRequest request) {
        meetingService.submitFeedback(code, getCurrentUserId(), request.getRating());
        return ResponseEntity.ok(ApiResponse.success(null, "Thank you for your feedback"));
    }

    @GetMapping("/{code}/settings")
    public ResponseEntity<ApiResponse<String>> getSettings(@PathVariable String code) {
        String settings = meetingService.getMeetingSettings(code);
        return ResponseEntity.ok(ApiResponse.success(settings, "Meeting settings retrieved successfully"));
    }

    @PutMapping("/{code}/settings")
    public ResponseEntity<ApiResponse<Meeting>> updateSettings(
            @PathVariable String code,
            @RequestBody java.util.Map<String, Object> settings) {
        try {
            String settingsJson = objectMapper.writeValueAsString(settings);
            Meeting meeting = meetingService.updateMeetingSettings(code, getCurrentUserId(), settingsJson);
            return ResponseEntity.ok(ApiResponse.success(meeting, "Meeting settings updated successfully"));
        } catch (Exception e) {
            throw new RuntimeException("Invalid settings format", e);
        }
    }
}
