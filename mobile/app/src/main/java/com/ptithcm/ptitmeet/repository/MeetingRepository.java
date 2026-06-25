package com.ptithcm.ptitmeet.repository;

import android.content.Context;

import com.google.gson.Gson;
import com.ptithcm.ptitmeet.api.SessionManager;
import com.ptithcm.ptitmeet.api.dto.chat.ChatMessageResponse;
import com.ptithcm.ptitmeet.api.dto.common.ApiResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.ApprovalRequest;
import com.ptithcm.ptitmeet.api.dto.meeting.JoinMeetingRequest;
import com.ptithcm.ptitmeet.api.dto.meeting.JoinMeetingResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingInfoResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.ParticipantResponse;
import com.ptithcm.ptitmeet.api.dto.recording.MeetingRecordingResponse;
import com.ptithcm.ptitmeet.api.realtime.MeetingRealtimeClient;
import com.ptithcm.ptitmeet.api.realtime.StompSocketClient;
import com.ptithcm.ptitmeet.api.services.ApiService;
import com.ptithcm.ptitmeet.api.services.RetrofitClient;

import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MeetingRepository {

    public interface DataCallback<T> {
        void onSuccess(T data);

        void onError(String message);
    }

    public interface MeetingRealtimeListener {
        void onConnected();

        void onDisconnected();

        void onError(String message);

        void onSystemMessage(String body);

        void onChatMessage(ChatMessageResponse message);

        void onJoinRequest(String participantId, String displayName);

        void onJoinRequestLeft(String participantId);

        void onWaitingRoomSignal(String body);
    }

    public interface WaitingRoomRealtimeListener {
        void onConnected();

        void onDisconnected();

        void onError(String message);

        void onUserUpdate(String body);

        void onWaitingRoomSignal(String body);
    }

    private final ApiService apiService;
    private final SessionManager sessionManager;
    private final String meetingCode;
    private final MeetingRealtimeClient meetingRealtimeClient;
    private final Gson gson = new Gson();

    public MeetingRepository(Context context, String meetingCode) {
        Context appContext = context.getApplicationContext();
        this.apiService = RetrofitClient.getApiService(appContext);
        this.sessionManager = new SessionManager(appContext);
        this.meetingCode = meetingCode;
        this.meetingRealtimeClient = new MeetingRealtimeClient(appContext, meetingCode);
    }

    public String getCurrentUserId() {
        return sessionManager.getUserId();
    }

    public String getCurrentUserName() {
        return sessionManager.getUserName();
    }

    public boolean isRealtimeConnected() {
        return meetingRealtimeClient.isConnected();
    }

    public void getMeetingInfo(DataCallback<MeetingInfoResponse> callback) {
        apiService.getMeetingInfo(meetingCode).enqueue(new Callback<ApiResponse<MeetingInfoResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<MeetingInfoResponse>> call, Response<ApiResponse<MeetingInfoResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError("Unable to load meeting details.");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MeetingInfoResponse>> call, Throwable t) {
                callback.onError("Unable to load meeting details.");
            }
        });
    }

    public void getMeetingSettings(DataCallback<String> callback) {
        apiService.getMeetingSettings(meetingCode).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError("Failed to load settings");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                callback.onError("Failed to load settings");
            }
        });
    }

    public void updateMeetingSettings(Map<String, Object> settings, DataCallback<MeetingResponse> callback) {
        apiService.updateMeetingSettings(meetingCode, settings).enqueue(new Callback<ApiResponse<MeetingResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<MeetingResponse>> call, Response<ApiResponse<MeetingResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError("Failed to update settings");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MeetingResponse>> call, Throwable t) {
                callback.onError("Network error updating settings");
            }
        });
    }

    public void getWaitingRoom(DataCallback<List<ParticipantResponse>> callback) {
        apiService.getWaitingRoom(meetingCode).enqueue(new Callback<ApiResponse<List<ParticipantResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ParticipantResponse>>> call, Response<ApiResponse<List<ParticipantResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onSuccess(Collections.emptyList());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<ParticipantResponse>>> call, Throwable t) {
                callback.onError("Unable to load waiting room");
            }
        });
    }

    public void approveParticipant(String participantId, String action, DataCallback<String> callback) {
        apiService.approveParticipant(meetingCode, new ApprovalRequest(participantId, action))
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        if (response.isSuccessful()) {
                            String message = response.body() != null ? response.body().getMessage() : "Processed successfully";
                            callback.onSuccess(message);
                        } else {
                            callback.onError("Unable to process waiting room request");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        callback.onError("Unable to process waiting room request");
                    }
                });
    }

    public void getChatHistory(DataCallback<List<ChatMessageResponse>> callback) {
        apiService.getChatHistory(meetingCode).enqueue(new Callback<ApiResponse<List<ChatMessageResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ChatMessageResponse>>> call, Response<ApiResponse<List<ChatMessageResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onSuccess(Collections.emptyList());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<ChatMessageResponse>>> call, Throwable t) {
                callback.onError("Unable to load chat history");
            }
        });
    }

    public void sendChatMessage(String content, DataCallback<Void> callback) {
        if (!meetingRealtimeClient.isChatConnected()) {
            callback.onError("Chat is not ready yet");
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("senderId", sessionManager.getUserId());
            jsonObject.put("senderName", sessionManager.getUserName());
            jsonObject.put("content", content);
            meetingRealtimeClient.sendChatMessage(jsonObject.toString());
            callback.onSuccess(null);
        } catch (Exception e) {
            callback.onError("Unable to send chat message");
        }
    }

    public void sendSystemAction(String payload, DataCallback<Void> callback) {
        if (!meetingRealtimeClient.isMeetingConnected()) {
            callback.onError("System socket is not ready yet");
            return;
        }
        meetingRealtimeClient.sendSystemAction(payload);
        callback.onSuccess(null);
    }

    public void leaveMeeting(DataCallback<Void> callback) {
        apiService.leaveMeeting(meetingCode).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError("Unable to leave the meeting at this time");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                callback.onError("Unable to leave the meeting at this time");
            }
        });
    }

    public void endMeeting(DataCallback<Void> callback) {
        apiService.endMeeting(meetingCode).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError("Unable to end the meeting");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                callback.onError("Unable to end the meeting");
            }
        });
    }

    public void startRecording(DataCallback<MeetingRecordingResponse> callback) {
        apiService.startRecording(meetingCode).enqueue(new Callback<ApiResponse<MeetingRecordingResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<MeetingRecordingResponse>> call, Response<ApiResponse<MeetingRecordingResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(response.body() != null ? response.body().getMessage() : "Unable to start recording");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MeetingRecordingResponse>> call, Throwable t) {
                callback.onError("Cannot start recording");
            }
        });
    }

    public void stopRecording(String egressId, DataCallback<MeetingRecordingResponse> callback) {
        apiService.stopRecording(egressId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError(response.body() != null ? response.body().getMessage() : "Cannot stop recording");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                callback.onError("Cannot stop recording");
            }
        });
    }

    public void joinMeeting(String displayName, DataCallback<JoinMeetingResponse> callback) {
        apiService.joinMeeting(meetingCode, new JoinMeetingRequest(null, displayName))
                .enqueue(new Callback<ApiResponse<JoinMeetingResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<JoinMeetingResponse>> call, Response<ApiResponse<JoinMeetingResponse>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            callback.onSuccess(response.body().getData());
                        } else {
                            callback.onError(response.body() != null ? response.body().getMessage() : "Unable to join meeting");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<JoinMeetingResponse>> call, Throwable t) {
                        callback.onError("Unable to connect to server");
                    }
                });
    }

    public void cancelWaiting() {
        apiService.leaveMeeting(meetingCode).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
            }
        });
    }

    public void connectMeetingRealtime(boolean hostLikeRole, MeetingRealtimeListener listener) {
        if (meetingRealtimeClient.isMeetingConnected() && meetingRealtimeClient.isChatConnected()) {
            return;
        }

        AtomicBoolean notifiedConnected = new AtomicBoolean(false);
        Runnable notifyConnectedOnce = () -> {
            if (notifiedConnected.compareAndSet(false, true)) {
                listener.onConnected();
            }
        };

        if (!meetingRealtimeClient.isMeetingConnected()) {
            meetingRealtimeClient.connectMeeting(new StompSocketClient.ConnectionListener() {
                @Override
                public void onConnected() {
                    meetingRealtimeClient.subscribeToSystem((destination, body) -> listener.onSystemMessage(body));
                    if (hostLikeRole) {
                        meetingRealtimeClient.subscribeToAdmin((destination, body) -> parseAdminMessage(body, listener));
                        meetingRealtimeClient.subscribeToWaitingRoom((destination, body) -> listener.onWaitingRoomSignal(body));
                    }
                    notifyConnectedOnce.run();
                }

                @Override
                public void onError(String message) {
                    listener.onError(message);
                }

                @Override
                public void onDisconnected() {
                    listener.onDisconnected();
                }
            });
        } else {
            notifyConnectedOnce.run();
        }

        if (!meetingRealtimeClient.isChatConnected()) {
            meetingRealtimeClient.connectChat(new StompSocketClient.ConnectionListener() {
                @Override
                public void onConnected() {
                    meetingRealtimeClient.subscribeToChat((destination, body) -> listener.onChatMessage(parseChatMessage(body)));
                    notifyConnectedOnce.run();
                }

                @Override
                public void onError(String message) {
                    listener.onError(message);
                }

                @Override
                public void onDisconnected() {
                    listener.onDisconnected();
                }
            });
        } else {
            notifyConnectedOnce.run();
        }
    }

    public void connectWaitingRoomRealtime(WaitingRoomRealtimeListener listener) {
        if (meetingRealtimeClient.isMeetingConnected()) {
            return;
        }
        meetingRealtimeClient.connectMeeting(new StompSocketClient.ConnectionListener() {
            @Override
            public void onConnected() {
                String userId = sessionManager.getUserId();
                if (userId != null) {
                    meetingRealtimeClient.subscribeToUserUpdates(userId, (destination, body) -> listener.onUserUpdate(body));
                    meetingRealtimeClient.subscribeToLegacyUserUpdates(userId, (destination, body) -> listener.onUserUpdate(body));
                }
                meetingRealtimeClient.subscribeToWaitingRoom((destination, body) -> listener.onWaitingRoomSignal(body));
                listener.onConnected();
            }

            @Override
            public void onError(String message) {
                listener.onError(message);
            }

            @Override
            public void onDisconnected() {
                listener.onDisconnected();
            }
        });
    }

    public void disconnectRealtime() {
        meetingRealtimeClient.disconnect();
    }

    private ChatMessageResponse parseChatMessage(String body) {
        try {
            ChatMessageResponse message = gson.fromJson(body, ChatMessageResponse.class);
            if (message != null) {
                return message;
            }
        } catch (Exception e) {
        }
        return new ChatMessageResponse("", "User", body);
    }

    private void parseAdminMessage(String body, MeetingRealtimeListener listener) {
        try {
            JSONObject jsonObject = new JSONObject(body);
            String status = jsonObject.optString("status", jsonObject.optString("action"));
            String participantId = jsonObject.optString("participantId");
            if ("LEFT".equalsIgnoreCase(status)) {
                listener.onJoinRequestLeft(participantId);
                return;
            }
            String displayName = jsonObject.optString("displayName", "User");
            listener.onJoinRequest(participantId, displayName);
        } catch (Exception ignored) {
        }
    }
}
