package com.ptithcm.ptitmeet.api.realtime;

import android.content.Context;

import com.ptithcm.ptitmeet.api.SessionManager;
import com.ptithcm.ptitmeet.api.config.ApiConfig;

import okhttp3.OkHttpClient;

public class MeetingRealtimeClient {

    private final StompSocketClient meetingSocketClient;
    private final StompSocketClient chatSocketClient;
    private final String meetingCode;

    public MeetingRealtimeClient(Context context, String meetingCode) {
        SessionManager sessionManager = new SessionManager(context.getApplicationContext());
        String userId = sessionManager.getUserId();
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        this.meetingSocketClient = new StompSocketClient(okHttpClient, ApiConfig.getWebSocketUrl("meeting", userId));
        this.chatSocketClient = new StompSocketClient(okHttpClient, ApiConfig.getWebSocketUrl("chat", userId));
        this.meetingCode = meetingCode;
    }

    public void connect(StompSocketClient.ConnectionListener listener) {
        connectMeeting(listener);
    }

    public void connectMeeting(StompSocketClient.ConnectionListener listener) {
        meetingSocketClient.connect(listener);
    }

    public void connectChat(StompSocketClient.ConnectionListener listener) {
        chatSocketClient.connect(listener);
    }

    public void disconnect() {
        meetingSocketClient.disconnect();
        chatSocketClient.disconnect();
    }

    public boolean isConnected() {
        return meetingSocketClient.isConnected() || chatSocketClient.isConnected();
    }

    public boolean isMeetingConnected() {
        return meetingSocketClient.isConnected();
    }

    public boolean isChatConnected() {
        return chatSocketClient.isConnected();
    }

    public String subscribeToUserUpdates(String userId, StompSocketClient.MessageListener listener) {
        return meetingSocketClient.subscribe("/user/queue/approval", listener);
    }

    public String subscribeToLegacyUserUpdates(String userId, StompSocketClient.MessageListener listener) {
        return meetingSocketClient.subscribe("/topic/meeting/" + meetingCode + "/user/" + userId, listener);
    }

    public String subscribeToWaitingRoom(StompSocketClient.MessageListener listener) {
        return meetingSocketClient.subscribe("/topic/meeting/" + meetingCode + "/waiting-room", listener);
    }

    public String subscribeToAdmin(StompSocketClient.MessageListener listener) {
        return meetingSocketClient.subscribe("/topic/meeting/" + meetingCode + "/host", listener);
    }

    public String subscribeToSystem(StompSocketClient.MessageListener listener) {
        return meetingSocketClient.subscribe("/topic/meeting/" + meetingCode, listener);
    }

    public String subscribeToChat(StompSocketClient.MessageListener listener) {
        return chatSocketClient.subscribe("/topic/chat/" + meetingCode, listener);
    }

    public void unsubscribe(String subscriptionId) {
        meetingSocketClient.unsubscribe(subscriptionId);
        chatSocketClient.unsubscribe(subscriptionId);
    }

    public void sendChatMessage(String body) {
        chatSocketClient.send("/app/chat/" + meetingCode, body);
    }

    public void sendSystemAction(String body) {
        meetingSocketClient.send("/app/meeting/" + meetingCode + "/system", body);
    }
}
