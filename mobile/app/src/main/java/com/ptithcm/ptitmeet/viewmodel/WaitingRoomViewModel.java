package com.ptithcm.ptitmeet.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.ptithcm.ptitmeet.api.dto.meeting.JoinMeetingResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingInfoResponse;
import com.ptithcm.ptitmeet.repository.MeetingRepository;

import org.json.JSONObject;

public class WaitingRoomViewModel extends AndroidViewModel {

    public static class Factory implements ViewModelProvider.Factory {
        private final Application application;
        private final String meetingCode;
        private final String displayName;
        private final String waitingMessage;
        private final boolean hostSetup;

        public Factory(Application application, String meetingCode, String displayName, String waitingMessage, boolean hostSetup) {
            this.application = application;
            this.meetingCode = meetingCode;
            this.displayName = displayName;
            this.waitingMessage = waitingMessage;
            this.hostSetup = hostSetup;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new WaitingRoomViewModel(application, meetingCode, displayName, waitingMessage, hostSetup);
        }
    }

    private final MeetingRepository repository;
    private final String displayName;
    private final boolean isHostSetup;
    private final MutableLiveData<WaitingRoomUiState> uiState = new MutableLiveData<>(new WaitingRoomUiState());
    private final MutableLiveData<Event<WaitingRoomUiEvent>> uiEvent = new MutableLiveData<>();
    private final Handler pollingHandler = new Handler(Looper.getMainLooper());
    private boolean pollingActive;

