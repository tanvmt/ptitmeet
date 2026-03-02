package com.ptithcm.ptitmeet.repositories;

import com.ptithcm.ptitmeet.entity.mysql.Meeting;
import com.ptithcm.ptitmeet.entity.enums.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    @Query("SELECT DISTINCT m FROM Meeting m LEFT JOIN Participant p ON m.meetingId = p.meeting.meetingId " +
           "WHERE (m.hostId = :userId OR p.user.userId = :userId) " +
           "AND (:status IS NULL OR m.status = :status) " +
           "AND (:role = 'ALL' OR (:role = 'HOST' AND m.hostId = :userId) OR (:role = 'GUEST' AND m.hostId <> :userId AND p.user.userId = :userId))")
    Page<Meeting> findMeetingHistoryWithFilters(
            @Param("userId") UUID userId,
            @Param("role") String role,
            @Param("status") MeetingStatus status,
            Pageable pageable);

    @Query("SELECT DISTINCT m FROM Meeting m LEFT JOIN Participant p ON m.meetingId = p.meeting.meetingId " +
           "WHERE (m.hostId = :userId OR p.user.userId = :userId) " +
           "AND m.status IN :statuses " +
           "ORDER BY m.startTime ASC")
    Page<Meeting> findUpNextMeeting(
            @Param("userId") UUID userId, 
            @Param("statuses") List<MeetingStatus> statuses, 
            Pageable pageable);
}