package com.ptithcm.ptitmeet.viewmodel;

import com.ptithcm.ptitmeet.api.dto.recording.MeetingRecordingResponse;

import java.util.ArrayList;
import java.util.List;

public class RecordingsUiState {
    private boolean loading;
    private String stateMessage = "Loading recordings...";
    private List<MeetingRecordingResponse> recordings = new ArrayList<>();

    public RecordingsUiState() {
    }

    public RecordingsUiState(RecordingsUiState other) {
        this.loading = other.loading;
        this.stateMessage = other.stateMessage;
        this.recordings = new ArrayList<>(other.recordings);
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public String getStateMessage() {
        return stateMessage;
    }

    public void setStateMessage(String stateMessage) {
        this.stateMessage = stateMessage;
    }

    public List<MeetingRecordingResponse> getRecordings() {
        return recordings;
    }

    public void setRecordings(List<MeetingRecordingResponse> recordings) {
        this.recordings = new ArrayList<>(recordings);
    }
}
