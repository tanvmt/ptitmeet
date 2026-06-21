package com.ptithcm.ptitmeet.viewmodel;

import com.ptithcm.ptitmeet.api.dto.chat.ChatMessageResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.ParticipantResponse;

import java.util.ArrayList;
import java.util.List;

public class MeetingUiState {
    private String meetingTitle = "---";
    private String currentMeetingSettings = "{}";
    private List<ParticipantResponse> waitingParticipants = new ArrayList<>();
    private List<ChatMessageResponse> chatMessages = new ArrayList<>();
    private boolean waitingBadgeVisible;
    private boolean recordingActive;
    private boolean recordingRequestInFlight;
    private String recordingStatus = "IDLE";
    private String recordingEgressId;

    public MeetingUiState() {
    }

    public MeetingUiState(MeetingUiState other) {
        this.meetingTitle = other.meetingTitle;
        this.currentMeetingSettings = other.currentMeetingSettings;
        this.waitingParticipants = new ArrayList<>(other.waitingParticipants);
        this.chatMessages = new ArrayList<>(other.chatMessages);
        this.waitingBadgeVisible = other.waitingBadgeVisible;
        this.recordingActive = other.recordingActive;
        this.recordingRequestInFlight = other.recordingRequestInFlight;
        this.recordingStatus = other.recordingStatus;
        this.recordingEgressId = other.recordingEgressId;
    }

    public String getMeetingTitle() {
        return meetingTitle;
    }

    public void setMeetingTitle(String meetingTitle) {
        this.meetingTitle = meetingTitle;
    }

    public String getCurrentMeetingSettings() {
        return currentMeetingSettings;
    }

    public void setCurrentMeetingSettings(String currentMeetingSettings) {
        this.currentMeetingSettings = currentMeetingSettings;
    }

    public List<ParticipantResponse> getWaitingParticipants() {
        return waitingParticipants;
    }

    public void setWaitingParticipants(List<ParticipantResponse> waitingParticipants) {
        this.waitingParticipants = new ArrayList<>(waitingParticipants);
    }

    public List<ChatMessageResponse> getChatMessages() {
        return chatMessages;
    }

    public void setChatMessages(List<ChatMessageResponse> chatMessages) {
        this.chatMessages = new ArrayList<>(chatMessages);
    }

    public boolean isWaitingBadgeVisible() {
        return waitingBadgeVisible;
    }

    public void setWaitingBadgeVisible(boolean waitingBadgeVisible) {
        this.waitingBadgeVisible = waitingBadgeVisible;
    }

    public boolean isRecordingActive() {
        return recordingActive;
    }

    public void setRecordingActive(boolean recordingActive) {
        this.recordingActive = recordingActive;
    }

    public boolean isRecordingRequestInFlight() {
        return recordingRequestInFlight;
    }

    public void setRecordingRequestInFlight(boolean recordingRequestInFlight) {
        this.recordingRequestInFlight = recordingRequestInFlight;
    }

    public String getRecordingStatus() {
        return recordingStatus;
    }

    public void setRecordingStatus(String recordingStatus) {
        this.recordingStatus = recordingStatus;
    }

    public String getRecordingEgressId() {
        return recordingEgressId;
    }

    public void setRecordingEgressId(String recordingEgressId) {
        this.recordingEgressId = recordingEgressId;
    }
}
