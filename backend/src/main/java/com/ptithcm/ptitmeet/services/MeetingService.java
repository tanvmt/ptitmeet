package com.ptithcm.ptitmeet.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptithcm.ptitmeet.dto.meeting.ApprovalRequest;
import com.ptithcm.ptitmeet.dto.meeting.CreateMeetingRequest;
import com.ptithcm.ptitmeet.dto.meeting.JoinMeetingRequest;
import com.ptithcm.ptitmeet.dto.meeting.JoinMeetingResponse;
import com.ptithcm.ptitmeet.dto.meeting.MeetingHistoryResponse;
import com.ptithcm.ptitmeet.dto.meeting.MeetingInfoResponse;
import com.ptithcm.ptitmeet.dto.meeting.MeetingSummaryResponse;
import com.ptithcm.ptitmeet.dto.meeting.ParticipantResponse;
import com.ptithcm.ptitmeet.entity.enums.MeetingAccessType;
import com.ptithcm.ptitmeet.entity.enums.MeetingStatus;
import com.ptithcm.ptitmeet.entity.enums.ParticipantApprovalStatus;
import com.ptithcm.ptitmeet.entity.enums.ParticipantRole;
import com.ptithcm.ptitmeet.entity.enums.SessionStatus;
import com.ptithcm.ptitmeet.entity.mysql.Meeting;
import com.ptithcm.ptitmeet.entity.mysql.MeetingInvitation;
import com.ptithcm.ptitmeet.entity.mysql.MeetingFeedback;
import com.ptithcm.ptitmeet.entity.mysql.Participant;
import com.ptithcm.ptitmeet.entity.mysql.ParticipantSession;
import com.ptithcm.ptitmeet.entity.mysql.User;
import com.ptithcm.ptitmeet.exception.AppException;
import com.ptithcm.ptitmeet.exception.ErrorCode;
import com.ptithcm.ptitmeet.repositories.ChatMessageRepository;
import com.ptithcm.ptitmeet.repositories.MeetingInvitationRepository;
import com.ptithcm.ptitmeet.repositories.MeetingRepository;
import com.ptithcm.ptitmeet.repositories.MeetingFeedbackRepository;
import com.ptithcm.ptitmeet.repositories.ParticipantRepository;
import com.ptithcm.ptitmeet.repositories.ParticipantSessionRepository;
import com.ptithcm.ptitmeet.repositories.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingInvitationRepository meetingInvitationRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final ParticipantSessionRepository sessionRepository;
    private final LiveKitService liveKitService;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private MeetingFeedbackRepository feedbackRepository;

    public Meeting createInstantMeeting(UUID hostId, CreateMeetingRequest request) {
        if (!userRepository.existsById(hostId)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        String meetingCode = generateUniqueMeetingCode();
        String title = (request.getTitle() == null || request.getTitle().isEmpty())
                ? "Cuộc họp nhanh"
                : request.getTitle();

        Meeting meeting = Meeting.builder()
                .hostId(hostId)
                .ownerId(hostId)
                .meetingCode(meetingCode)
                .title(title)
                .isInstant(true)
                .status(MeetingStatus.ACTIVE)
                .startTime(LocalDateTime.now())
                .accessType(MeetingAccessType.TRUSTED)
                .settings(request.getSettings())
                .build();

        if (request.getSettings() == null || request.getSettings().isEmpty()) {
            String defaultSettings = "{" +
                    "\"waitingRoom\": true," +
                    "\"muteAudioOnEntry\": false," +
                    "\"muteVideoOnEntry\": false," +
                    "\"chatEnabled\": true," +
                    "\"screenShareEnabled\": true" +
                    "}";
            meeting.setSettings(defaultSettings);
        } else {
            meeting.setSettings(request.getSettings());
        }

        return meetingRepository.save(meeting);
    }

    @Transactional
    public Meeting scheduleMeeting(UUID hostId, CreateMeetingRequest request) {
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        if (request.getStartTime() == null) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        if (request.getEndTime() != null && request.getEndTime().isBefore(request.getStartTime())) {
            throw new AppException(ErrorCode.INVALID_TIME_RANGE);
        }

        String meetingCode = generateUniqueMeetingCode();

        Meeting newMeeting = Meeting.builder()
                .hostId(hostId)
                .ownerId(hostId)
                .meetingCode(meetingCode)
                .title(request.getTitle())
                .password(request.getPassword())
                .isInstant(false)
                .status(MeetingStatus.SCHEDULED)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .accessType(request.getAccessType() != null ? request.getAccessType() : MeetingAccessType.TRUSTED)
                .settings(request.getSettings())
                .build();

        if (request.getSettings() == null || request.getSettings().isEmpty()) {
            String defaultSettings = "{" +
                    "\"waitingRoom\": true," +
                    "\"muteAudioOnEntry\": false," +
                    "\"muteVideoOnEntry\": false," +
                    "\"chatEnabled\": true," +
                    "\"screenShareEnabled\": true" +
                    "}";
            newMeeting.setSettings(defaultSettings);
        } else {
            newMeeting.setSettings(request.getSettings());
        }

        Meeting meeting = meetingRepository.save(newMeeting);

        System.out.println("Danh sách email nhận được từ Frontend: " + request.getParticipantEmails());

        if (request.getParticipantEmails() != null && !request.getParticipantEmails().isEmpty()) {

            List<String> uniqueEmails = request.getParticipantEmails().stream().distinct().toList();

            for (String email : uniqueEmails) {
                User invitedUser = userRepository.findByEmail(email).orElse(null);

                MeetingInvitation invitation = MeetingInvitation.builder()
                        .meeting(meeting)
                        .email(email)
                        .user(invitedUser)
                        .build();

                meetingInvitationRepository.save(invitation);

                emailService.sendMeetingInvite(
                        email,
                        meeting.getMeetingCode(),
                        meeting.getTitle(),
                        meeting.getStartTime(),
                        host.getFullName());
            }
        }

        return meeting;
    }

    public MeetingInfoResponse getMeetingInfo(String code) {
        Meeting meeting = meetingRepository.findByMeetingCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));

        if (meeting.getStatus() == MeetingStatus.FINISHED || meeting.getStatus() == MeetingStatus.CANCELED) {
            throw new AppException(ErrorCode.MEETING_ALREADY_FINISHED);
        }

        User host = userRepository.findById(meeting.getHostId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return MeetingInfoResponse.builder()
                .title(meeting.getTitle())
                .meetingCode(meeting.getMeetingCode())
                .hostName(host.getFullName())
                .isPasswordProtected(meeting.getPassword() != null && !meeting.getPassword().isEmpty())
                .accessType(meeting.getAccessType())
                .status(meeting.getStatus().toString())
                .build();
    }

    public List<Meeting> getMyMeetings(UUID userId) {
        return meetingRepository.findOwnedMeetingsOrderByStartTimeDesc(userId);
    }

    public void cancelMeeting(UUID userId, String code) {
        Meeting meeting = meetingRepository.findByMeetingCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));

        if (!meeting.getHostId().equals(userId)) {
            throw new AppException(ErrorCode.HOST_ONLY_ACTION);
        }

        meeting.setStatus(MeetingStatus.CANCELED);
        meetingRepository.save(meeting);

        // Thông báo tất cả participants qua WebSocket
        messagingTemplate.convertAndSend("/topic/meeting/" + code + "/system", "MEETING_CANCELED");
    }

    @Transactional
    public JoinMeetingResponse joinMeeting(String meetingCode, JoinMeetingRequest request, UUID userId) {
        Meeting meeting = meetingRepository.findByMeetingCode(meetingCode)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (meeting.getStatus() == MeetingStatus.FINISHED) {
            throw new AppException(ErrorCode.MEETING_ALREADY_FINISHED);
        }
        if (meeting.getStatus() == MeetingStatus.CANCELED) {
            throw new AppException(ErrorCode.MEETING_CANCELED);
        }

        boolean isRuntimeHost = meeting.getHostId().equals(user.getUserId());
        boolean isOwner = getMeetingOwnerId(meeting).equals(user.getUserId());
        ParticipantRole role = isRuntimeHost ? ParticipantRole.HOST : ParticipantRole.ATTENDEE;

        if (!isRuntimeHost && !isOwner && meeting.getPassword() != null && !meeting.getPassword().isEmpty()) {
            if (request.getPassword() == null || !request.getPassword().equals(meeting.getPassword())) {
                throw new AppException(ErrorCode.INVALID_MEETING_PASSWORD);
            }
        }

        ParticipantApprovalStatus targetStatus;

        if (isRuntimeHost || isOwner) {
            targetStatus = ParticipantApprovalStatus.APPROVED;

            if (meeting.getStatus() == MeetingStatus.SCHEDULED) {
                meeting.setStatus(MeetingStatus.ACTIVE);
                meeting.setStartTime(LocalDateTime.now());
                meetingRepository.save(meeting);
                messagingTemplate.convertAndSend("/topic/meeting/" + meetingCode + "/waiting-room", "HOST_JOINED");
            }
        } else {
            if (meeting.getStatus() == MeetingStatus.SCHEDULED) {
                targetStatus = ParticipantApprovalStatus.PENDING;
            } else {
                targetStatus = determineParticipantStatus(meeting, user);
            }
        }

        Participant participant = participantRepository.findByMeetingAndUser(meeting, user)
                .orElseGet(() -> {
                    Participant newP = new Participant();
                    newP.setMeeting(meeting);
                    newP.setUser(user);
                    String displayName = (request.getDisplayName() != null && !request.getDisplayName().isEmpty())
                            ? request.getDisplayName()
                            : user.getFullName();
                    newP.setDisplayName(displayName);
                    return newP;
                });

        participant.setRole(role);
        participant = participantRepository.save(participant);

        SessionStatus latestSessionStatus = getLatestSessionStatus(participant);
        if (!isRuntimeHost && !isOwner && latestSessionStatus == SessionStatus.KICKED) {
            targetStatus = ParticipantApprovalStatus.PENDING;
        }

        if (participant.getApprovalStatus() != ParticipantApprovalStatus.APPROVED || latestSessionStatus == SessionStatus.KICKED) {
            participant.setApprovalStatus(targetStatus);
            participant = participantRepository.save(participant);
        }

        if (participant.getApprovalStatus() == ParticipantApprovalStatus.PENDING) {
            ParticipantResponse notiData = ParticipantResponse.builder()
                    .participantId(participant.getParticipantId())
                    .userId(user.getUserId())
                    .displayName(participant.getDisplayName())
                    .status("PENDING")
                    .build();

            messagingTemplate.convertAndSend("/topic/meeting/" + meetingCode + "/admin", notiData);

            String message = (meeting.getStatus() == MeetingStatus.SCHEDULED)
                    ? "The meeting has not started yet. Please wait for the host to join."
                    : "You are in the waiting room. Please wait for the host to let you in.";

            return JoinMeetingResponse.builder()
                    .status("PENDING")
                    .message(message)
                    .settings(meeting.getSettings())
                    .isOwner(isOwner)
                    .currentHostId(meeting.getHostId() != null ? meeting.getHostId().toString() : null)
                    .build();
        }

        if (participant.getApprovalStatus() == ParticipantApprovalStatus.REJECTED) {
            throw new AppException(ErrorCode.MEETING_REJECTED);
        }

        createNewSession(participant);
        String token = liveKitService.generateJoinToken(meetingCode, user.getFullName(), userId.toString());

        return JoinMeetingResponse.builder()
                .token(token)
                .serverUrl(liveKitService.getLivekitUrl())
                .role(role.name())
                .status(participant.getApprovalStatus().name())
                .settings(meeting.getSettings())
                .isOwner(isOwner)
                .currentHostId(meeting.getHostId() != null ? meeting.getHostId().toString() : null)
                .build();
    }

    public List<ParticipantResponse> getWaitingParticipants(String meetingCode, UUID hostId) {
        Meeting meeting = meetingRepository.findByMeetingCode(meetingCode)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));

        User host = userRepository.findByUserId(hostId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!meeting.getHostId().equals(host.getUserId())) {
            throw new AppException(ErrorCode.HOST_ONLY_ACTION);
        }

        List<Participant> pendingList = participantRepository.findAllByMeetingAndApprovalStatus(
                meeting,
                ParticipantApprovalStatus.PENDING);

        return pendingList.stream().map(p -> ParticipantResponse.builder()
                .participantId(p.getParticipantId())
                .userId(p.getUser().getUserId())
                .displayName(p.getUser().getFullName())
                .email(p.getUser().getEmail())
                .avatarUrl(p.getUser().getAvatarUrl())
                .status(p.getApprovalStatus().name())
                .requestTime(p.getCreatedAt() != null ? p.getCreatedAt().toString() : "")
                .build()).collect(Collectors.toList());
    }

    public void processParticipantApproval(String meetingCode, UUID hostId, ApprovalRequest request) {
        Meeting meeting = meetingRepository.findByMeetingCode(meetingCode)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));

        User host = userRepository.findByUserId(hostId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!meeting.getHostId().equals(host.getUserId())) {
            throw new AppException(ErrorCode.HOST_ONLY_ACTION);
        }

        Participant participant = participantRepository.findById(request.getParticipantId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!participant.getMeeting().getMeetingId().equals(meeting.getMeetingId())) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        if ("APPROVED".equalsIgnoreCase(request.getAction())) {
            participant.setApprovalStatus(ParticipantApprovalStatus.APPROVED);
            participantRepository.save(participant);

            createNewSession(participant);

            User guestUser = participant.getUser();
            String token = liveKitService.generateJoinToken(meetingCode, guestUser.getFullName(),
                    guestUser.getUserId().toString());

            JoinMeetingResponse approvalResponse = JoinMeetingResponse.builder()
                    .status("APPROVED")
                    .role("ATTENDEE")
                    .token(token)
                    .serverUrl(liveKitService.getLivekitUrl())
                    .settings(meeting.getSettings())
                    .isOwner(false)
                    .currentHostId(meeting.getHostId() != null ? meeting.getHostId().toString() : null)
                    .build();

            messagingTemplate.convertAndSend(
                    "/topic/meeting/" + meetingCode + "/user/" + guestUser.getUserId(),
                    approvalResponse);
        } else if ("REJECTED".equalsIgnoreCase(request.getAction())) {
            participant.setApprovalStatus(ParticipantApprovalStatus.REJECTED);
            participantRepository.save(participant);

            JoinMeetingResponse rejectResponse = JoinMeetingResponse.builder()
                    .status("REJECTED")
                    .message("Chủ phòng đã từ chối yêu cầu tham gia.")
                    .isOwner(false)
                    .currentHostId(meeting.getHostId() != null ? meeting.getHostId().toString() : null)
                    .build();

            messagingTemplate.convertAndSend(
                    "/topic/meeting/" + meetingCode + "/user/" + participant.getUser().getUserId(),
                    rejectResponse);
        } else {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        participantRepository.save(participant);
    }

    public Page<MeetingHistoryResponse> getUserMeetingHistory(UUID userId, String role, String statusStr, int page,
            int size) {
        MeetingStatus statusEnum = null;
        if (statusStr != null && !statusStr.equals("ALL")) {
            try {
                statusEnum = MeetingStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.INVALID_KEY);
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime"));

        Page<Meeting> meetingPage = meetingRepository.findMeetingHistoryWithFilters(userId, role, statusEnum, pageable);

        return meetingPage.map(meeting -> {
            boolean isOwner = getMeetingOwnerId(meeting).equals(userId);
            return MeetingHistoryResponse.builder()
                    .meetingCode(meeting.getMeetingCode())
                    .title(meeting.getTitle())
                    .startTime(meeting.getStartTime())
                    .endTime(meeting.getEndTime())
                    .status(meeting.getStatus().name())
                    .isHost(isOwner)
                    .isOwner(isOwner)
                    .canViewChatHistory(isOwner)
                    .canViewRecordings(isOwner)
                    .build();
        });
    }

    public MeetingHistoryResponse getUpNextMeeting(UUID userId) {
        List<MeetingStatus> activeStatuses = List.of(MeetingStatus.ACTIVE, MeetingStatus.SCHEDULED);

        Pageable topOne = PageRequest.of(0, 1);

        Page<Meeting> pageResult = meetingRepository.findUpNextMeeting(userId, activeStatuses, topOne);

        if (pageResult.isEmpty()) {
            return null;
        }

        Meeting meeting = pageResult.getContent().get(0);
        boolean isOwner = getMeetingOwnerId(meeting).equals(userId);

        return MeetingHistoryResponse.builder()
                .meetingCode(meeting.getMeetingCode())
                .title(meeting.getTitle())
                .startTime(meeting.getStartTime())
                .endTime(meeting.getEndTime())
                .status(meeting.getStatus().name())
                .isHost(isOwner)
                .isOwner(isOwner)
                .canViewChatHistory(isOwner)
                .canViewRecordings(isOwner)
                .build();
    }

    @Transactional
    public void leaveMeeting(String code, UUID userId) {
        Meeting meeting = meetingRepository.findByMeetingCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Participant participant = participantRepository.findByMeetingAndUser(meeting, user)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_PARTICIPANT));

        ParticipantSession activeSession = sessionRepository
                .findFirstByParticipantAndStatusOrderByJoinedAtDesc(participant, SessionStatus.ACTIVE)
                .orElse(null);

        if (activeSession != null) {
            activeSession.setLeftAt(LocalDateTime.now());
            activeSession.setStatus(SessionStatus.LEFT);
            sessionRepository.save(activeSession);
        }

        List<ParticipantSession> remainingActiveSessions = sessionRepository.findActiveSessionsByMeetingCode(code);
        if (remainingActiveSessions.isEmpty()) {
            finishMeetingBecauseRoomIsEmpty(meeting);
            return;
        }

        if (meeting.getHostId().equals(userId)) {
            participant.setRole(ParticipantRole.ATTENDEE);
            participantRepository.save(participant);
            transferHostToNextActiveParticipant(meeting, userId);
        }
    }

    @Transactional
    public void endMeetingForAll(String code, UUID hostId) {
        Meeting meeting = meetingRepository.findByMeetingCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));

        if (!meeting.getHostId().equals(hostId)) {
            throw new AppException(ErrorCode.HOST_ONLY_ACTION);
        }

        meeting.setStatus(MeetingStatus.FINISHED);
        meeting.setEndTime(LocalDateTime.now());
        meetingRepository.save(meeting);

        List<ParticipantSession> activeSessions = sessionRepository.findActiveSessionsByMeetingCode(code);
        LocalDateTime now = LocalDateTime.now();

        for (ParticipantSession session : activeSessions) {
            session.setLeftAt(now);
            session.setStatus(SessionStatus.ENDED_BY_HOST);
        }
        sessionRepository.saveAll(activeSessions);

        messagingTemplate.convertAndSend("/topic/meeting/" + code + "/system", "MEETING_ENDED");
    }

    public MeetingSummaryResponse getMeetingSummary(String code, UUID userId, String actionTaken) {
        Meeting meeting = meetingRepository.findByMeetingCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        MeetingSummaryResponse summary = new MeetingSummaryResponse();

        int totalMessages = chatMessageRepository.findByMeetingCodeOrderByTimestampAsc(code).size();
        summary.setMessages(totalMessages);

        if ("END".equals(actionTaken) || "ENDED_BY_HOST".equals(actionTaken)) {
            LocalDateTime end = meeting.getEndTime() != null ? meeting.getEndTime() : LocalDateTime.now();
            long totalSeconds = java.time.Duration.between(meeting.getCreatedAt(), end).getSeconds();
            summary.setDuration(formatDuration(totalSeconds));

            summary.setParticipants((int) participantRepository.countByMeeting(meeting));

        } else {
            Participant p = participantRepository.findByMeetingAndUser(meeting, user)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_PARTICIPANT));

            List<ParticipantSession> sessions = sessionRepository.findByParticipant(p);
            long totalSeconds = 0;
            for (ParticipantSession s : sessions) {
                LocalDateTime out = s.getLeftAt() != null ? s.getLeftAt() : LocalDateTime.now();
                totalSeconds += java.time.Duration.between(s.getJoinedAt(), out).getSeconds();
            }
            summary.setDuration(formatDuration(totalSeconds));
            summary.setParticipants(1);
        }

        return summary;
    }

    public List<com.ptithcm.ptitmeet.entity.mongodb.ChatMessage> getChatHistory(String code, UUID userId) {
        Meeting meeting = meetingRepository.findByMeetingCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));

        if (getMeetingOwnerId(meeting).equals(userId)) {
            return chatMessageRepository.findByMeetingCodeOrderByTimestampAsc(code);
        }

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Participant participant = participantRepository.findByMeetingAndUser(meeting, user)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));

        boolean hasActiveSession = sessionRepository
                .findFirstByParticipantAndStatusOrderByJoinedAtDesc(participant, SessionStatus.ACTIVE)
                .isPresent();
        if (!hasActiveSession) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return chatMessageRepository.findByMeetingCodeOrderByTimestampAsc(code);
    }

    @Transactional
    public void submitFeedback(String code, UUID userId, int rating) {
        Meeting meeting = meetingRepository.findByMeetingCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));
                
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        MeetingFeedback feedback = MeetingFeedback.builder()
                .meeting(meeting)
                .user(user)
                .rating(rating)
                .build();
        feedbackRepository.save(feedback);
    }

    private String formatDuration(long totalSeconds) {
        if (totalSeconds < 0) totalSeconds = 0;
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        
        if (h > 0) return h + "h " + m + "m " + s + "s";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }

    private static final java.util.Set<String> PUBLIC_DOMAINS = java.util.Set.of(
            "gmail.com", "yahoo.com", "outlook.com", "hotmail.com", "icloud.com",
            "aol.com", "zoho.com", "protonmail.com", "proton.me", "mail.com", "yandex.com"
    );

    private ParticipantApprovalStatus determineParticipantStatus(Meeting meeting, User user) {
        boolean isWaitingRoom = isWaitingRoomEnabled(meeting.getSettings());
        MeetingAccessType type = meeting.getAccessType();

        switch (type) {
            case OPEN:
                return isWaitingRoom ? ParticipantApprovalStatus.PENDING : ParticipantApprovalStatus.APPROVED;

            case TRUSTED:
                User host = userRepository.findById(meeting.getHostId()).orElse(null);
                String hostEmail = (host != null) ? host.getEmail() : null;
                boolean isInternal = isUserInternal(user.getEmail(), hostEmail, meeting.getAllowedDomain());
                boolean isInvited = isUserInvited(meeting, user);
                if (isInternal || isInvited) {
                    return ParticipantApprovalStatus.APPROVED;
                } else {
                    return isWaitingRoom ? ParticipantApprovalStatus.PENDING : ParticipantApprovalStatus.REJECTED;
                }

            case RESTRICTED:
                boolean isInvitedRestricted = isUserInvited(meeting, user);
                if (!isInvitedRestricted) {
                    return ParticipantApprovalStatus.REJECTED;
                }
                return isWaitingRoom ? ParticipantApprovalStatus.PENDING : ParticipantApprovalStatus.APPROVED;

            default:
                return ParticipantApprovalStatus.PENDING;
        }
    }

    private boolean isUserInternal(String guestEmail, String hostEmail, String allowedDomain) {
        String domainToCheck = null;

        if (allowedDomain != null && !allowedDomain.isBlank()) {
            domainToCheck = allowedDomain;
        } else if (hostEmail != null && hostEmail.contains("@")) {
            String hostDomain = hostEmail.substring(hostEmail.indexOf("@") + 1).trim().toLowerCase();
            if (!PUBLIC_DOMAINS.contains(hostDomain)) {
                domainToCheck = hostDomain;
            }
        }

        if (domainToCheck == null) {
            return false;
        }

        if (domainToCheck.startsWith("@")) {
            domainToCheck = domainToCheck.substring(1);
        }

        String guestEmailLower = guestEmail.trim().toLowerCase();
        domainToCheck = domainToCheck.trim().toLowerCase();

        return guestEmailLower.endsWith("@" + domainToCheck) || guestEmailLower.endsWith("." + domainToCheck);
    }

    private boolean isUserInvited(Meeting meeting, User user) {
        return meetingInvitationRepository.existsByMeetingAndUser(meeting, user) ||
                meetingInvitationRepository.existsByMeetingAndEmail(meeting, user.getEmail());
    }

    private String generateUniqueMeetingCode() {
        String code;
        int retryCount = 0;
        do {
            code = generateRandomCode();
            retryCount++;
            if (retryCount > 5) {
                throw new AppException(ErrorCode.CANNOT_GENERATE_CODE);
            }
        } while (meetingRepository.existsByMeetingCode(code));
        return code;
    }

    private String generateRandomCode() {
        String chars = "abcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.substring(0, 3) + "-" + sb.substring(3, 7) + "-" + sb.substring(7, 10);
    }

    private boolean isWaitingRoomEnabled(String settingsJson) {
        if (settingsJson == null || settingsJson.isEmpty()) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(settingsJson);
            if (root.has("waitingRoom")) {
                return root.get("waitingRoom").asBoolean();
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void createNewSession(Participant participant) {
        List<ParticipantSession> oldSessions = sessionRepository
                .findActiveSessionsByMeetingCode(participant.getMeeting().getMeetingCode())
                .stream()
                .filter(s -> s.getParticipant().getParticipantId().equals(participant.getParticipantId()))
                .toList();

        if (!oldSessions.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            for (ParticipantSession old : oldSessions) {
                old.setLeftAt(now);
                old.setStatus(SessionStatus.LEFT);
            }
            sessionRepository.saveAll(oldSessions);
        }

        String ipAddress = request.getRemoteAddr();
        String deviceInfo = request.getHeader("User-Agent");

        ParticipantSession newSession = ParticipantSession.builder()
                .participant(participant)
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .build();

        sessionRepository.save(newSession);
    }

    private SessionStatus getLatestSessionStatus(Participant participant) {
        if (participant.getParticipantId() == null) {
            return null;
        }
        return sessionRepository.findByParticipant(participant).stream()
                .max(Comparator.comparing(ParticipantSession::getJoinedAt))
                .map(ParticipantSession::getStatus)
                .orElse(null);
    }

    private UUID getMeetingOwnerId(Meeting meeting) {
        return meeting.getOwnerId() != null ? meeting.getOwnerId() : meeting.getHostId();
    }

    private void finishMeetingBecauseRoomIsEmpty(Meeting meeting) {
        if (meeting.getStatus() != MeetingStatus.FINISHED) {
            meeting.setStatus(MeetingStatus.FINISHED);
        }
        if (meeting.getEndTime() == null) {
            meeting.setEndTime(LocalDateTime.now());
        }
        meetingRepository.save(meeting);
    }

    private void transferHostToNextActiveParticipant(Meeting meeting, UUID previousHostId) {
        List<ParticipantSession> remainingSessions = sessionRepository.findActiveSessionsByMeetingCode(meeting.getMeetingCode())
                .stream()
                .filter(session -> session.getParticipant().getUser() != null)
                .filter(session -> !previousHostId.equals(session.getParticipant().getUser().getUserId()))
                .sorted(Comparator.comparing(ParticipantSession::getJoinedAt))
                .toList();

        if (remainingSessions.isEmpty()) {
            return;
        }

        ParticipantSession nextHostSession = remainingSessions.get(0);
        Participant nextHostParticipant = nextHostSession.getParticipant();

        meeting.setHostId(nextHostParticipant.getUser().getUserId());
        meetingRepository.save(meeting);

        nextHostParticipant.setRole(ParticipantRole.HOST);
        participantRepository.save(nextHostParticipant);

        messagingTemplate.convertAndSend(
                "/topic/meeting/" + meeting.getMeetingCode() + "/system",
                String.format("{\"type\":\"HOST_TRANSFERRED\",\"newHostId\":\"%s\"}", nextHostParticipant.getUser().getUserId()));
    }

    @Transactional
    public void handleSystemAction(String code, String payload) {
        try {
            JsonNode actionNode = objectMapper.readTree(payload);
            String type = actionNode.path("type").asText();
            if (type == null || type.isBlank()) {
                return;
            }

            Meeting meeting = meetingRepository.findByMeetingCode(code)
                    .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));

            switch (type) {
                case "KICK_ALL" -> kickAllParticipants(meeting);
                case "KICK_PARTICIPANT" -> {
                    String targetParticipantId = actionNode.path("targetParticipantId").asText();
                    if (targetParticipantId != null && !targetParticipantId.isBlank()) {
                        kickSingleParticipant(meeting, UUID.fromString(targetParticipantId));
                    }
                }
                default -> {
                }
            }
        } catch (Exception ignored) {
            // Ignore malformed or unsupported payloads; websocket broadcast still proceeds.
        }
    }

    private void kickAllParticipants(Meeting meeting) {
        LocalDateTime now = LocalDateTime.now();
        List<ParticipantSession> sessions = sessionRepository.findActiveSessionsByMeetingCode(meeting.getMeetingCode());
        for (ParticipantSession session : sessions) {
            Participant participant = session.getParticipant();
            if (participant.getUser() == null) {
                continue;
            }
            if (meeting.getHostId().equals(participant.getUser().getUserId())) {
                continue;
            }
            participant.setApprovalStatus(ParticipantApprovalStatus.PENDING);
            participantRepository.save(participant);

            session.setLeftAt(now);
            session.setStatus(SessionStatus.KICKED);
        }
        sessionRepository.saveAll(sessions);
    }

    private void kickSingleParticipant(Meeting meeting, UUID targetUserId) {
        Participant participant = participantRepository.findByMeeting_MeetingCodeAndUser_UserId(meeting.getMeetingCode(), targetUserId)
                .orElse(null);
        if (participant == null) {
            return;
        }

        participant.setApprovalStatus(ParticipantApprovalStatus.PENDING);
        participantRepository.save(participant);

        List<ParticipantSession> sessions = sessionRepository.findByParticipant(participant).stream()
                .filter(session -> session.getStatus() == SessionStatus.ACTIVE)
                .toList();
        LocalDateTime now = LocalDateTime.now();
        for (ParticipantSession session : sessions) {
            session.setLeftAt(now);
            session.setStatus(SessionStatus.KICKED);
        }
        sessionRepository.saveAll(sessions);
    }

    public String getMeetingSettings(String code) {
        Meeting meeting = meetingRepository.findByMeetingCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));
        return meeting.getSettings();
    }

    @Transactional
    public Meeting updateMeetingSettings(String code, UUID hostId, String settingsJson) {
        Meeting meeting = meetingRepository.findByMeetingCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));

        if (!meeting.getHostId().equals(hostId)) {
            throw new AppException(ErrorCode.HOST_ONLY_ACTION);
        }

        meeting.setSettings(settingsJson);
        meeting = meetingRepository.save(meeting);

        boolean isWaitingRoom = isWaitingRoomEnabled(settingsJson);
        if (!isWaitingRoom) {
            List<Participant> pendingList = participantRepository.findAllByMeetingAndApprovalStatus(
                    meeting,
                    ParticipantApprovalStatus.PENDING);

            for (Participant participant : pendingList) {
                participant.setApprovalStatus(ParticipantApprovalStatus.APPROVED);
                participantRepository.save(participant);

                createNewSession(participant);

                User guestUser = participant.getUser();
                String token = liveKitService.generateJoinToken(code, guestUser.getFullName(),
                        guestUser.getUserId().toString());

                JoinMeetingResponse approvalResponse = JoinMeetingResponse.builder()
                        .status("APPROVED")
                        .role("ATTENDEE")
                        .token(token)
                        .serverUrl(liveKitService.getLivekitUrl())
                        .build();

                messagingTemplate.convertAndSend(
                        "/topic/meeting/" + code + "/user/" + guestUser.getUserId(),
                        approvalResponse);
            }

            messagingTemplate.convertAndSend("/topic/meeting/" + code + "/waiting-room", "SETTINGS_CHANGED");
        }

        return meeting;
    }

    @Transactional
    public Meeting updateMeeting(UUID userId, String code, com.ptithcm.ptitmeet.dto.meeting.UpdateMeetingRequest request) {
        Meeting meeting = meetingRepository.findByMeetingCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));

        if (!getMeetingOwnerId(meeting).equals(userId)) {
            throw new AppException(ErrorCode.HOST_ONLY_ACTION);
        }

        if (meeting.getStatus() != MeetingStatus.SCHEDULED) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            meeting.setTitle(request.getTitle());
        }

        if (request.getStartTime() != null) {
            if (request.getStartTime().isBefore(LocalDateTime.now())) {
                throw new AppException(ErrorCode.INVALID_KEY);
            }
            meeting.setStartTime(request.getStartTime());
        }

        if (request.getEndTime() != null) {
            LocalDateTime effectiveStart = request.getStartTime() != null
                    ? request.getStartTime()
                    : meeting.getStartTime();
            if (request.getEndTime().isBefore(effectiveStart)) {
                throw new AppException(ErrorCode.INVALID_TIME_RANGE);
            }
            meeting.setEndTime(request.getEndTime());
        }

        if (request.getAccessType() != null) {
            meeting.setAccessType(request.getAccessType());
        }

        return meetingRepository.save(meeting);
    }
}
