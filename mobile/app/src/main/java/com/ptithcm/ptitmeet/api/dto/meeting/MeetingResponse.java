package com.ptithcm.ptitmeet.api.dto.meeting;

import com.google.gson.annotations.SerializedName;
import com.ptithcm.ptitmeet.models.MeetingAccessType;

public class MeetingResponse {

    @SerializedName("id")
    private String id;

    @SerializedName("meetingCode")
    private String meetingCode;

    @SerializedName("title")
    private String title;

    @SerializedName("status")
    private String status;

    @SerializedName("startTime")
    private String startTime;

    @SerializedName("endTime")
    private String endTime;

    @SerializedName("accessType")
    private MeetingAccessType accessType;

    @SerializedName("settings")
    private String settings;

    public String getId() {
        return id;
    }

    public String getMeetingCode() {
        return meetingCode;
    }

    public String getTitle() {
        return title;
    }
}
