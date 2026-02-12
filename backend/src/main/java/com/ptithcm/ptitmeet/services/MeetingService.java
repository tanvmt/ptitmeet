package com.ptithcm.ptitmeet.services;

import com.ptithcm.ptitmeet.dto.meeting.CreateMeetingRequest;
import com.ptithcm.ptitmeet.dto.meeting.MeetingInfoResponse;
import com.ptithcm.ptitmeet.entity.enums.MeetingAccessType;
import com.ptithcm.ptitmeet.entity.enums.MeetingStatus;
import com.ptithcm.ptitmeet.entity.mysql.Meeting;
import com.ptithcm.ptitmeet.entity.mysql.User;
import com.ptithcm.ptitmeet.exception.AppException;
import com.ptithcm.ptitmeet.exception.ErrorCode;
import com.ptithcm.ptitmeet.repositories.MeetingRepository;
import com.ptithcm.ptitmeet.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;

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
}