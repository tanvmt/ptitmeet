package com.ptithcm.ptitmeet.viewmodel;

public class MainUiEvent {
    public static final String SHOW_TOAST = "SHOW_TOAST";
    public static final String OPEN_WAITING_ROOM = "OPEN_WAITING_ROOM";

    private final String type;
    private final String message;
    private final String meetingCode;
    private final String displayName;
    private final boolean hostSetup;

    private MainUiEvent(String type, String message, String meetingCode, String displayName, boolean hostSetup) {
        this.type = type;
        this.message = message;
        this.meetingCode = meetingCode;
        this.displayName = displayName;
        this.hostSetup = hostSetup;
    }

    public static MainUiEvent toast(String message) {
        return new MainUiEvent(SHOW_TOAST, message, null, null, false);
    }

    public static MainUiEvent openWaitingRoom(String meetingCode, String displayName, boolean hostSetup) {
        return new MainUiEvent(OPEN_WAITING_ROOM, null, meetingCode, displayName, hostSetup);
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

    public boolean isHostSetup() {
        return hostSetup;
    }
}
