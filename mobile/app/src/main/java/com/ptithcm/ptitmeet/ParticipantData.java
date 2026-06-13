package com.ptithcm.ptitmeet;

public class ParticipantData {
    private String identity;
    private String id;
    private String name;
    private boolean hasVideo;
    private boolean isMicOn;
    private boolean isSpeaking;
    private boolean isLocal;

    public ParticipantData(String id, String name, boolean hasVideo, boolean isMicOn, boolean isSpeaking) {
        this.id = id;
        this.name = name;
        this.hasVideo = hasVideo;
        this.isMicOn = isMicOn;
        this.isSpeaking = isSpeaking;
        this.identity = id;
    }

    public ParticipantData(String identity, String id, String name, boolean hasVideo, boolean isMicOn, boolean isSpeaking, boolean isLocal) {
        this.identity = identity;
        this.id = id;
        this.name = name;
        this.hasVideo = hasVideo;
        this.isMicOn = isMicOn;
        this.isSpeaking = isSpeaking;
        this.isLocal = isLocal;
    }

    public String getIdentity() { return identity; }
    public String getId() { return id; }
    public String getName() { return name; }
    public boolean hasVideo() { return hasVideo; }
    public boolean isMicOn() { return isMicOn; }
    public boolean isSpeaking() { return isSpeaking; }
    public boolean isLocal() { return isLocal; }

    // Setters (nếu cần update dữ liệu real-time)
    public void setIdentity(String identity) { this.identity = identity; }
    public void setHasVideo(boolean hasVideo) { this.hasVideo = hasVideo; }
    public void setMicOn(boolean micOn) { isMicOn = micOn; }
    public void setSpeaking(boolean speaking) { isSpeaking = speaking; }
    public void setLocal(boolean local) { isLocal = local; }
}
