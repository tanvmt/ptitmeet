package com.ptithcm.ptitmeet.api.dto;

import com.google.gson.annotations.SerializedName;
import com.ptithcm.ptitmeet.models.MeetingAccessType;

public class MeetingInfoResponse {
    @SerializedName("title")
    private String title;

    @SerializedName("meeting_code")
    private String meetingCode;

    @SerializedName("host_name")
    private String hostName;

    @SerializedName("is_password_protected")
    private boolean isPasswordProtected;

    @SerializedName("access_type")
    private MeetingAccessType accessType;

    @SerializedName("status")
    private String status;

    public String getMeetingCode() { return meetingCode; }
    public String getTitle() { return title; }
    public String getHostName() { return hostName; }
    public boolean isPasswordProtected() { return isPasswordProtected; }
    public MeetingAccessType getAccessType() { return accessType; }
    public String getStatus() { return status; }
}
