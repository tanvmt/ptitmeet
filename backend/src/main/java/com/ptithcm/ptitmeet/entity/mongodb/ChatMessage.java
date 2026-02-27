package com.ptit.meet.entity.mongodb;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import lombok.Builder;
import lombok.Data;
import com.ptithcm.ptitmeet.entity.enums.MessageType;

import java.time.LocalDateTime;

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