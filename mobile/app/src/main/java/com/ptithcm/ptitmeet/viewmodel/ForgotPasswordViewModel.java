package com.ptithcm.ptitmeet.viewmodel;

import android.app.Application;
import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.ptitmeet.repository.AuthRepository;

public class ForgotPasswordViewModel extends AndroidViewModel {

    private final AuthRepository repository;
    private final MutableLiveData<ForgotPasswordUiState> uiState = new MutableLiveData<>(new ForgotPasswordUiState());
    private final MutableLiveData<Event<ForgotPasswordUiEvent>> uiEvent = new MutableLiveData<>();

    public ForgotPasswordViewModel(@NonNull Application application) {
        super(application);
        this.repository = new AuthRepository(application);
    }

    public LiveData<ForgotPasswordUiState> getUiState() {
        return uiState;
    }

    public LiveData<Event<ForgotPasswordUiEvent>> getUiEvent() {
        return uiEvent;
    }

    public void sendResetEmail(String email) {
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            updateState(false, "Enter a valid email address.");
            uiEvent.setValue(new Event<>(ForgotPasswordUiEvent.toast("Enter a valid email address.")));
            return;
        }
        updateState(true, "");
        repository.forgotPasswordMobile(email, new AuthRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                String message = "OTP sent to " + email + ".";
                updateState(false, message);
                uiEvent.postValue(new Event<>(ForgotPasswordUiEvent.toast(message)));
                uiEvent.postValue(new Event<>(ForgotPasswordUiEvent.openOtp(email)));
            }

            @Override
            public void onError(String message) {
                updateState(false, message);
                uiEvent.postValue(new Event<>(ForgotPasswordUiEvent.toast(message)));
            }
        });
    }

    private void updateState(boolean loading, String message) {
        ForgotPasswordUiState current = uiState.getValue();
        ForgotPasswordUiState copy = current != null ? new ForgotPasswordUiState(current) : new ForgotPasswordUiState();
        copy.setLoading(loading);
        copy.setMessage(message);
        uiState.postValue(copy);
    }
}
