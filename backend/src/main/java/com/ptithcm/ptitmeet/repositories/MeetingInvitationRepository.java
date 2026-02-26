package com.ptithcm.ptitmeet.repositories;

import com.ptithcm.ptitmeet.entity.mysql.Meeting;
import com.ptithcm.ptitmeet.entity.mysql.MeetingInvitation;
import com.ptithcm.ptitmeet.entity.mysql.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetingInvitationRepository extends JpaRepository<MeetingInvitation, UUID> {
    boolean existsByMeetingAndUser(Meeting meeting, User user);
    
    boolean existsByMeetingAndEmail(Meeting meeting, String email);
}