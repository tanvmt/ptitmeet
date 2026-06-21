package com.ptithcm.ptitmeet.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.ptitmeet.api.dto.user.UserResponse;
import com.ptithcm.ptitmeet.repository.ProfileRepository;

public class ProfileViewModel extends AndroidViewModel {

    private final ProfileRepository repository;
    private final MutableLiveData<ProfileUiState> uiState = new MutableLiveData<>(new ProfileUiState());
    private final MutableLiveData<Event<ProfileUiEvent>> uiEvent = new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        this.repository = new ProfileRepository(application);
    }

    public LiveData<ProfileUiState> getUiState() {
        return uiState;
    }

    public LiveData<Event<ProfileUiEvent>> getUiEvent() {
        return uiEvent;
    }

    public void loadProfile() {
        updateState(state -> {
            state.setLoading(true);
            state.setStatusMessage("Loading profile...");
        });
        repository.getProfile(new ProfileRepository.DataCallback<UserResponse>() {
            @Override
            public void onSuccess(UserResponse data) {
                repository.updateCachedUserName(data.getFullName());
                updateState(state -> {
                    state.setLoading(false);
                    state.setUser(data);
                    state.setStatusMessage("Keep your display name and profile photo up to date.");
                });
            }

            @Override
            public void onError(String message) {
                updateState(state -> {
                    state.setLoading(false);
                    state.setStatusMessage(message);
                });
            }
        });
    }

    public void uploadAvatar(Uri uri) {
        updateState(state -> state.setAvatarUploading(true));
        repository.uploadAvatar(uri, new ProfileRepository.DataCallback<UserResponse>() {
            @Override
            public void onSuccess(UserResponse data) {
                repository.updateCachedUserName(data.getFullName());
                updateState(state -> {
                    state.setAvatarUploading(false);
                    state.setUser(data);
                    state.setStatusMessage("Avatar updated.");
                });
                uiEvent.postValue(new Event<>(ProfileUiEvent.toast("Avatar updated")));
            }

            @Override
            public void onError(String message) {
                updateState(state -> {
                    state.setAvatarUploading(false);
                    state.setStatusMessage(message);
                });
            }
        });
    }

    public void updateProfile(String fullName, String avatarUrl) {
        if (fullName.length() < 2) {
            updateState(state -> state.setStatusMessage("Full name must contain at least 2 characters."));
            return;
        }
        updateState(state -> state.setSaving(true));
        repository.updateProfile(fullName, avatarUrl == null || avatarUrl.trim().isEmpty() ? null : avatarUrl.trim(), new ProfileRepository.DataCallback<UserResponse>() {
            @Override
            public void onSuccess(UserResponse data) {
                updateState(state -> {
                    state.setSaving(false);
                    state.setUser(data);
                    state.setStatusMessage("Profile updated.");
                });
                uiEvent.postValue(new Event<>(ProfileUiEvent.toast("Profile updated")));
            }

            @Override
            public void onError(String message) {
                updateState(state -> {
                    state.setSaving(false);
                    state.setStatusMessage(message);
                });
            }
        });
    }

    public void logout() {
        repository.logout();
        uiEvent.setValue(new Event<>(ProfileUiEvent.openLogin()));
    }

    private void updateState(StateMutation mutation) {
        ProfileUiState current = uiState.getValue();
        ProfileUiState copy = current != null ? new ProfileUiState(current) : new ProfileUiState();
        mutation.apply(copy);
        uiState.postValue(copy);
    }

    private interface StateMutation {
        void apply(ProfileUiState state);
    }
}
