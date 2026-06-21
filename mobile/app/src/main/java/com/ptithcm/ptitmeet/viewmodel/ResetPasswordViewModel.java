package com.ptithcm.ptitmeet.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.ptitmeet.repository.AuthRepository;

public class ResetPasswordViewModel extends AndroidViewModel {

    private final AuthRepository repository;
    private final MutableLiveData<ResetPasswordUiState> uiState = new MutableLiveData<>(new ResetPasswordUiState());
    private final MutableLiveData<Event<ResetPasswordUiEvent>> uiEvent = new MutableLiveData<>();

    public ResetPasswordViewModel(@NonNull Application application) {
        super(application);
        this.repository = new AuthRepository(application);
    }

    public LiveData<ResetPasswordUiState> getUiState() {
        return uiState;
    }

    public LiveData<Event<ResetPasswordUiEvent>> getUiEvent() {
        return uiEvent;
    }

    public void resetPassword(String token, String password, String confirmPassword) {
        if (token.isEmpty()) {
            updateState(false, "Reset token is required.");
            return;
        }
        if (password.length() < 8) {
            updateState(false, "Password must be at least 8 characters.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            updateState(false, "Passwords do not match.");
            return;
        }
        updateState(true, "");
        repository.resetPassword(token, password, new AuthRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                updateState(false, "Password updated. You can sign in now.");
                uiEvent.postValue(new Event<>(ResetPasswordUiEvent.toast("Password updated")));
                uiEvent.postValue(new Event<>(ResetPasswordUiEvent.openLogin()));
            }

            @Override
            public void onError(String message) {
                updateState(false, message);
            }
        });
    }

    private void updateState(boolean loading, String message) {
        ResetPasswordUiState current = uiState.getValue();
        ResetPasswordUiState copy = current != null ? new ResetPasswordUiState(current) : new ResetPasswordUiState();
        copy.setLoading(loading);
        copy.setMessage(message);
        uiState.postValue(copy);
    }
}
