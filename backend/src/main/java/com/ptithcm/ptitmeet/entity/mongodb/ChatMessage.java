package com.ptithcm.ptitmeet.entity.mongodb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "meeting_chats")
public class ChatMessage {
    
    @Id
    private String id; 

    private String meetingCode;

    private UUID senderId;

    private String senderName; 

    private String content;

    private LocalDateTime timestamp;
}