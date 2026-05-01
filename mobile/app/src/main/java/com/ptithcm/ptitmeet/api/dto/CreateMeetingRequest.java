package com.ptithcm.ptitmeet.api.dto;

import com.google.gson.annotations.SerializedName;
import com.ptithcm.ptitmeet.models.MeetingAccessType;
import java.util.List;

public class CreateMeetingRequest {
    @SerializedName("title")
    private String title;

    @SerializedName("password")
    private String password;

    @SerializedName("start_time")
    private String startTime;

    @SerializedName("end_time")
    private String endTime;

    @SerializedName("access_type")
    private MeetingAccessType accessType;

    @SerializedName("participant_emails")
    private List<String> participantEmails;

    @SerializedName("settings")
    private String settings;

    // Constructors, Getters và Setters
    public CreateMeetingRequest(String title) {
        this.title = title;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getStartTime() {
        return startTime;
    }
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }
    public String getEndTime() {
        return endTime;
    }
    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
    public List<String> getParticipantEmails() {
        return participantEmails;
    }
    public void setParticipantEmails(List<String> participantEmails) {
        this.participantEmails = participantEmails;
    }
}