package com.ptithcm.ptitmeet.controllers.websocket;

import com.ptithcm.ptitmeet.entity.mongodb.ChatMessage;
import com.ptithcm.ptitmeet.repositories.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/meeting/{code}/chat.sendMessage")
    public void sendMessage(@DestinationVariable String code, @Payload ChatMessage chatMessage) {
        chatMessage.setMeetingCode(code);
        chatMessage.setTimestamp(LocalDateTime.now());

        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        messagingTemplate.convertAndSend("/topic/meeting/" + code + "/chat", savedMessage);
    }
}