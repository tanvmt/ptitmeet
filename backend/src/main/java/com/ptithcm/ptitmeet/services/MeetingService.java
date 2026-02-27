package com.ptithcm.ptitmeet.services;

import com.ptithcm.ptitmeet.dto.meeting.ApprovalRequest;
import com.ptithcm.ptitmeet.dto.meeting.CreateMeetingRequest;
import com.ptithcm.ptitmeet.dto.meeting.MeetingInfoResponse;
import com.ptithcm.ptitmeet.dto.meeting.JoinMeetingRequest;
import com.ptithcm.ptitmeet.dto.meeting.JoinMeetingResponse;
import com.ptithcm.ptitmeet.dto.meeting.ParticipantResponse;
import com.ptithcm.ptitmeet.entity.enums.MeetingAccessType;
import com.ptithcm.ptitmeet.entity.enums.MeetingStatus;
import com.ptithcm.ptitmeet.entity.enums.ParticipantApprovalStatus;
import com.ptithcm.ptitmeet.entity.enums.ParticipantRole;
import com.ptithcm.ptitmeet.entity.mysql.Meeting;
import com.ptithcm.ptitmeet.entity.mysql.MeetingInvitation;
import com.ptithcm.ptitmeet.entity.mysql.Participant;
import com.ptithcm.ptitmeet.entity.mysql.User;
import com.ptithcm.ptitmeet.exception.AppException;
import com.ptithcm.ptitmeet.exception.ErrorCode;
import com.ptithcm.ptitmeet.repositories.MeetingInvitationRepository;
import com.ptithcm.ptitmeet.repositories.MeetingRepository;
import com.ptithcm.ptitmeet.repositories.UserRepository;
import com.ptithcm.ptitmeet.repositories.ParticipantRepository;
import com.ptithcm.ptitmeet.services.LiveKitService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingInvitationRepository meetingInvitationRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final LiveKitService liveKitService;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;

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
                        host.getFullName()
                );
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

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (meeting.getStatus() == MeetingStatus.FINISHED) {
            throw new AppException(ErrorCode.MEETING_ALREADY_FINISHED);
        }
        if (meeting.getStatus() == MeetingStatus.CANCELED) {
            throw new AppException(ErrorCode.MEETING_CANCELED);
        }

        boolean isHost = meeting.getHostId().equals(user.getUserId());
        ParticipantRole role = isHost ? ParticipantRole.HOST : ParticipantRole.ATTENDEE;

        if (!isHost && meeting.getPassword() != null && !meeting.getPassword().isEmpty()) {
            if (request.getPassword() == null || !request.getPassword().equals(meeting.getPassword())) {
                throw new AppException(ErrorCode.INVALID_MEETING_PASSWORD);
            }
        }

        ParticipantApprovalStatus targetStatus;
        
        if (isHost) {
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
        
        if (participant.getApprovalStatus() != ParticipantApprovalStatus.APPROVED) {
            participant.setApprovalStatus(targetStatus);
        }

        participant = participantRepository.save(participant);

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
                    .build();
        }

        if (participant.getApprovalStatus() == ParticipantApprovalStatus.REJECTED) {
            throw new AppException(ErrorCode.MEETING_REJECTED);
        }

        String token = liveKitService.generateToken(user, meeting);
        
        return JoinMeetingResponse.builder()
                .token(token)
                .serverUrl(liveKitService.getLiveKitUrl())
                .role(role.name())
                .status(participant.getApprovalStatus().name())
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
                ParticipantApprovalStatus.PENDING
        );

        return pendingList.stream().map(p -> ParticipantResponse.builder()
                .participantId(p.getParticipantId())
                .userId(p.getUser().getUserId())
                .displayName(p.getUser().getFullName())
                .email(p.getUser().getEmail())
                .avatarUrl(p.getUser().getAvatarUrl())
                .status(p.getApprovalStatus().name())
                .requestTime(p.getCreatedAt() != null ? p.getCreatedAt().toString() : "")
                .build()
        ).collect(Collectors.toList());
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

            User guestUser = participant.getUser();
            String token = liveKitService.generateToken(guestUser, meeting);

            JoinMeetingResponse approvalResponse = JoinMeetingResponse.builder()
                    .status("APPROVED")
                    .role("ATTENDEE")
                    .token(token)
                    .serverUrl(liveKitService.getLiveKitUrl())
                    .build();

            messagingTemplate.convertAndSend(
                    "/topic/meeting/" + meetingCode + "/user/" + guestUser.getUserId(), 
                    approvalResponse
            );
        } else if ("REJECTED".equalsIgnoreCase(request.getAction())) {
            participant.setApprovalStatus(ParticipantApprovalStatus.REJECTED);
            participantRepository.save(participant);

            JoinMeetingResponse rejectResponse = JoinMeetingResponse.builder()
                    .status("REJECTED")
                    .message("Chủ phòng đã từ chối yêu cầu tham gia.")
                    .build();

            messagingTemplate.convertAndSend(
                    "/topic/meeting/" + meetingCode + "/user/" + participant.getUser().getUserId(),
                    rejectResponse
            );
        } else {
            throw new AppException(ErrorCode.INVALID_KEY); 
        }

        participantRepository.save(participant);
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

    private boolean isUserInternal(String email, String allowedDomain) {
        if (allowedDomain == null || allowedDomain.isEmpty()) return false;
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
}