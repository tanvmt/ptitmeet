package com.ptithcm.ptitmeet.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.ptithcm.ptitmeet.api.dto.chat.ChatMessageResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingInfoResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.ParticipantResponse;
import com.ptithcm.ptitmeet.api.dto.recording.MeetingRecordingResponse;
import com.ptithcm.ptitmeet.repository.MeetingRepository;
import com.ptithcm.ptitmeet.utils.SystemActionHelper;

import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MeetingViewModel extends AndroidViewModel {

    public static class Factory implements ViewModelProvider.Factory {
        private final Application application;
        private final String meetingCode;
        private final String userRole;
        private final boolean meetingOwner;

        public Factory(Application application, String meetingCode, String userRole, boolean meetingOwner) {
            this.application = application;
            this.meetingCode = meetingCode;
            this.userRole = userRole;
            this.meetingOwner = meetingOwner;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new MeetingViewModel(application, meetingCode, userRole, meetingOwner);
        }
    }

    private final MeetingRepository repository;
    private final String userRole;
    private final boolean isMeetingOwner;
    private boolean hostPrivilegesGranted;
    private final MutableLiveData<MeetingUiState> uiState = new MutableLiveData<>(new MeetingUiState());
    private final MutableLiveData<Event<MeetingUiEvent>> uiEvent = new MutableLiveData<>();
    private final Handler waitingRoomHandler = new Handler(Looper.getMainLooper());
    private final Handler recordingStatusHandler = new Handler(Looper.getMainLooper());
    private boolean waitingRoomPollingActive;

    private final Runnable waitingRoomPollingRunnable = new Runnable() {
        @Override
        public void run() {
            if (!waitingRoomPollingActive) {
                return;
            }
            loadWaitingParticipants();
            waitingRoomHandler.postDelayed(this, 5000);
        }
    };

    private final Runnable recordingStatusRunnable = new Runnable() {
        @Override
        public void run() {
            pollRecordingStatus();
        }
    };

    public MeetingViewModel(@NonNull Application application, String meetingCode, String userRole, boolean meetingOwner) {
        super(application);
        this.repository = new MeetingRepository(application, meetingCode);
        this.userRole = userRole;
        this.isMeetingOwner = meetingOwner;
    }

    public LiveData<MeetingUiState> getUiState() {
        return uiState;
    }

    public LiveData<Event<MeetingUiEvent>> getUiEvent() {
        return uiEvent;
    }

    public boolean isHostLikeRole() {
        if (hostPrivilegesGranted) {
            return true;
        }
        if (userRole == null) {
            return false;
        }
        String normalized = userRole.trim().toUpperCase();
        return "HOST".equals(normalized) || "OWNER".equals(normalized) || "ADMIN".equals(normalized);
    }

    public boolean isMeetingOwner() {
        return isMeetingOwner;
    }

    public String getCurrentUserId() {
        return repository.getCurrentUserId();
    }

    public String getCurrentUserName() {
        return repository.getCurrentUserName();
    }

    public void loadInitialData() {
        loadMeetingInfo();
        loadMeetingSettings();
        if (isHostLikeRole()) {
            loadWaitingParticipants();
        } else {
            updateState(state -> {
                state.setWaitingParticipants(java.util.Collections.emptyList());
                state.setWaitingBadgeVisible(false);
            });
        }
    }

    public void connectRealtime() {
        repository.connectMeetingRealtime(isHostLikeRole(), new MeetingRepository.MeetingRealtimeListener() {
            @Override
            public void onConnected() {
                loadChatHistory();
            }

            @Override
            public void onDisconnected() {
            }

            @Override
            public void onError(String message) {
            }

            @Override
            public void onSystemMessage(String body) {
                handleSystemRealtime(body);
            }

            @Override
            public void onChatMessage(ChatMessageResponse message) {
                appendChatMessage(message, true);
            }

            @Override
            public void onJoinRequest(String participantId, String displayName) {
                uiEvent.postValue(new Event<>(MeetingUiEvent.showJoinRequest(participantId, displayName)));
            }

            @Override
            public void onJoinRequestLeft(String participantId) {
                uiEvent.postValue(new Event<>(MeetingUiEvent.hideJoinRequest(participantId)));
            }

            @Override
            public void onWaitingRoomSignal(String body) {
                if ("SETTINGS_CHANGED".equalsIgnoreCase(body) || "HOST_JOINED".equalsIgnoreCase(body)) {
                    loadWaitingParticipants();
                }
            }
        });
    }

    public void disconnectRealtime() {
        repository.disconnectRealtime();
    }

    public void startWaitingRoomPolling() {
        if (!isHostLikeRole() || waitingRoomPollingActive) {
            return;
        }
        waitingRoomPollingActive = true;
        waitingRoomHandler.removeCallbacks(waitingRoomPollingRunnable);
        waitingRoomHandler.postDelayed(waitingRoomPollingRunnable, 5000);
    }

    public void stopWaitingRoomPolling() {
        waitingRoomPollingActive = false;
        waitingRoomHandler.removeCallbacks(waitingRoomPollingRunnable);
    }

    public void loadMeetingInfo() {
        repository.getMeetingInfo(new MeetingRepository.DataCallback<MeetingInfoResponse>() {
            @Override
            public void onSuccess(MeetingInfoResponse data) {
                updateState(state -> state.setMeetingTitle(data.getMeetingCode() + " • " + data.getTitle()));
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    public void loadMeetingSettings() {
        repository.getMeetingSettings(new MeetingRepository.DataCallback<String>() {
            @Override
            public void onSuccess(String data) {
                updateState(state -> state.setCurrentMeetingSettings(data));
                uiEvent.postValue(new Event<>(MeetingUiEvent.connectLiveKit()));
            }

            @Override
            public void onError(String message) {
                uiEvent.postValue(new Event<>(MeetingUiEvent.connectLiveKit()));
            }
        });
    }

    public void loadWaitingParticipants() {
        repository.getWaitingRoom(new MeetingRepository.DataCallback<List<ParticipantResponse>>() {
            @Override
            public void onSuccess(List<ParticipantResponse> data) {
                updateState(state -> {
                    state.setWaitingParticipants(data);
                    state.setWaitingBadgeVisible(!data.isEmpty());
                });
            }

            @Override
            public void onError(String message) {
                updateState(state -> {
                    state.setWaitingParticipants(java.util.Collections.emptyList());
                    state.setWaitingBadgeVisible(false);
                });
            }
        });
    }

    public void approveWaitingParticipant(String participantId, String action) {
        repository.approveParticipant(participantId, action, new MeetingRepository.DataCallback<String>() {
            @Override
            public void onSuccess(String data) {
                uiEvent.postValue(new Event<>(MeetingUiEvent.toast(data)));
                loadWaitingParticipants();
            }

            @Override
            public void onError(String message) {
                uiEvent.postValue(new Event<>(MeetingUiEvent.toast(message)));
            }
        });
    }

    public void updateMeetingSettings(Map<String, Object> settings, String successMessage) {
        repository.updateMeetingSettings(settings, new MeetingRepository.DataCallback<MeetingResponse>() {
            @Override
            public void onSuccess(MeetingResponse data) {
                uiEvent.postValue(new Event<>(MeetingUiEvent.toast(successMessage)));
                loadMeetingSettings();
                loadWaitingParticipants();
            }

            @Override
            public void onError(String message) {
                uiEvent.postValue(new Event<>(MeetingUiEvent.toast(message)));
            }
        });
    }

    public void loadChatHistory() {
        repository.getChatHistory(new MeetingRepository.DataCallback<List<ChatMessageResponse>>() {
            @Override
            public void onSuccess(List<ChatMessageResponse> data) {
                updateState(state -> state.setChatMessages(data));
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    public void sendChatMessage(String content, boolean chatEnabled) {
        if (!chatEnabled) {
            uiEvent.postValue(new Event<>(MeetingUiEvent.toast("Chat has been disabled by the host")));
            return;
        }
        repository.sendChatMessage(content, new MeetingRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
            }

            @Override
            public void onError(String message) {
                uiEvent.postValue(new Event<>(MeetingUiEvent.toast(message)));
            }
        });
    }

    public void sendSystemAction(String payload) {
        repository.sendSystemAction(payload, new MeetingRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
            }

            @Override
            public void onError(String message) {
                uiEvent.postValue(new Event<>(MeetingUiEvent.toast(message)));
            }
        });
    }

    public void leaveMeeting() {
        repository.leaveMeeting(new MeetingRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                uiEvent.postValue(new Event<>(MeetingUiEvent.toast("Left the meeting")));
                uiEvent.postValue(new Event<>(MeetingUiEvent.navigateToSummary("LEAVE")));
            }

            @Override
            public void onError(String message) {
                uiEvent.postValue(new Event<>(MeetingUiEvent.toast(message)));
            }
        });
    }

    public void endMeeting() {
        repository.endMeeting(new MeetingRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                uiEvent.postValue(new Event<>(MeetingUiEvent.toast("Ended the meeting")));
                uiEvent.postValue(new Event<>(MeetingUiEvent.navigateToSummary("END")));
            }

            @Override
            public void onError(String message) {
                uiEvent.postValue(new Event<>(MeetingUiEvent.toast(message)));
            }
        });
    }

    public void toggleRecording() {
        MeetingUiState current = requireState();
        if (!isMeetingOwner) {
            uiEvent.postValue(new Event<>(MeetingUiEvent.toast("Only the meeting owner can record")));
            return;
        }
        if (current.isRecordingRequestInFlight()) {
            return;
        }
        if (current.isRecordingActive()) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    public void startRecording() {
        updateState(state -> {
            state.setRecordingRequestInFlight(true);
            state.setRecordingStatus("STARTING");
        });
        repository.startRecording(new MeetingRepository.DataCallback<MeetingRecordingResponse>() {
            @Override
            public void onSuccess(MeetingRecordingResponse data) {
                updateState(state -> {
                    state.setRecordingRequestInFlight(false);
                    state.setRecordingActive(true);
                    state.setRecordingEgressId(data.getEgressId());
                    state.setRecordingStatus(safeRecordingStatus(data.getStatus(), "RECORDING"));
                });
                publishRecordingAction("RECORDING_STARTED");
                uiEvent.postValue(new Event<>(MeetingUiEvent.toast("Recording started")));
            }

            @Override
            public void onError(String message) {
                updateState(state -> {
                    state.setRecordingRequestInFlight(false);
                    state.setRecordingActive(false);
                    state.setRecordingStatus("FAILED");
                });
                uiEvent.postValue(new Event<>(MeetingUiEvent.toast(message)));
            }
        });
    }

    public void stopRecording() {
        MeetingUiState current = requireState();
        if (current.getRecordingEgressId() == null || current.getRecordingEgressId().trim().isEmpty()) {
            updateState(state -> {
                state.setRecordingActive(false);
                state.setRecordingStatus("FAILED");
            });
            uiEvent.postValue(new Event<>(MeetingUiEvent.toast("Missing recording session id")));
            return;
        }

        updateState(state -> {
            state.setRecordingRequestInFlight(true);
            state.setRecordingStatus("STOPPING");
        });
        repository.stopRecording(current.getRecordingEgressId(), new MeetingRepository.DataCallback<MeetingRecordingResponse>() {
            @Override
            public void onSuccess(MeetingRecordingResponse data) {
                updateState(state -> {
                    state.setRecordingRequestInFlight(false);
                    state.setRecordingActive(false);
                    state.setRecordingStatus(safeRecordingStatus(data.getStatus(), "STOPPING"));
                });
                publishRecordingAction("RECORDING_STOPPED");
                startRecordingStatusPolling();
                uiEvent.postValue(new Event<>(MeetingUiEvent.toast("Stopping recording")));
            }

            @Override
            public void onError(String message) {
                updateState(state -> {
                    state.setRecordingRequestInFlight(false);
                    state.setRecordingStatus("FAILED");
                });
                uiEvent.postValue(new Event<>(MeetingUiEvent.toast(message)));
            }
        });
    }

    private void startRecordingStatusPolling() {
        recordingStatusHandler.removeCallbacks(recordingStatusRunnable);
        recordingStatusHandler.postDelayed(recordingStatusRunnable, 3000);
    }

    public void stopRecordingStatusPolling() {
        recordingStatusHandler.removeCallbacks(recordingStatusRunnable);
    }

    private void pollRecordingStatus() {
        String egressId = requireState().getRecordingEgressId();
        if (egressId == null || egressId.trim().isEmpty()) {
            stopRecordingStatusPolling();
            return;
        }
        repository.getRecordingStatus(egressId, new MeetingRepository.DataCallback<MeetingRecordingResponse>() {
            @Override
            public void onSuccess(MeetingRecordingResponse data) {
                String status = safeRecordingStatus(data.getStatus(), "STOPPING");
                updateState(state -> state.setRecordingStatus(status));
                if ("COMPLETED".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status)) {
                    stopRecordingStatusPolling();
                    if ("COMPLETED".equalsIgnoreCase(status)) {
                        uiEvent.postValue(new Event<>(MeetingUiEvent.toast("Recording saved")));
                    }
                } else {
                    recordingStatusHandler.postDelayed(recordingStatusRunnable, 5000);
                }
            }

            @Override
            public void onError(String message) {
                updateState(state -> state.setRecordingStatus("FAILED"));
                stopRecordingStatusPolling();
            }
        });
    }

    private void handleSystemRealtime(String body) {
        if ("MEETING_ENDED".equalsIgnoreCase(body)) {
            uiEvent.postValue(new Event<>(MeetingUiEvent.toast("The meeting has ended")));
            uiEvent.postValue(new Event<>(MeetingUiEvent.navigateToSummary("ENDED_BY_HOST")));
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(body);
            String type = jsonObject.optString("type");
            if ("SETTINGS_UPDATED".equalsIgnoreCase(type)) {
                JSONObject settingsObj = jsonObject.optJSONObject("settings");
                if (settingsObj != null) {
                    updateState(state -> state.setCurrentMeetingSettings(settingsObj.toString()));
                }
                return;
            }
            if ("HOST_TRANSFERRED".equalsIgnoreCase(type)) {
                String newHostId = jsonObject.optString("newHostId");
                if (newHostId != null && newHostId.equals(repository.getCurrentUserId())) {
                    hostPrivilegesGranted = true;
                    uiEvent.postValue(new Event<>(MeetingUiEvent.toast("You have been transferred host privileges")));
                    loadWaitingParticipants();
                }
                return;
            }
            if ("RECORDING_STARTED".equalsIgnoreCase(type)) {
                updateState(state -> {
                    state.setRecordingActive(true);
                    state.setRecordingStatus("RECORDING");
                });
                return;
            }
            if ("RECORDING_STOPPED".equalsIgnoreCase(type)) {
                updateState(state -> {
                    state.setRecordingActive(false);
                    state.setRecordingStatus(isMeetingOwner && state.getRecordingEgressId() != null ? "STOPPING" : "IDLE");
                });
                return;
            }
            if ("MUTE_ALL".equalsIgnoreCase(type)) {
                uiEvent.postValue(new Event<>(MeetingUiEvent.applyRemoteMicMute("The host muted everyone's microphone.")));
                return;
            }
            if ("STOP_CAMERA_ALL".equalsIgnoreCase(type)) {
                uiEvent.postValue(new Event<>(MeetingUiEvent.applyRemoteCameraOff("The host disabled everyone's camera.")));
                return;
            }
            if ("KICK_ALL".equalsIgnoreCase(type)) {
                if (!isHostLikeRole()) {
                    uiEvent.postValue(new Event<>(MeetingUiEvent.toast("You have been removed from the meeting")));
                    uiEvent.postValue(new Event<>(MeetingUiEvent.navigateToSummary("KICKED")));
                }
                return;
            }

            String targetParticipantId = jsonObject.optString("targetParticipantId");
            if (targetParticipantId != null && targetParticipantId.equals(repository.getCurrentUserId())) {
                if ("MUTE_PARTICIPANT".equalsIgnoreCase(type)) {
                    uiEvent.postValue(new Event<>(MeetingUiEvent.applyRemoteMicMute("The host muted your microphone.")));
                } else if ("STOP_CAMERA_PARTICIPANT".equalsIgnoreCase(type)) {
                    uiEvent.postValue(new Event<>(MeetingUiEvent.applyRemoteCameraOff("The host disabled your camera.")));
                } else if ("KICK_PARTICIPANT".equalsIgnoreCase(type)) {
                    uiEvent.postValue(new Event<>(MeetingUiEvent.toast("You have been removed from the meeting by the host")));
                    uiEvent.postValue(new Event<>(MeetingUiEvent.navigateToSummary("KICKED")));
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void appendChatMessage(ChatMessageResponse message, boolean allowSound) {
        updateState(state -> {
            java.util.ArrayList<ChatMessageResponse> updated = new java.util.ArrayList<>(state.getChatMessages());
            updated.add(message);
            state.setChatMessages(updated);
        });
        if (allowSound && message.getSenderId() != null && !message.getSenderId().equals(repository.getCurrentUserId())) {
            uiEvent.postValue(new Event<>(MeetingUiEvent.playChatSound()));
        }
    }

    private String safeRecordingStatus(String status, String fallback) {
        return status == null || status.trim().isEmpty() ? fallback : status.trim().toUpperCase();
    }

    private void publishRecordingAction(String type) {
        sendSystemAction(SystemActionHelper.createPayload(type));
    }

    private void updateState(StateMutation mutation) {
        MeetingUiState base = requireState();
        MeetingUiState copy = new MeetingUiState(base);
        mutation.apply(copy);
        uiState.postValue(copy);
    }

    private MeetingUiState requireState() {
        MeetingUiState state = uiState.getValue();
        return state != null ? state : new MeetingUiState();
    }

    @Override
    protected void onCleared() {
        stopWaitingRoomPolling();
        stopRecordingStatusPolling();
        repository.disconnectRealtime();
        super.onCleared();
    }

    private interface StateMutation {
        void apply(MeetingUiState state);
    }
}
