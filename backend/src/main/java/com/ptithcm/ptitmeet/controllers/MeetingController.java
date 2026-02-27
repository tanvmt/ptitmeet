package com.ptithcm.ptitmeet.controllers;

import com.ptithcm.ptitmeet.dto.ApiResponse;
import com.ptithcm.ptitmeet.dto.meeting.ApprovalRequest;
import com.ptithcm.ptitmeet.dto.meeting.CreateMeetingRequest;
import com.ptithcm.ptitmeet.dto.meeting.MeetingInfoResponse;
import com.ptithcm.ptitmeet.dto.meeting.JoinMeetingRequest;
import com.ptithcm.ptitmeet.dto.meeting.JoinMeetingResponse;
import com.ptithcm.ptitmeet.dto.meeting.ParticipantResponse;
import com.ptithcm.ptitmeet.entity.mysql.Meeting;
import com.ptithcm.ptitmeet.services.MeetingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.ptithcm.ptitmeet.dto.meeting.MeetingHistoryResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

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
}