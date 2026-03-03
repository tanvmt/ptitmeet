package com.ptithcm.ptitmeet.repositories;


import com.ptithcm.ptitmeet.entity.mysql.MeetingRecording;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MeetingRecordingRepository extends JpaRepository<MeetingRecording, Long> {
    Optional<MeetingRecording> findByEgressId(String egressId);
    Optional<MeetingRecording> findByRoomNameAndStatus(String roomName, String status);
}