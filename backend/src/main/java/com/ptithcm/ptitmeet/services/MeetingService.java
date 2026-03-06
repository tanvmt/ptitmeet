package com.ptithcm.ptitmeet.services;

import java.time.LocalDateTime;
import java.util.List;
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
import lombok.AllArgsConstructor;
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

    public Meeting scheduleMeeting(UUID hostId, CreateMeetingRequest request) {
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (request.getStartTime() == null) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        if (request.getEndTime() != null && request.getEndTime().isBefore(request.getStartTime())) {
            throw new AppException(ErrorCode.INVALID_TIME_RANGE);
        }

        String meetingCode = generateUniqueMeetingCode();

        Meeting newMeeting = Meeting.builder()
                .hostId(hostId)
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
        return meetingRepository.findByHostIdOrderByStartTimeDesc(userId);
    }

    public void cancelMeeting(UUID userId, String code) {
        Meeting meeting = meetingRepository.findByMeetingCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));

        if (!meeting.getHostId().equals(userId)) {
            throw new AppException(ErrorCode.HOST_ONLY_ACTION);
        }

        meeting.setStatus(MeetingStatus.CANCELED);
        meetingRepository.save(meeting);
    }

    @Transactional
    public JoinMeetingResponse joinMeeting(String meetingCode, JoinMeetingRequest request, UUID userId) {
        Meeting meeting = meetingRepository.findByMeetingCode(meetingCode)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));

        if (meeting.getStatus() == MeetingStatus.FINISHED) {
            throw new AppException(ErrorCode.MEETING_ALREADY_FINISHED);
        }
        if (meeting.getStatus() == MeetingStatus.CANCELED) {
            throw new AppException(ErrorCode.MEETING_CANCELED);
        }

        User user = null;
        boolean isHost = false;
        ParticipantRole role = ParticipantRole.ATTENDEE;
        ParticipantApprovalStatus targetStatus;
        Participant participant;
        String liveKitIdentity;
        String liveKitName;

        if (meeting.getPassword() != null && !meeting.getPassword().isEmpty()) {
            boolean skipPassword = (userId != null && meeting.getHostId().equals(userId));
            if (!skipPassword) {
                if (request.getPassword() == null || !request.getPassword().equals(meeting.getPassword())) {
                    throw new AppException(ErrorCode.INVALID_MEETING_PASSWORD);
                }
            }
        }

        if (userId != null) {
            // ================= TRƯỜNG HỢP 1: NGƯỜI DÙNG ĐÃ ĐĂNG NHẬP =================
            user = userRepository.findByUserId(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            isHost = meeting.getHostId().equals(user.getUserId());
            role = isHost ? ParticipantRole.HOST : ParticipantRole.ATTENDEE;

            if (isHost) {
                targetStatus = ParticipantApprovalStatus.APPROVED;
                if (meeting.getStatus() == MeetingStatus.SCHEDULED) {
                    meeting.setStatus(MeetingStatus.ACTIVE);
                    meeting.setStartTime(LocalDateTime.now());
                    meetingRepository.save(meeting);
                    messagingTemplate.convertAndSend("/topic/meeting/" + meetingCode + "/waiting-room", "HOST_JOINED");
                }
            } else {
                targetStatus = (meeting.getStatus() == MeetingStatus.SCHEDULED) 
                        ? ParticipantApprovalStatus.PENDING 
                        : determineParticipantStatus(meeting, user);
            }

            User finalUser = user;
            participant = participantRepository.findByMeetingAndUser(meeting, finalUser)
                    .orElseGet(() -> {
                        Participant newP = new Participant();
                        newP.setMeeting(meeting);
                        newP.setUser(finalUser);
                        String dName = (request.getDisplayName() != null && !request.getDisplayName().isEmpty()) ? request.getDisplayName() : finalUser.getFullName();
                        newP.setDisplayName(dName);
                        return newP;
                    });
            liveKitIdentity = user.getUserId().toString();
            liveKitName = participant.getDisplayName();

        } else {
            // ================= TRƯỜNG HỢP 2: KHÁCH VÃNG LAI (GUEST) =================
            if (request.getGuestIdentity() == null || request.getGuestIdentity().trim().isEmpty()) {
                throw new AppException(ErrorCode.GUEST_IDENTITY_REQUIRED); 
            }
            if (request.getDisplayName() == null || request.getDisplayName().trim().isEmpty()) {
                throw new AppException(ErrorCode.GUEST_NAME_REQUIRED); 
            }

            targetStatus = (meeting.getStatus() == MeetingStatus.SCHEDULED) 
                    ? ParticipantApprovalStatus.PENDING 
                    : determineGuestStatus(meeting);

            participant = participantRepository.findByMeetingCodeAndGuestIdentity(meetingCode, request.getGuestIdentity())
                    .orElseGet(() -> {
                        Participant newP = new Participant();
                        newP.setMeeting(meeting);
                        newP.setGuestIdentity(request.getGuestIdentity());
                        newP.setDisplayName(request.getDisplayName() + " (Khách)");
                        return newP;
                    });
            liveKitIdentity = request.getGuestIdentity();
            liveKitName = participant.getDisplayName();
        }

        participant.setRole(role);
        if (participant.getApprovalStatus() != ParticipantApprovalStatus.APPROVED) {
            participant.setApprovalStatus(targetStatus);
        }
        participant = participantRepository.save(participant);

        if (participant.getApprovalStatus() == ParticipantApprovalStatus.PENDING) {
            ParticipantResponse notiData = ParticipantResponse.builder()
                    .participantId(participant.getParticipantId())
                    .userId(user != null ? user.getUserId() : null)
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
                    .build();
        }

        if (participant.getApprovalStatus() == ParticipantApprovalStatus.REJECTED) {
            throw new AppException(ErrorCode.MEETING_REJECTED);
        }

        createNewSession(participant);
        String token = liveKitService.generateJoinToken(meetingCode, liveKitName, liveKitIdentity);

        return JoinMeetingResponse.builder()
                .token(token)
                .serverUrl(liveKitService.getLivekitUrl())
                .role(role.name())
                .status(participant.getApprovalStatus().name())
                .build();
    }

    public List<ParticipantResponse> getWaitingParticipants(String meetingCode, UUID hostId) {
        Meeting meeting = meetingRepository.findByMeetingCode(meetingCode)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));

        if (!meeting.getHostId().equals(hostId)) {
            throw new AppException(ErrorCode.HOST_ONLY_ACTION);
        }

        List<Participant> pendingList = participantRepository.findAllByMeetingAndApprovalStatus(
                meeting, ParticipantApprovalStatus.PENDING);

        return pendingList.stream().map(p -> ParticipantResponse.builder()
                .participantId(p.getParticipantId())
                .userId(p.getUser() != null ? p.getUser().getUserId() : null)
                .displayName(p.getDisplayName())
                .email(p.getUser() != null ? p.getUser().getEmail() : null)
                .avatarUrl(p.getUser() != null ? p.getUser().getAvatarUrl() : null)
                .status(p.getApprovalStatus().name())
                .requestTime(p.getCreatedAt() != null ? p.getCreatedAt().toString() : "")
                .build()).collect(Collectors.toList());
    }

    public void processParticipantApproval(String meetingCode, UUID hostId, ApprovalRequest request) {
        Meeting meeting = meetingRepository.findByMeetingCode(meetingCode)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));

        if (!meeting.getHostId().equals(hostId)) {
            throw new AppException(ErrorCode.HOST_ONLY_ACTION);
        }

        Participant participant = participantRepository.findById(request.getParticipantId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String topicPath = (participant.getUser() != null) 
                ? "/topic/meeting/" + meetingCode + "/user/" + participant.getUser().getUserId()
                : "/topic/meeting/" + meetingCode + "/guest/" + participant.getGuestIdentity();

        if ("APPROVED".equalsIgnoreCase(request.getAction())) {
            participant.setApprovalStatus(ParticipantApprovalStatus.APPROVED);
            participantRepository.save(participant);
            createNewSession(participant);

            String liveKitIdentity = (participant.getUser() != null) 
                    ? participant.getUser().getUserId().toString() 
                    : participant.getGuestIdentity();
            String liveKitName = participant.getDisplayName();

            String token = liveKitService.generateJoinToken(meetingCode, liveKitName, liveKitIdentity);

            JoinMeetingResponse approvalResponse = JoinMeetingResponse.builder()
                    .status("APPROVED")
                    .role("ATTENDEE")
                    .token(token)
                    .serverUrl(liveKitService.getLivekitUrl())
                    .build();

            messagingTemplate.convertAndSend(topicPath, approvalResponse);
            
        } else if ("REJECTED".equalsIgnoreCase(request.getAction())) {
            participant.setApprovalStatus(ParticipantApprovalStatus.REJECTED);
            participantRepository.save(participant);

            JoinMeetingResponse rejectResponse = JoinMeetingResponse.builder()
                    .status("REJECTED")
                    .message("Chủ phòng đã từ chối yêu cầu tham gia.")
                    .build();

            messagingTemplate.convertAndSend(topicPath, rejectResponse);
        }
    }

    public Page<MeetingHistoryResponse> getUserMeetingHistory(UUID userId, String role, String statusStr, int page,
            int size) {
        MeetingStatus statusEnum = null;
        if (statusStr != null && !statusStr.equals("ALL")) {
            statusEnum = MeetingStatus.valueOf(statusStr);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime"));

        Page<Meeting> meetingPage = meetingRepository.findMeetingHistoryWithFilters(userId, role, statusEnum, pageable);

        return meetingPage.map(meeting -> {
            boolean isHost = meeting.getHostId().equals(userId);
            return MeetingHistoryResponse.builder()
                    .meetingCode(meeting.getMeetingCode())
                    .title(meeting.getTitle())
                    .startTime(meeting.getStartTime())
                    .endTime(meeting.getEndTime())
                    .status(meeting.getStatus().name())
                    .isHost(isHost)
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
        boolean isHost = meeting.getHostId().equals(userId);

        return MeetingHistoryResponse.builder()
                .meetingCode(meeting.getMeetingCode())
                .title(meeting.getTitle())
                .startTime(meeting.getStartTime())
                .endTime(meeting.getEndTime())
                .status(meeting.getStatus().name())
                .isHost(isHost)
                .build();
    }

    @Transactional
    public void leaveMeeting(String code, UUID userId, String guestId) {
        Meeting meeting = meetingRepository.findByMeetingCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));

        Participant participant;

        if (userId != null) {
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            
            participant = participantRepository.findByMeetingAndUser(meeting, user)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_PARTICIPANT));
        } else if (guestId != null && !guestId.trim().isEmpty()) {
            participant = participantRepository.findByMeetingCodeAndGuestIdentity(code, guestId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_PARTICIPANT));
        } else {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        ParticipantSession activeSession = sessionRepository
                .findFirstByParticipantAndStatusOrderByJoinedAtDesc(participant, SessionStatus.ACTIVE)
                .orElse(null);

        if (activeSession != null) {
            activeSession.setLeftAt(LocalDateTime.now());
            activeSession.setStatus(SessionStatus.LEFT);
            sessionRepository.save(activeSession);
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

    public MeetingSummaryResponse getMeetingSummary(String code, UUID userId, String guestId, String actionTaken) {
        Meeting meeting = meetingRepository.findByMeetingCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));

        MeetingSummaryResponse summary = new MeetingSummaryResponse();
        int totalMessages = chatMessageRepository.findByMeetingCodeOrderByTimestampAsc(code).size();
        summary.setMessages(totalMessages);

        if ("END".equals(actionTaken) || "KICKED".equals(actionTaken)) {
            LocalDateTime end = meeting.getEndTime() != null ? meeting.getEndTime() : LocalDateTime.now();
            long totalSeconds = java.time.Duration.between(meeting.getCreatedAt(), end).getSeconds();
            summary.setDuration(formatDuration(totalSeconds));
            summary.setParticipants((int) participantRepository.countByMeeting(meeting));
        } else {
            Participant p;
            
            if (userId != null) {
                p = participantRepository.findByMeetingCodeAndUserId(code, userId)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_PARTICIPANT));
            } else if (guestId != null) {
                p = participantRepository.findByMeetingCodeAndGuestIdentity(code, guestId)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_PARTICIPANT));
            } else {
                throw new AppException(ErrorCode.USER_NOT_FOUND);
            }

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

    private ParticipantApprovalStatus determineParticipantStatus(Meeting meeting, User user) {
        boolean isWaitingRoom = isWaitingRoomEnabled(meeting.getSettings());
        MeetingAccessType type = meeting.getAccessType();

        switch (type) {
            case OPEN:
                return isWaitingRoom ? ParticipantApprovalStatus.PENDING : ParticipantApprovalStatus.APPROVED;

            case TRUSTED:
                boolean isInternal = isUserInternal(user.getEmail(), meeting.getAllowedDomain());
                if (isInternal) {
                    return ParticipantApprovalStatus.APPROVED;
                } else {
                    if (isWaitingRoom) {
                        return ParticipantApprovalStatus.PENDING;
                    } else {
                        return ParticipantApprovalStatus.REJECTED;
                    }
                }

            case RESTRICTED:
                boolean isInvited = isUserInvited(meeting, user);
                if (!isInvited) {
                    return ParticipantApprovalStatus.REJECTED;
                }
                return isWaitingRoom ? ParticipantApprovalStatus.PENDING : ParticipantApprovalStatus.APPROVED;

            default:
                return ParticipantApprovalStatus.PENDING;
        }
    }

    private ParticipantApprovalStatus determineGuestStatus(Meeting meeting) {
        boolean isWaitingRoom = isWaitingRoomEnabled(meeting.getSettings());
        MeetingAccessType type = meeting.getAccessType();

        if (type == MeetingAccessType.OPEN) {
            return isWaitingRoom ? ParticipantApprovalStatus.PENDING : ParticipantApprovalStatus.APPROVED;
        } else if (type == MeetingAccessType.TRUSTED) {
            return isWaitingRoom ? ParticipantApprovalStatus.PENDING : ParticipantApprovalStatus.REJECTED;
        } else {
            return ParticipantApprovalStatus.REJECTED;
        }
    }

    private boolean isUserInternal(String email, String allowedDomain) {
        if (allowedDomain == null || allowedDomain.isEmpty())
            return false;
        return email.endsWith(allowedDomain);
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
}