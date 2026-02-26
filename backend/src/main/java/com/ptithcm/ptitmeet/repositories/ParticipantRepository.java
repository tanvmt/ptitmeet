package com.ptithcm.ptitmeet.repositories;

import com.ptithcm.ptitmeet.entity.enums.ParticipantApprovalStatus;
import com.ptithcm.ptitmeet.entity.mysql.Meeting;
import com.ptithcm.ptitmeet.entity.mysql.Participant;
import com.ptithcm.ptitmeet.entity.mysql.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    Optional<Participant> findByMeetingAndUser(Meeting meeting, User user);
    List<Participant> findAllByMeetingAndApprovalStatus(Meeting meeting, ParticipantApprovalStatus status);
    long countByMeeting(Meeting meeting);
}