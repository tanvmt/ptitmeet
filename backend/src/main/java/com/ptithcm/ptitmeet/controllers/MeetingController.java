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
import com.ptithcm.ptitmeet.entity.mongodb.ChatMessage;
import com.ptithcm.ptitmeet.entity.mysql.Meeting;
import com.ptithcm.ptitmeet.repositories.ChatMessageRepository;
import com.ptithcm.ptitmeet.services.MeetingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    private final ChatMessageRepository chatMessageRepository;

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(authentication.getName()); 
    }

    @PostMapping("/instant")
    public ResponseEntity<ApiResponse<Meeting>> createInstant(
            @RequestBody(required = false) CreateMeetingRequest request) {
        
        if (request == null) request = new CreateMeetingRequest();
        
        Meeting meeting = meetingService.createInstantMeeting(getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(meeting, "Tạo phòng họp ngay thành công"));
    }

    @PostMapping("/schedule")
    public ResponseEntity<ApiResponse<Meeting>> schedule(
            @RequestBody @Valid CreateMeetingRequest request) { 
            
        Meeting meeting = meetingService.scheduleMeeting(getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(meeting, "Lên lịch họp thành công"));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<MeetingHistoryResponse>>> getHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(defaultValue = "ALL") String role,
            @RequestParam(defaultValue = "ALL") String status) {

        Page<MeetingHistoryResponse> historyPage = meetingService.getUserMeetingHistory(
                getCurrentUserId(), role, status, page - 1, size);

        return ResponseEntity.ok(ApiResponse.success(historyPage, "Lấy lịch sử họp thành công"));
    }

    @GetMapping("/up-next")
    public ResponseEntity<ApiResponse<MeetingHistoryResponse>> getUpNext() {
        MeetingHistoryResponse upNext = meetingService.getUpNextMeeting(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(upNext, "Lấy Up Next thành công"));
    }

    @GetMapping("/my-meetings")
    public ResponseEntity<ApiResponse<List<Meeting>>> getMyMeetings() {
        List<Meeting> meetings = meetingService.getMyMeetings(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(meetings, "Lấy danh sách thành công"));
    }

    @GetMapping("/{code}/info")
    public ResponseEntity<ApiResponse<MeetingInfoResponse>> getInfo(@PathVariable String code) {
        MeetingInfoResponse info = meetingService.getMeetingInfo(code);
        return ResponseEntity.ok(ApiResponse.success(info, "Thông tin phòng hợp lệ"));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable String code) {
        meetingService.cancelMeeting(getCurrentUserId(), code);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã hủy cuộc họp"));
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
        return ApiResponse.success(response, "Tham gia phòng họp thành công");
    }

    @GetMapping("/{code}/waiting-room")
    public ResponseEntity<ApiResponse<List<ParticipantResponse>>> getWaitingRoom(
            @PathVariable String code
    ) {
        List<ParticipantResponse> list = meetingService.getWaitingParticipants(code, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy danh sách chờ thành công"));
    }

    @PostMapping("/{code}/approval")
    public ResponseEntity<ApiResponse<Void>> approveParticipant(
            @PathVariable String code,
            @RequestBody ApprovalRequest request
    ) {
        meetingService.processParticipantApproval(code, getCurrentUserId(), request);
        
        String msg = "APPROVED".equalsIgnoreCase(request.getAction()) ? "Đã duyệt thành viên" : "Đã từ chối thành viên";
        return ResponseEntity.ok(ApiResponse.success(null, msg));
    }

    @GetMapping("/{code}/chat/history")
    public ResponseEntity<ApiResponse<List<ChatMessage>>> getChatHistory(@PathVariable String code) {
        List<ChatMessage> history = chatMessageRepository.findByMeetingCodeOrderByTimestampAsc(code);
        return ResponseEntity.ok(ApiResponse.success(history, "Lấy lịch sử chat thành công"));
    }

    @PostMapping("/{code}/leave")
    public ResponseEntity<ApiResponse<Void>> leave(@PathVariable String code) {
        meetingService.leaveMeeting(code, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Đã rời phòng"));
    }

    @PostMapping("/{code}/end")
    public ResponseEntity<ApiResponse<Void>> endForAll(@PathVariable String code) {
        meetingService.endMeetingForAll(code, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Đã kết thúc cuộc họp cho tất cả"));
    }

    @GetMapping("/{code}/summary")
    public ResponseEntity<ApiResponse<MeetingSummaryResponse>> getSummary(
            @PathVariable String code,
            @RequestParam(defaultValue = "LEAVE") String action) {
        MeetingSummaryResponse summary = meetingService.getMeetingSummary(code, getCurrentUserId(), action);
        return ResponseEntity.ok(ApiResponse.success(summary, "Lấy thống kê thành công"));
    }

    @PostMapping("/{code}/feedback")
    public ResponseEntity<ApiResponse<Void>> submitFeedback(
            @PathVariable String code,
            @RequestBody FeedbackRequest request) {
        meetingService.submitFeedback(code, getCurrentUserId(), request.getRating());
        return ResponseEntity.ok(ApiResponse.success(null, "Cảm ơn bạn đã đánh giá"));
    }
}