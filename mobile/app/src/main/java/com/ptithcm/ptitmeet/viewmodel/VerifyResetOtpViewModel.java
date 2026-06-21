package com.ptithcm.ptitmeet.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.ptitmeet.api.dto.auth.VerifyResetOtpResponse;
import com.ptithcm.ptitmeet.repository.AuthRepository;

public class VerifyResetOtpViewModel extends AndroidViewModel {

    private final AuthRepository repository;
    private final MutableLiveData<VerifyResetOtpUiState> uiState = new MutableLiveData<>(new VerifyResetOtpUiState());
    private final MutableLiveData<Event<VerifyResetOtpUiEvent>> uiEvent = new MutableLiveData<>();

    public VerifyResetOtpViewModel(@NonNull Application application) {
        super(application);
        this.repository = new AuthRepository(application);
    }

    public LiveData<VerifyResetOtpUiState> getUiState() {
        return uiState;
    }

    public LiveData<Event<VerifyResetOtpUiEvent>> getUiEvent() {
        return uiEvent;
    }

    public void verifyOtp(String email, String otp) {
        if (email.trim().isEmpty()) {
            updateState(false, "Email is missing. Please request a new code.");
            return;
        }
        if (!otp.matches("\\d{6}")) {
            updateState(false, "Enter the 6-digit code from your email.");
            return;
        }
        updateState(true, "");
        repository.verifyResetOtp(email, otp, new AuthRepository.DataCallback<VerifyResetOtpResponse>() {
            @Override
            public void onSuccess(VerifyResetOtpResponse data) {
                updateState(false, "");
                uiEvent.postValue(new Event<>(VerifyResetOtpUiEvent.toast("Code verified")));
                uiEvent.postValue(new Event<>(VerifyResetOtpUiEvent.openReset(data.getResetToken())));
            }

            @Override
            public void onError(String message) {
                updateState(false, message);
            }
        });
    }

    private void updateState(boolean loading, String message) {
        VerifyResetOtpUiState current = uiState.getValue();
        VerifyResetOtpUiState copy = current != null ? new VerifyResetOtpUiState(current) : new VerifyResetOtpUiState();
        copy.setLoading(loading);
        copy.setMessage(message);
        uiState.postValue(copy);
    }
}
