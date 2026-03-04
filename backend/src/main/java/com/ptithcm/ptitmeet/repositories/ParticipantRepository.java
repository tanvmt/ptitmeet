package com.ptithcm.ptitmeet.repositories;

import com.ptithcm.ptitmeet.entity.enums.ParticipantApprovalStatus;
import com.ptithcm.ptitmeet.entity.mysql.Meeting;
import com.ptithcm.ptitmeet.entity.mysql.Participant;
import com.ptithcm.ptitmeet.entity.mysql.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, UUID> {
    Optional<Participant> findByMeetingAndUser(Meeting meeting, User user);

    @Query("SELECT p FROM Participant p WHERE p.meeting.meetingCode = :code AND p.guestIdentity = :guestIdentity")
    Optional<Participant> findByMeetingCodeAndGuestIdentity(@Param("code") String code, @Param("guestIdentity") String guestIdentity);

    @Query("SELECT p FROM Participant p WHERE p.meeting.meetingCode = :code")
    List<Participant> findByMeetingCode(@Param("code") String code);

    List<Participant> findAllByMeetingAndApprovalStatus(Meeting meeting, ParticipantApprovalStatus status);
    long countByMeeting(Meeting meeting);
}