package com.ptithcm.ptitmeet.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.ptitmeet.api.dto.recording.MeetingRecordingResponse;
import com.ptithcm.ptitmeet.repository.RecordingsRepository;

import java.util.Collections;
import java.util.List;

public class RecordingsViewModel extends AndroidViewModel {

    private final RecordingsRepository repository;
    private final MutableLiveData<RecordingsUiState> uiState = new MutableLiveData<>(new RecordingsUiState());

    public RecordingsViewModel(@NonNull Application application) {
        super(application);
        this.repository = new RecordingsRepository(application);
    }

    public LiveData<RecordingsUiState> getUiState() {
        return uiState;
    }

    public void loadRecordings() {
        updateState(true, "Loading recordings...", Collections.emptyList());
        repository.getMyRecordings(new RecordingsRepository.DataCallback<List<MeetingRecordingResponse>>() {
            @Override
            public void onSuccess(List<MeetingRecordingResponse> data) {
                String message = data.isEmpty()
                        ? "No recordings yet. Start recording from an active meeting."
                        : data.size() + " recording(s) found";
                updateState(false, message, data);
            }

            @Override
            public void onError(String message) {
                updateState(false, message, Collections.emptyList());
            }
        });
    }

    private void updateState(boolean loading, String message, List<MeetingRecordingResponse> recordings) {
        RecordingsUiState current = uiState.getValue();
        RecordingsUiState copy = current != null ? new RecordingsUiState(current) : new RecordingsUiState();
        copy.setLoading(loading);
        copy.setStateMessage(message);
        copy.setRecordings(recordings);
        uiState.postValue(copy);
    }
}
