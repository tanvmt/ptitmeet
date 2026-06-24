package com.ptithcm.ptitmeet.api.realtime;

import android.content.Context;

import com.ptithcm.ptitmeet.api.config.ApiConfig;

import okhttp3.OkHttpClient;

public class MeetingRealtimeClient {

    private final StompSocketClient stompSocketClient;
    private final String meetingCode;

    public MeetingRealtimeClient(Context context, String meetingCode) {
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        this.stompSocketClient = new StompSocketClient(okHttpClient, ApiConfig.getWebSocketUrl());
        this.meetingCode = meetingCode;
    }

    public void connect(StompSocketClient.ConnectionListener listener) {
        stompSocketClient.connect(listener);
    }

    public void disconnect() {
        stompSocketClient.disconnect();
    }

    public boolean isConnected() {
        return stompSocketClient.isConnected();
    }

    public String subscribeToUserUpdates(String userId, StompSocketClient.MessageListener listener) {
        return stompSocketClient.subscribe("/topic/meeting/" + meetingCode + "/user/" + userId, listener);
    }

    public String subscribeToWaitingRoom(StompSocketClient.MessageListener listener) {
        return stompSocketClient.subscribe("/topic/meeting/" + meetingCode + "/waiting-room", listener);
    }

    public String subscribeToAdmin(StompSocketClient.MessageListener listener) {
        return stompSocketClient.subscribe("/topic/meeting/" + meetingCode + "/admin", listener);
    }

    public String subscribeToSystem(StompSocketClient.MessageListener listener) {
        return stompSocketClient.subscribe("/topic/meeting/" + meetingCode + "/system", listener);
    }

    public String subscribeToChat(StompSocketClient.MessageListener listener) {
        return stompSocketClient.subscribe("/topic/meeting/" + meetingCode + "/chat", listener);
    }

    public void unsubscribe(String subscriptionId) {
        stompSocketClient.unsubscribe(subscriptionId);
    }

    public void sendChatMessage(String body) {
        stompSocketClient.send("/app/meeting/" + meetingCode + "/chat.sendMessage", body);
    }

    public void sendSystemAction(String body) {
        stompSocketClient.send("/app/meeting/" + meetingCode + "/system", body);
    }
}
