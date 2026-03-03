package com.ptithcm.ptitmeet;

public class ParticipantData {
    private String id;
    private String name;
    private boolean hasVideo;
    private boolean isMicOn;
    private boolean isSpeaking;

    public ParticipantData(String id, String name, boolean hasVideo, boolean isMicOn, boolean isSpeaking) {
        this.id = id;
        this.name = name;
        this.hasVideo = hasVideo;
        this.isMicOn = isMicOn;
        this.isSpeaking = isSpeaking;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public boolean hasVideo() { return hasVideo; }
    public boolean isMicOn() { return isMicOn; }
    public boolean isSpeaking() { return isSpeaking; }

    // Setters (nếu cần update dữ liệu real-time)
    public void setHasVideo(boolean hasVideo) { this.hasVideo = hasVideo; }
    public void setMicOn(boolean micOn) { isMicOn = micOn; }
    public void setSpeaking(boolean speaking) { isSpeaking = speaking; }
}