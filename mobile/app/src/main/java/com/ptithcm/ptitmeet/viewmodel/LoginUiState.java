package com.ptithcm.ptitmeet.viewmodel;

public class LoginUiState {
    private boolean loading;

    public LoginUiState() {
    }

    public LoginUiState(LoginUiState other) {
        this.loading = other.loading;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }
}
