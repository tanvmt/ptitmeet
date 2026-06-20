package com.ptithcm.ptitmeet.api.dto.meeting;

import com.google.gson.annotations.SerializedName;

public class MeetingSummaryResponse {

    @SerializedName("duration")
    private String duration;

    @SerializedName("participants")
    private int participants;

    @SerializedName("messages")
    private int messages;

    public String getDuration() {
        return duration;
    }

    public int getParticipants() {
        return participants;
    }

    public int getMessages() {
        return messages;
    }
}
