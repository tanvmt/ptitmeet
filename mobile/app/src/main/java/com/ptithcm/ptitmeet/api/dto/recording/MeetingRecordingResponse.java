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

    public Long getId() {
        return id;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getEgressId() {
        return egressId;
    }

    public String getMeetingId() {
        return meetingId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getStatus() {
        return status;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public boolean hasFile() {
        return fileUrl != null && !fileUrl.trim().isEmpty();
    }
}
