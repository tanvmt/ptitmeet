package com.ptithcm.ptitmeet.viewmodel;

import com.ptithcm.ptitmeet.api.dto.meeting.JoinMeetingResponse;

public class WaitingRoomUiEvent {
    public static final String SHOW_TOAST = "SHOW_TOAST";
    public static final String OPEN_MEETING_ROOM = "OPEN_MEETING_ROOM";
    public static final String FINISH = "FINISH";

    private final String type;
    private final String message;
    private final JoinMeetingResponse joinMeetingResponse;

    private WaitingRoomUiEvent(String type, String message, JoinMeetingResponse joinMeetingResponse) {
        this.type = type;
        this.message = message;
        this.joinMeetingResponse = joinMeetingResponse;
    }

    public static WaitingRoomUiEvent toast(String message) {
        return new WaitingRoomUiEvent(SHOW_TOAST, message, null);
    }

    public static WaitingRoomUiEvent openMeetingRoom(JoinMeetingResponse joinMeetingResponse) {
        return new WaitingRoomUiEvent(OPEN_MEETING_ROOM, null, joinMeetingResponse);
    }

    public static WaitingRoomUiEvent finishScreen() {
        return new WaitingRoomUiEvent(FINISH, null, null);
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public JoinMeetingResponse getJoinMeetingResponse() {
        return joinMeetingResponse;
    }
}
