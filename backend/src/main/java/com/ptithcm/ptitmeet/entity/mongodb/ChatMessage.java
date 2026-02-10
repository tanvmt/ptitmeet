package com.ptithcm.ptitmeet.entity.mongodb;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.ptithcm.ptitmeet.entity.enums.MessageType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Document(collection = "chat_messages") 
public class ChatMessage {
    
    @Id
    private String id; 

    @Field("meeting_id")
    private String meetingId;

    @Field("sender_id")
    private String senderId;

    @Field("sender_name")
    private String senderName; 

    private String content;

    @Field("type")
    private MessageType type;

    @Field("created_at")
    private LocalDateTime createdAt;
}