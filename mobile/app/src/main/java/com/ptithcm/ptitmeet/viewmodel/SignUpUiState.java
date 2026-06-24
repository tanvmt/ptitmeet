package com.ptithcm.ptitmeet.viewmodel;

public class SignUpUiState {
    private boolean loading;

    public SignUpUiState() {
    }

    public SignUpUiState(SignUpUiState other) {
        this.loading = other.loading;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }
}
