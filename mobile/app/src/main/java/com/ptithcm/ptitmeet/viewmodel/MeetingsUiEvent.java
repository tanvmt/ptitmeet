package com.ptithcm.ptitmeet.viewmodel;

import com.ptithcm.ptitmeet.api.dto.chat.ChatMessageResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.JoinMeetingResponse;

import java.util.List;

public class MeetingsUiEvent {
    public static final String SHOW_TOAST = "SHOW_TOAST";
    public static final String OPEN_WAITING_ROOM = "OPEN_WAITING_ROOM";
    public static final String OPEN_MEETING_ROOM = "OPEN_MEETING_ROOM";
    public static final String SHOW_CHAT_HISTORY = "SHOW_CHAT_HISTORY";

    private final String type;
    private final String message;
    private final String meetingCode;
    private final String displayName;
    private final String waitingMessage;
    private final JoinMeetingResponse joinMeetingResponse;
    private final String meetingTitle;
    private final List<ChatMessageResponse> chatHistory;

    private MeetingsUiEvent(String type, String message, String meetingCode, String displayName, String waitingMessage, JoinMeetingResponse joinMeetingResponse, String meetingTitle, List<ChatMessageResponse> chatHistory) {
        this.type = type;
        this.message = message;
        this.meetingCode = meetingCode;
        this.displayName = displayName;
        this.waitingMessage = waitingMessage;
        this.joinMeetingResponse = joinMeetingResponse;
        this.meetingTitle = meetingTitle;
        this.chatHistory = chatHistory;
    }

    public static MeetingsUiEvent toast(String message) {
        return new MeetingsUiEvent(SHOW_TOAST, message, null, null, null, null, null, null);
    }

    public static MeetingsUiEvent openWaitingRoom(String meetingCode, String displayName, String waitingMessage) {
        return new MeetingsUiEvent(OPEN_WAITING_ROOM, null, meetingCode, displayName, waitingMessage, null, null, null);
    }

    public static MeetingsUiEvent openMeetingRoom(String meetingCode, JoinMeetingResponse joinMeetingResponse) {
        return new MeetingsUiEvent(OPEN_MEETING_ROOM, null, meetingCode, null, null, joinMeetingResponse, null, null);
    }

    public static MeetingsUiEvent showChatHistory(String meetingTitle, List<ChatMessageResponse> chatHistory) {
        return new MeetingsUiEvent(SHOW_CHAT_HISTORY, null, null, null, null, null, meetingTitle, chatHistory);
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getMeetingCode() {
        return meetingCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getWaitingMessage() {
        return waitingMessage;
    }

    public JoinMeetingResponse getJoinMeetingResponse() {
        return joinMeetingResponse;
    }

    public String getMeetingTitle() {
        return meetingTitle;
    }

    public List<ChatMessageResponse> getChatHistory() {
        return chatHistory;
    }
}
