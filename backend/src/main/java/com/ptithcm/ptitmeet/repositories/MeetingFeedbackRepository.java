package com.ptithcm.ptitmeet.repositories;

import com.ptithcm.ptitmeet.entity.mysql.MeetingFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface MeetingFeedbackRepository extends JpaRepository<MeetingFeedback, UUID> {
}