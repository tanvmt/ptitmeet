package com.ptithcm.ptitmeet.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.ptitmeet.api.dto.meeting.CreateMeetingRequest;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingHistoryResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingResponse;
import com.ptithcm.ptitmeet.repository.MainRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainViewModel extends AndroidViewModel {

    private final MainRepository repository;
    private final MutableLiveData<MainUiState> uiState = new MutableLiveData<>(new MainUiState());
    private final MutableLiveData<Event<MainUiEvent>> uiEvent = new MutableLiveData<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
        this.repository = new MainRepository(application);
        updateState(state -> {
            state.setFullName(repository.getUserName());
            state.setAvatarUrl(repository.getAvatarUrl());
            state.setWelcomeText("Welcome back, " + repository.getUserName());
        });
    }

    public LiveData<MainUiState> getUiState() {
        return uiState;
    }

    public LiveData<Event<MainUiEvent>> getUiEvent() {
        return uiEvent;
    }

    public void loadDashboardData() {
        loadUpNextMeeting();
        loadRecentActivity();
    }

    public void createNewMeeting() {
        updateState(state -> state.setCreatingMeeting(true));
        String fullName = repository.getUserName();
        CreateMeetingRequest request = new CreateMeetingRequest("Phòng họp của " + fullName);
        String nowPlus1Min = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .format(new Date(System.currentTimeMillis() + 60000));
        request.setStartTime(nowPlus1Min);

        repository.createInstantMeeting(request, new MainRepository.DataCallback<MeetingResponse>() {
            @Override
            public void onSuccess(MeetingResponse data) {
                updateState(state -> state.setCreatingMeeting(false));
                uiEvent.postValue(new Event<>(MainUiEvent.openWaitingRoom(data.getMeetingCode(), repository.getUserName(), true)));
            }

            @Override
            public void onError(String message) {
                updateState(state -> state.setCreatingMeeting(false));
                uiEvent.postValue(new Event<>(MainUiEvent.toast(message)));
            }
        });
    }

    public void joinMeeting(String meetingCode) {
        if (meetingCode == null || meetingCode.trim().isEmpty()) {
            uiEvent.setValue(new Event<>(MainUiEvent.toast("Mã phòng không được để trống")));
            return;
        }
        uiEvent.setValue(new Event<>(MainUiEvent.openWaitingRoom(meetingCode.trim(), repository.getUserName(), false)));
    }

    public void joinUpNextMeeting() {
        MainUiState current = uiState.getValue();
        if (current == null || current.getUpNextMeeting() == null || current.getUpNextMeeting().getMeetingCode() == null) {
            uiEvent.setValue(new Event<>(MainUiEvent.toast("Hiện chưa có cuộc họp sắp diễn ra")));
            return;
        }
        uiEvent.setValue(new Event<>(MainUiEvent.openWaitingRoom(current.getUpNextMeeting().getMeetingCode(), repository.getUserName(), false)));
    }

    private void loadUpNextMeeting() {
        repository.getUpNextMeeting(new MainRepository.DataCallback<MeetingHistoryResponse>() {
            @Override
            public void onSuccess(MeetingHistoryResponse data) {
                updateState(state -> state.setUpNextMeeting(data));
            }

            @Override
            public void onError(String message) {
                updateState(state -> state.setUpNextMeeting(null));
            }
        });
    }

    private void loadRecentActivity() {
        repository.getRecentActivity(new MainRepository.DataCallback<List<MeetingHistoryResponse>>() {
            @Override
            public void onSuccess(List<MeetingHistoryResponse> data) {
                updateState(state -> state.setRecentActivity(data));
            }

            @Override
            public void onError(String message) {
                updateState(state -> state.setRecentActivity(java.util.Collections.emptyList()));
            }
        });
    }

    private void updateState(StateMutation mutation) {
        MainUiState current = uiState.getValue();
        MainUiState copy = current != null ? new MainUiState(current) : new MainUiState();
        mutation.apply(copy);
        uiState.postValue(copy);
    }

    private interface StateMutation {
        void apply(MainUiState state);
    }
}
