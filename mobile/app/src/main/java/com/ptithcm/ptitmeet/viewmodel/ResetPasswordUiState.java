package com.ptithcm.ptitmeet.viewmodel;

public class ResetPasswordUiState {
    private boolean loading;
    private String message = "";

    public ResetPasswordUiState() {
    }

    public ResetPasswordUiState(ResetPasswordUiState other) {
        this.loading = other.loading;
        this.message = other.message;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
