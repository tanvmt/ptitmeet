package com.ptithcm.ptitmeet.api.dto.meeting;

import android.graphics.Color;
import android.widget.TextView;

import com.google.gson.annotations.SerializedName;
import com.ptithcm.ptitmeet.utils.MeetingUiFormatter;

public class MeetingHistoryResponse {

    @SerializedName("meetingCode")
    private String meetingCode;

    @SerializedName("title")
    private String title;

    @SerializedName("startTime")
    private String startTime;

    @SerializedName("endTime")
    private String endTime;

    @SerializedName("status")
    private String status;

    @SerializedName("host")
    private boolean host;

    @SerializedName("owner")
    private boolean owner;

    @SerializedName("canViewChatHistory")
    private boolean canViewChatHistory;

    @SerializedName("canViewRecordings")
    private boolean canViewRecordings;

    public String getMeetingCode() {
        return meetingCode;
    }

    public String getTitle() {
        return title;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getStatus() {
        return status == null ? "UNKNOWN" : status;
    }

    public String getDisplayTime() {
        return MeetingUiFormatter.formatTimeRange(startTime, endTime);
    }

    public int getStatusColor(TextView textView) {
        String normalizedStatus = getStatus().toUpperCase();
        if ("ENDED".equals(normalizedStatus) || "COMPLETED".equals(normalizedStatus)) {
            return Color.parseColor("#86EFAC");
        }
        if ("SCHEDULED".equals(normalizedStatus) || "UPCOMING".equals(normalizedStatus)) {
            return Color.parseColor("#93C5FD");
        }
        if ("CANCELLED".equals(normalizedStatus)) {
            return Color.parseColor("#FDA4AF");
        }
        return textView.getCurrentTextColor();
    }
}
