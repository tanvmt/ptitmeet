package com.ptithcm.ptitmeet.viewmodel;

public class MeetingUiEvent {
    public static final String SHOW_TOAST = "SHOW_TOAST";
    public static final String NAVIGATE_SUMMARY = "NAVIGATE_SUMMARY";
    public static final String APPLY_REMOTE_MIC_MUTE = "APPLY_REMOTE_MIC_MUTE";
    public static final String APPLY_REMOTE_CAMERA_OFF = "APPLY_REMOTE_CAMERA_OFF";
    public static final String SHOW_JOIN_REQUEST = "SHOW_JOIN_REQUEST";
    public static final String HIDE_JOIN_REQUEST = "HIDE_JOIN_REQUEST";
    public static final String PLAY_CHAT_SOUND = "PLAY_CHAT_SOUND";
    public static final String CONNECT_LIVEKIT = "CONNECT_LIVEKIT";

    private final String type;
    private final String message;
    private final String action;
    private final String participantId;
    private final String displayName;

    private MeetingUiEvent(String type, String message, String action, String participantId, String displayName) {
        this.type = type;
        this.message = message;
        this.action = action;
        this.participantId = participantId;
        this.displayName = displayName;
    }

    public static MeetingUiEvent toast(String message) {
        return new MeetingUiEvent(SHOW_TOAST, message, null, null, null);
    }

    public static MeetingUiEvent navigateToSummary(String action) {
        return new MeetingUiEvent(NAVIGATE_SUMMARY, null, action, null, null);
    }

    public static MeetingUiEvent applyRemoteMicMute(String message) {
        return new MeetingUiEvent(APPLY_REMOTE_MIC_MUTE, message, null, null, null);
    }

    public static MeetingUiEvent applyRemoteCameraOff(String message) {
        return new MeetingUiEvent(APPLY_REMOTE_CAMERA_OFF, message, null, null, null);
    }

    public static MeetingUiEvent showJoinRequest(String participantId, String displayName) {
        return new MeetingUiEvent(SHOW_JOIN_REQUEST, null, null, participantId, displayName);
    }

    public static MeetingUiEvent hideJoinRequest(String participantId) {
        return new MeetingUiEvent(HIDE_JOIN_REQUEST, null, null, participantId, null);
    }

    public static MeetingUiEvent playChatSound() {
        return new MeetingUiEvent(PLAY_CHAT_SOUND, null, null, null, null);
    }

    public static MeetingUiEvent connectLiveKit() {
        return new MeetingUiEvent(CONNECT_LIVEKIT, null, null, null, null);
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getAction() {
        return action;
    }

    public String getParticipantId() {
        return participantId;
    }

    public String getDisplayName() {
        return displayName;
    }
}