    private final Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            WaitingRoomUiState state = requireState();
            if (!pollingActive || !state.isWaitingState()) {
                return;
            }
            if (!state.isRealtimeConnected()) {
                requestJoin();
            }
            if (pollingActive && state.isWaitingState()) {
                pollingHandler.postDelayed(this, 10000);
            }
        }
    };

    public WaitingRoomViewModel(@NonNull Application application, String meetingCode, String displayName, String waitingMessage, boolean hostSetup) {
        super(application);
        this.repository = new MeetingRepository(application, meetingCode);
        this.displayName = (displayName == null || displayName.trim().isEmpty()) ? repository.getCurrentUserName() : displayName;
        this.isHostSetup = hostSetup;
        if (waitingMessage != null && !waitingMessage.trim().isEmpty()) {
            updateState(state -> applyWaitingState(state, waitingMessage));
        } else {
            updateState(this::applyReadyState);
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public LiveData<WaitingRoomUiState> getUiState() {
        return uiState;
    }

    public LiveData<Event<WaitingRoomUiEvent>> getUiEvent() {
        return uiEvent;
    }

    public void loadMeetingInfo() {
        repository.getMeetingInfo(new MeetingRepository.DataCallback<MeetingInfoResponse>() {
            @Override
            public void onSuccess(MeetingInfoResponse data) {
                updateState(state -> {
                    state.setReadyTitle(isHostSetup ? "Set up before you go live" : "Ready to join?");
                    state.setMeetingDetails("Meeting: " + data.getTitle() + "\nHost: " + data.getHostName());
                });
            }

            @Override
            public void onError(String message) {
                updateState(state -> state.setMeetingDetails("Unable to load meeting details."));
            }
        });
    }

    public void connectRealtime() {
        repository.connectWaitingRoomRealtime(new MeetingRepository.WaitingRoomRealtimeListener() {
            @Override
            public void onConnected() {
                updateState(state -> state.setRealtimeConnected(true));
            }

            @Override
            public void onDisconnected() {
                updateState(state -> state.setRealtimeConnected(false));
            }

            @Override
            public void onError(String message) {
                updateState(state -> state.setRealtimeConnected(false));
            }

            @Override
            public void onUserUpdate(String body) {
                handleRealtimeUserMessage(body);
            }

            @Override
            public void onWaitingRoomSignal(String body) {
                handleWaitingRoomRealtime(body);
            }
        });
    }

    public void disconnectRealtime() {
        updateState(state -> state.setRealtimeConnected(false));
        repository.disconnectRealtime();
    }

    public void requestJoin() {
        WaitingRoomUiState current = requireState();
        if (current.isJoinInFlight()) {
            return;
        }
        updateState(state -> {
            state.setJoinInFlight(true);
            state.setJoinButtonEnabled(false);
            state.setJoinButtonText(state.isWaitingState() ? "Checking..." : (isHostSetup ? "Preparing..." : "Joining..."));
        });
        repository.joinMeeting(displayName, new MeetingRepository.DataCallback<JoinMeetingResponse>() {
            @Override
            public void onSuccess(JoinMeetingResponse data) {
                updateState(state -> {
                    state.setJoinInFlight(false);
                    state.setJoinButtonEnabled(true);
                });
                if ("PENDING".equalsIgnoreCase(data.getStatus())) {
                    updateState(state -> applyWaitingState(state, data.getMessage()));
                    return;
                }
                if ("REJECTED".equalsIgnoreCase(data.getStatus())) {
                    uiEvent.postValue(new Event<>(WaitingRoomUiEvent.toast(data.getMessage())));
                    updateState(WaitingRoomViewModel.this::applyReadyState);
                    return;
                }
                stopPolling();
                uiEvent.postValue(new Event<>(WaitingRoomUiEvent.openMeetingRoom(data)));
            }

            @Override
            public void onError(String message) {
                updateState(state -> {
                    state.setJoinInFlight(false);
                    state.setJoinButtonEnabled(true);
                    state.setJoinButtonText(state.isWaitingState() ? "Check Again" : (isHostSetup ? "Start meeting" : "Join now"));
                });
                uiEvent.postValue(new Event<>(WaitingRoomUiEvent.toast(message)));
            }
        });
    }

    public void startPolling() {
        if (pollingActive) {
            return;
        }
        pollingActive = true;
        pollingHandler.removeCallbacks(pollingRunnable);
        pollingHandler.postDelayed(pollingRunnable, 10000);
    }

    public void stopPolling() {
        pollingActive = false;
        pollingHandler.removeCallbacks(pollingRunnable);
    }

    public void cancelWaiting() {
        stopPolling();
        repository.cancelWaiting();
        uiEvent.postValue(new Event<>(WaitingRoomUiEvent.toast("Stopped waiting for approval")));
        uiEvent.postValue(new Event<>(WaitingRoomUiEvent.finishScreen()));
    }

    private void handleRealtimeUserMessage(String body) {
        try {
            JSONObject jsonObject = new JSONObject(body);
            String status = jsonObject.optString("status", jsonObject.optString("action"));
            if ("APPROVED".equalsIgnoreCase(status)) {
                stopPolling();
                uiEvent.postValue(new Event<>(WaitingRoomUiEvent.openMeetingRoom(parseJoinMeetingResponse(jsonObject))));
                return;
            }
            if ("REJECTED".equalsIgnoreCase(status)) {
                String message = jsonObject.optString("message", "Your request to join was rejected.");
                uiEvent.postValue(new Event<>(WaitingRoomUiEvent.toast(message)));
                updateState(this::applyReadyState);
            }
        } catch (Exception ignored) {
        }
    }

    private void handleWaitingRoomRealtime(String body) {
        if ("HOST_JOINED".equalsIgnoreCase(body)) {
            updateState(state -> applyWaitingState(state, "The host has joined the meeting. Please wait for approval."));
            requestJoin();
            return;
        }
        if ("SETTINGS_CHANGED".equalsIgnoreCase(body)) {
            requestJoin();
        }
    }

    private JoinMeetingResponse parseJoinMeetingResponse(JSONObject jsonObject) {
        String status = jsonObject.optString("status", jsonObject.optString("action", null));
        return new JoinMeetingResponse(
                jsonObject.optString("token", null),
                jsonObject.optString("serverUrl", null),
                status,
                jsonObject.optString("role", null),
                jsonObject.optString("message", null),
                jsonObject.optString("settings", null),
                jsonObject.optBoolean("isOwner", false),
                jsonObject.optString("currentHostId", null)
        );
    }

    private void applyWaitingState(WaitingRoomUiState state, String message) {
        state.setWaitingState(true);
        state.setWaitingMessage(message == null || message.trim().isEmpty()
                ? "Waiting for host to let you in..."
                : message);
        state.setJoinButtonText("Check Again");
        state.setJoinButtonEnabled(true);
    }

    private void applyReadyState(WaitingRoomUiState state) {
        state.setWaitingState(false);
        state.setWaitingMessage("");
        state.setJoinButtonText(isHostSetup ? "Start meeting" : "Join now");
        state.setJoinButtonEnabled(true);
    }

    private void updateState(StateMutation mutation) {
        WaitingRoomUiState current = requireState();
        WaitingRoomUiState copy = new WaitingRoomUiState(current);
        mutation.apply(copy);
        uiState.postValue(copy);
    }

    private WaitingRoomUiState requireState() {
        WaitingRoomUiState state = uiState.getValue();
        return state != null ? state : new WaitingRoomUiState();
    }

    @Override
    protected void onCleared() {
        stopPolling();
        repository.disconnectRealtime();
        super.onCleared();
    }

    private interface StateMutation {
        void apply(WaitingRoomUiState state);
    }
}
