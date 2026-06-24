package com.ptithcm.ptitmeet.viewmodel;

public class ScheduleMeetingUiState {
    private boolean submitting;

    public ScheduleMeetingUiState() {
    }

    public ScheduleMeetingUiState(ScheduleMeetingUiState other) {
        this.submitting = other.submitting;
    }

    public boolean isSubmitting() {
        return submitting;
    }

    public void setSubmitting(boolean submitting) {
        this.submitting = submitting;
    }
}
