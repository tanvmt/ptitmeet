package com.ptithcm.ptitmeet.viewmodel;

import com.ptithcm.ptitmeet.api.dto.meeting.MeetingHistoryResponse;

import java.util.ArrayList;
import java.util.List;

public class MainUiState {
    private String welcomeText = "";
    private String fullName = "";
    private String avatarUrl = null;
    private MeetingHistoryResponse upNextMeeting;
    private List<MeetingHistoryResponse> recentActivity = new ArrayList<>();
    private boolean creatingMeeting;

    public MainUiState() {
    }

    public MainUiState(MainUiState other) {
        this.welcomeText = other.welcomeText;
        this.fullName = other.fullName;
        this.avatarUrl = other.avatarUrl;
        this.upNextMeeting = other.upNextMeeting;
        this.recentActivity = new ArrayList<>(other.recentActivity);
        this.creatingMeeting = other.creatingMeeting;
    }

    public String getWelcomeText() {
        return welcomeText;
    }

    public void setWelcomeText(String welcomeText) {
        this.welcomeText = welcomeText;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public MeetingHistoryResponse getUpNextMeeting() {
        return upNextMeeting;
    }

    public void setUpNextMeeting(MeetingHistoryResponse upNextMeeting) {
        this.upNextMeeting = upNextMeeting;
    }

    public List<MeetingHistoryResponse> getRecentActivity() {
        return recentActivity;
    }

    public void setRecentActivity(List<MeetingHistoryResponse> recentActivity) {
        this.recentActivity = new ArrayList<>(recentActivity);
    }

    public boolean isCreatingMeeting() {
        return creatingMeeting;
    }

    public void setCreatingMeeting(boolean creatingMeeting) {
        this.creatingMeeting = creatingMeeting;
    }
}
