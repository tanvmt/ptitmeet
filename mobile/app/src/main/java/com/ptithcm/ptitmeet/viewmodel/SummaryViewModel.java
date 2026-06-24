package com.ptithcm.ptitmeet.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.ptitmeet.api.dto.meeting.MeetingSummaryResponse;
import com.ptithcm.ptitmeet.repository.SummaryRepository;

public class SummaryViewModel extends AndroidViewModel {

    private final SummaryRepository repository;
    private final String meetingCode;
    private final String actionTaken;
    private final MutableLiveData<SummaryUiState> uiState = new MutableLiveData<>(new SummaryUiState());
    private final MutableLiveData<Event<SummaryUiEvent>> uiEvent = new MutableLiveData<>();

    public SummaryViewModel(@NonNull Application application, String meetingCode, String actionTaken) {
        super(application);
        this.repository = new SummaryRepository(application);
        this.meetingCode = meetingCode;
        this.actionTaken = actionTaken;
    }

    public LiveData<SummaryUiState> getUiState() {
        return uiState;
    }

    public LiveData<Event<SummaryUiEvent>> getUiEvent() {
        return uiEvent;
    }

    public String getDisplayName() {
        return repository.getUserName();
    }

    public void fetchSummaryData() {
        if (meetingCode == null || meetingCode.trim().isEmpty()) {
            updateState(state -> state.setSubtitle("Meeting ID: Unknown • -- duration"));
            return;
        }
        repository.getMeetingSummary(meetingCode, actionTaken, new SummaryRepository.DataCallback<MeetingSummaryResponse>() {
            @Override
            public void onSuccess(MeetingSummaryResponse data) {
                updateState(state -> {
                    state.setSubtitle("Meeting ID: " + meetingCode + " • " + data.getDuration() + " duration");
                    state.setParticipantsText(data.getParticipants() + " Joined");
                    state.setMessagesText(data.getMessages() + " Sent");
                });
            }

            @Override
            public void onError(String message) {
                updateState(state -> state.setSubtitle("Meeting ID: " + meetingCode + " • -- duration"));
            }
        });
    }

    public void submitRating(int rating) {
        SummaryUiState current = uiState.getValue();
        if (meetingCode == null || meetingCode.trim().isEmpty() || (current != null && current.isRatingSubmitted())) {
            return;
        }
        updateState(state -> state.setSelectedRating(rating));
        repository.submitFeedback(meetingCode, rating, new SummaryRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                updateState(state -> state.setRatingSubmitted(true));
            }

            @Override
            public void onError(String message) {
                uiEvent.postValue(new Event<>(SummaryUiEvent.toast(message)));
            }
        });
    }

    private void updateState(StateMutation mutation) {
        SummaryUiState current = uiState.getValue();
        SummaryUiState copy = current != null ? new SummaryUiState(current) : new SummaryUiState();
        mutation.apply(copy);
        uiState.postValue(copy);
    }

    public static class Factory implements androidx.lifecycle.ViewModelProvider.Factory {
        private final Application application;
        private final String meetingCode;
        private final String actionTaken;

        public Factory(Application application, String meetingCode, String actionTaken) {
            this.application = application;
            this.meetingCode = meetingCode;
            this.actionTaken = actionTaken;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new SummaryViewModel(application, meetingCode, actionTaken);
        }
    }

    private interface StateMutation {
        void apply(SummaryUiState state);
    }
}
