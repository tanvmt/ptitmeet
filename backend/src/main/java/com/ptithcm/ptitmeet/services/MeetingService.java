package com.ptithcm.ptitmeet.services;

import com.ptithcm.ptitmeet.dto.meeting.CreateMeetingRequest;
import com.ptithcm.ptitmeet.dto.meeting.MeetingInfoResponse;
import com.ptithcm.ptitmeet.dto.meeting.JoinMeetingRequest;
import com.ptithcm.ptitmeet.dto.meeting.JoinMeetingResponse;
import com.ptithcm.ptitmeet.entity.enums.MeetingAccessType;
import com.ptithcm.ptitmeet.entity.enums.MeetingStatus;
import com.ptithcm.ptitmeet.entity.enums.ParticipantApprovalStatus;
import com.ptithcm.ptitmeet.entity.enums.ParticipantRole;
import com.ptithcm.ptitmeet.entity.mysql.Meeting;
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
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
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

        return meetingRepository.save(meeting);
    }

    public Meeting scheduleMeeting(UUID hostId, CreateMeetingRequest request) {
        if (!userRepository.existsById(hostId)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        if (request.getStartTime() == null) {
            throw new AppException(ErrorCode.INVALID_KEY); 
        }
        
        if (request.getEndTime() != null && request.getEndTime().isBefore(request.getStartTime())) {
             throw new AppException(ErrorCode.INVALID_TIME_RANGE);
        }

        String meetingCode = generateUniqueMeetingCode();

        Meeting meeting = Meeting.builder()
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

        return meetingRepository.save(meeting);
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
    public JoinMeetingResponse joinMeeting(String meetingCode, JoinMeetingRequest request, String email) {
        Meeting meeting = meetingRepository.findByMeetingCode(meetingCode)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));

        if (meeting.getStatus() == MeetingStatus.FINISHED) {
            throw new AppException(ErrorCode.MEETING_ALREADY_FINISHED);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        boolean isHost = meeting.getHostId().equals(user.getUserId());
        ParticipantRole role = isHost ? ParticipantRole.HOST : ParticipantRole.ATTENDEE;

        ParticipantApprovalStatus status;

        if (isHost) {
            status = ParticipantApprovalStatus.APPROVED;
        } else {
            status = determineParticipantStatus(meeting, user);
        }

        if (!isHost && meeting.getPassword() != null && !meeting.getPassword().isEmpty()) {
            if (request.getPassword() == null || !request.getPassword().equals(meeting.getPassword())) {
                throw new AppException(ErrorCode.INVALID_MEETING_PASSWORD);
            }
        }

        Participant participant = participantRepository.findByMeetingAndUser(meeting, user)
                .orElseGet(() -> {
                    Participant newP = new Participant();
                    newP.setMeeting(meeting);
                    newP.setUser(user);
                    newP.setRole(role);
                    newP.setApprovalStatus(status);
                    return newP;
                });

        if (!participant.getRole().equals(role)) {
            participant.setRole(role);
        }

        if (participant.getApprovalStatus() != status && !isHost) {
            participant.setApprovalStatus(status);
        }

        participantRepository.save(participant);

        if (participant.getApprovalStatus() == ParticipantApprovalStatus.PENDING) {
            return JoinMeetingResponse.builder()
                    .status("PENDING")
                    .message("Bạn đang ở trong phòng chờ. Vui lòng chờ chủ phòng duyệt.")
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