package com.ptithcm.ptitmeet.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.ptithcm.ptitmeet.entity.mongodb.ChatMessage;
import com.ptithcm.ptitmeet.repositories.ChatMessageRepository;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatController chatController;

    @Test
    void sendMessageShouldPersistAndBroadcastMessage() {
        ChatMessage message = ChatMessage.builder()
                .senderId(UUID.randomUUID())
                .senderName("Demo User")
                .content("Hello")
                .build();

        when(chatMessageRepository.save(message)).thenAnswer(invocation -> invocation.getArgument(0));

        chatController.sendMessage("room-123", message);

        assertEquals("room-123", message.getMeetingCode());
        assertNotNull(message.getTimestamp());

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messagingTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("/topic/meeting/room-123/chat"), captor.capture());
        assertEquals("Hello", captor.getValue().getContent());
    }
}
