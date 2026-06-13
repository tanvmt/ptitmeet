package com.ptithcm.ptitmeet.api.dto.recording;

import com.google.gson.annotations.SerializedName;

public class MeetingRecordingResponse {

    @SerializedName("id")
    private Long id;

    @SerializedName("roomName")
    private String roomName;

    @SerializedName("egressId")
    private String egressId;

    @SerializedName("meetingId")
    private String meetingId;

    @SerializedName("ownerId")
    private String ownerId;

    @SerializedName("status")
    private String status;

    @SerializedName("fileUrl")
    private String fileUrl;

    @SerializedName("createdAt")
    private String createdAt;
}
