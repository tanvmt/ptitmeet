package com.ptithcm.ptitmeet.repositories;

import com.ptithcm.ptitmeet.entity.mongodb.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findByMeetingCodeOrderByTimestampAsc(String meetingCode);
}