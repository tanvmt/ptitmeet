package com.ptithcm.ptitmeet.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.ptitmeet.api.dto.chat.ChatMessageResponse;
import com.ptithcm.ptitmeet.api.dto.common.PageResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.JoinMeetingResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingHistoryResponse;
import com.ptithcm.ptitmeet.repository.MeetingsRepository;

import java.util.Collections;
import java.util.List;

public class MeetingsViewModel extends AndroidViewModel {

    private final MeetingsRepository repository;
    private final MutableLiveData<MeetingsUiState> uiState = new MutableLiveData<>(new MeetingsUiState());
    private final MutableLiveData<Event<MeetingsUiEvent>> uiEvent = new MutableLiveData<>();

    public MeetingsViewModel(@NonNull Application application) {
        super(application);
        this.repository = new MeetingsRepository(application);
        updateState(state -> state.setJoinDisplayName(repository.getUserName()));
    }

    public LiveData<MeetingsUiState> getUiState() {
        return uiState;
    }

    public LiveData<Event<MeetingsUiEvent>> getUiEvent() {
        return uiEvent;
    }

    public void loadMeetings(int currentPage, int pageSize, String role, String status) {
        updateState(state -> state.setLoading(true));
        repository.getMeetingHistory(currentPage, pageSize, role, status, new MeetingsRepository.DataCallback<PageResponse<MeetingHistoryResponse>>() {
            @Override
            public void onSuccess(PageResponse<MeetingHistoryResponse> data) {
                int totalPages = data.getTotalPages() < 1 ? 1 : data.getTotalPages();
                List<MeetingHistoryResponse> content = data.getContent() == null ? Collections.emptyList() : data.getContent();
                updateState(state -> {
                    state.setLoading(false);
                    state.setCurrentPage(currentPage);
                    state.setTotalPages(totalPages);
                    state.setMeetings(content);
                    state.setSummaryText(content.isEmpty()
                            ? "No meetings match this filter"
                            : data.getTotalElements() + " meetings found");
                });
            }

            @Override
            public void onError(String message) {
                updateState(state -> {
                    state.setLoading(false);
                    state.setCurrentPage(1);
                    state.setTotalPages(1);
                    state.setMeetings(Collections.emptyList());
                    state.setSummaryText(message);
                });
            }
        });
    }

    public void joinMeeting(MeetingHistoryResponse meeting) {
        repository.joinMeeting(meeting.getMeetingCode(), new MeetingsRepository.DataCallback<JoinMeetingResponse>() {
            @Override
            public void onSuccess(JoinMeetingResponse data) {
                if ("PENDING".equalsIgnoreCase(data.getStatus())) {
                    uiEvent.postValue(new Event<>(MeetingsUiEvent.openWaitingRoom(meeting.getMeetingCode(), repository.getUserName(), data.getMessage())));
                } else {
                    uiEvent.postValue(new Event<>(MeetingsUiEvent.openMeetingRoom(meeting.getMeetingCode(), data)));
                }
            }

            @Override
            public void onError(String message) {
                uiEvent.postValue(new Event<>(MeetingsUiEvent.toast("Unable to join: " + message)));
            }
        });
    }

    public void cancelMeeting(MeetingHistoryResponse meeting, int currentPage, int pageSize, String role, String status) {
        repository.cancelMeeting(meeting.getMeetingCode(), new MeetingsRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                uiEvent.postValue(new Event<>(MeetingsUiEvent.toast("Meeting canceled successfully")));
                loadMeetings(currentPage, pageSize, role, status);
            }

            @Override
            public void onError(String message) {
                uiEvent.postValue(new Event<>(MeetingsUiEvent.toast(message)));
            }
        });
    }

    public void loadChatHistory(MeetingHistoryResponse meeting) {
        repository.getChatHistory(meeting.getMeetingCode(), new MeetingsRepository.DataCallback<List<ChatMessageResponse>>() {
            @Override
            public void onSuccess(List<ChatMessageResponse> data) {
                uiEvent.postValue(new Event<>(MeetingsUiEvent.showChatHistory(meeting.getTitle(), data)));
            }

            @Override
            public void onError(String message) {
                uiEvent.postValue(new Event<>(MeetingsUiEvent.toast(message)));
            }
        });
    }

    private void updateState(StateMutation mutation) {
        MeetingsUiState current = uiState.getValue();
        MeetingsUiState copy = current != null ? new MeetingsUiState(current) : new MeetingsUiState();
        mutation.apply(copy);
        uiState.postValue(copy);
    }

    private interface StateMutation {
        void apply(MeetingsUiState state);
    }
}
