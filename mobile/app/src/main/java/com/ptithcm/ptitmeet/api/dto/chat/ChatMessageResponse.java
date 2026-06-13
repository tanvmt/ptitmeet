package com.ptithcm.ptitmeet.api.dto.chat;

import com.google.gson.annotations.SerializedName;

public class ChatMessageResponse {

    @SerializedName("id")
    private String id;

    @SerializedName("meetingCode")
    private String meetingCode;

    @SerializedName("senderId")
    private String senderId;

    @SerializedName("senderName")
    private String senderName;

    @SerializedName("content")
    private String content;

    @SerializedName("timestamp")
    private String timestamp;

    public ChatMessageResponse() {
    }

    public ChatMessageResponse(String senderId, String senderName, String content) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public String getMeetingCode() {
        return meetingCode;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getContent() {
        return content;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
