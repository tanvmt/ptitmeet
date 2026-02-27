package com.ptithcm.ptitmeet.repositories;

import com.ptithcm.ptitmeet.entity.mysql.Meeting;
import com.ptithcm.ptitmeet.entity.enums.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, UUID> {
    
    Optional<Meeting> findByMeetingCode(String meetingCode);
    
    boolean existsByMeetingCode(String meetingCode);
    
    List<Meeting> findByHostIdOrderByStartTimeDesc(UUID hostId);
    
    List<Meeting> findByStatusAndEndTimeBefore(MeetingStatus status, LocalDateTime now);
}