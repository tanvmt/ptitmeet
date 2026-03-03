package com.ptithcm.ptitmeet.repositories.mysql;

import com.ptithcm.ptitmeet.entity.mysql.ParticipantSession;
import com.ptithcm.ptitmeet.entity.mysql.Participant;
import com.ptithcm.ptitmeet.entity.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParticipantSessionRepository extends JpaRepository<ParticipantSession, UUID> {

    Optional<ParticipantSession> findFirstByParticipantAndStatusOrderByJoinedAtDesc(Participant participant, SessionStatus status);

    @Query("SELECT ps FROM ParticipantSession ps WHERE ps.participant.meeting.meetingCode = :code AND ps.status = 'ACTIVE'")
    List<ParticipantSession> findActiveSessionsByMeetingCode(@Param("code") String code);
}