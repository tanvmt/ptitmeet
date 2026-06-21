package com.ptithcm.ptitmeet.viewmodel;

public class WaitingRoomUiState {
    private String readyTitle = "Ready to join?";
    private String meetingDetails = "Unable to load meeting details.";
    private String waitingMessage = "";
    private boolean waitingState;
    private boolean joinInFlight;
    private boolean realtimeConnected;
    private String joinButtonText = "Join now";
    private boolean joinButtonEnabled = true;

    public WaitingRoomUiState() {
    }

    public WaitingRoomUiState(WaitingRoomUiState other) {
        this.readyTitle = other.readyTitle;
        this.meetingDetails = other.meetingDetails;
        this.waitingMessage = other.waitingMessage;
        this.waitingState = other.waitingState;
        this.joinInFlight = other.joinInFlight;
        this.realtimeConnected = other.realtimeConnected;
        this.joinButtonText = other.joinButtonText;
        this.joinButtonEnabled = other.joinButtonEnabled;
    }

    public String getReadyTitle() {
        return readyTitle;
    }

    public void setReadyTitle(String readyTitle) {
        this.readyTitle = readyTitle;
    }

    public String getMeetingDetails() {
        return meetingDetails;
    }

    public void setMeetingDetails(String meetingDetails) {
        this.meetingDetails = meetingDetails;
    }

    public String getWaitingMessage() {
        return waitingMessage;
    }

    public void setWaitingMessage(String waitingMessage) {
        this.waitingMessage = waitingMessage;
    }

    public boolean isWaitingState() {
        return waitingState;
    }

    public void setWaitingState(boolean waitingState) {
        this.waitingState = waitingState;
    }

    public boolean isJoinInFlight() {
        return joinInFlight;
    }

    public void setJoinInFlight(boolean joinInFlight) {
        this.joinInFlight = joinInFlight;
    }

    public boolean isRealtimeConnected() {
        return realtimeConnected;
    }

    public void setRealtimeConnected(boolean realtimeConnected) {
        this.realtimeConnected = realtimeConnected;
    }

    public String getJoinButtonText() {
        return joinButtonText;
    }

    public void setJoinButtonText(String joinButtonText) {
        this.joinButtonText = joinButtonText;
    }

    public boolean isJoinButtonEnabled() {
        return joinButtonEnabled;
    }

    public void setJoinButtonEnabled(boolean joinButtonEnabled) {
        this.joinButtonEnabled = joinButtonEnabled;
    }
}
