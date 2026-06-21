package com.ptithcm.ptitmeet.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.ptitmeet.api.dto.auth.AuthResponse;
import com.ptithcm.ptitmeet.repository.AuthRepository;

public class LoginViewModel extends AndroidViewModel {

    private final AuthRepository repository;
    private final MutableLiveData<LoginUiState> uiState = new MutableLiveData<>(new LoginUiState());
    private final MutableLiveData<Event<LoginUiEvent>> uiEvent = new MutableLiveData<>();

    public LoginViewModel(@NonNull Application application) {
        super(application);
        this.repository = new AuthRepository(application);
    }

    public LiveData<LoginUiState> getUiState() {
        return uiState;
    }

    public LiveData<Event<LoginUiEvent>> getUiEvent() {
        return uiEvent;
    }

    public void login(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            uiEvent.setValue(new Event<>(LoginUiEvent.toast("Please enter both email and password")));
            return;
        }

        updateState(true);
        repository.login(email, password, new AuthRepository.DataCallback<AuthResponse>() {
            @Override
            public void onSuccess(AuthResponse data) {
                updateState(false);
                uiEvent.postValue(new Event<>(LoginUiEvent.toast("Welcome " + data.getUser().getFullName())));
                uiEvent.postValue(new Event<>(LoginUiEvent.navigateMain()));
            }

            @Override
            public void onError(String message) {
                updateState(false);
                uiEvent.postValue(new Event<>(LoginUiEvent.toast(message)));
            }
        });
    }

    private void updateState(boolean loading) {
        LoginUiState current = uiState.getValue();
        LoginUiState copy = current != null ? new LoginUiState(current) : new LoginUiState();
        copy.setLoading(loading);
        uiState.postValue(copy);
    }
}
