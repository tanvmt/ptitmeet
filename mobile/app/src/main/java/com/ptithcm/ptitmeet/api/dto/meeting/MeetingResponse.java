package com.ptithcm.ptitmeet.api.dto.meeting;

import com.google.gson.annotations.SerializedName;
import com.ptithcm.ptitmeet.models.MeetingAccessType;

public class MeetingResponse {

    @SerializedName(value = "meeting_id", alternate = {"id", "meetingId"})
    private String id;

    @SerializedName(value = "meeting_code", alternate = {"meetingCode"})
    private String meetingCode;

    @SerializedName("title")
    private String title;

    @SerializedName("status")
    private String status;

    @SerializedName(value = "is_instant", alternate = {"isInstant"})
    private Boolean instant;

    @SerializedName(value = "start_time", alternate = {"startTime"})
    private String startTime;

    @SerializedName(value = "end_time", alternate = {"endTime"})
    private String endTime;

    @SerializedName(value = "access_type", alternate = {"accessType"})
    private MeetingAccessType accessType;

    @SerializedName("settings")
    private String settings;

    @SerializedName(value = "join_token", alternate = {"joinToken"})
    private String joinToken;

    @SerializedName(value = "created_at", alternate = {"createdAt"})
    private String createdAt;

    public String getId() {
        return id;
    }

    public String getMeetingCode() {
        return meetingCode;
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public Boolean getInstant() {
        return instant;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public MeetingAccessType getAccessType() {
        return accessType;
    }

    public String getSettings() {
        return settings;
    }

    public String getJoinToken() {
        return joinToken;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public boolean hasMeetingCode() {
        return meetingCode != null && !meetingCode.trim().isEmpty();
    }
}
