package com.ptithcm.ptitmeet.repositories;


import com.ptithcm.ptitmeet.entity.mysql.MeetingRecording;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetingRecordingRepository extends JpaRepository<MeetingRecording, Long> {
    Optional<MeetingRecording> findByEgressId(String egressId);
    Optional<MeetingRecording> findByEgressIdAndOwnerId(String egressId, UUID ownerId);
    Optional<MeetingRecording> findByRoomNameAndStatus(String roomName, String status);
    List<MeetingRecording> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
}
