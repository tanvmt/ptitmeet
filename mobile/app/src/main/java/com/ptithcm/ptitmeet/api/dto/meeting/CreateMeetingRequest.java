package com.ptithcm.ptitmeet.api.dto.meeting;

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

    public CreateMeetingRequest(String title) {
        this.title = title;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public void setAccessType(MeetingAccessType accessType) {
        this.accessType = accessType;
    }

    public void setParticipantEmails(List<String> participantEmails) {
        this.participantEmails = participantEmails;
    }

    public void setSettings(String settings) {
        this.settings = settings;
    }
}
