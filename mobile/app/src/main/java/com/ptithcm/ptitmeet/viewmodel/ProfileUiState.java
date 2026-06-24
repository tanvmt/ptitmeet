package com.ptithcm.ptitmeet.viewmodel;

import com.ptithcm.ptitmeet.api.dto.user.UserResponse;

public class ProfileUiState {
    private boolean loading;
    private boolean saving;
    private boolean avatarUploading;
    private String statusMessage = "";
    private UserResponse user;

    public ProfileUiState() {
    }

    public ProfileUiState(ProfileUiState other) {
        this.loading = other.loading;
        this.saving = other.saving;
        this.avatarUploading = other.avatarUploading;
        this.statusMessage = other.statusMessage;
        this.user = other.user;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public boolean isSaving() {
        return saving;
    }

    public void setSaving(boolean saving) {
        this.saving = saving;
    }

    public boolean isAvatarUploading() {
        return avatarUploading;
    }

    public void setAvatarUploading(boolean avatarUploading) {
        this.avatarUploading = avatarUploading;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }
}
