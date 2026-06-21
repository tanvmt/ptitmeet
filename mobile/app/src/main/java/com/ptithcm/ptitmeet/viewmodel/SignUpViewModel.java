package com.ptithcm.ptitmeet.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.ptitmeet.api.dto.user.UserResponse;
import com.ptithcm.ptitmeet.repository.AuthRepository;

public class SignUpViewModel extends AndroidViewModel {

    private final AuthRepository repository;
    private final MutableLiveData<SignUpUiState> uiState = new MutableLiveData<>(new SignUpUiState());
    private final MutableLiveData<Event<SignUpUiEvent>> uiEvent = new MutableLiveData<>();

    public SignUpViewModel(@NonNull Application application) {
        super(application);
        this.repository = new AuthRepository(application);
    }

    public LiveData<SignUpUiState> getUiState() {
        return uiState;
    }

    public LiveData<Event<SignUpUiEvent>> getUiEvent() {
        return uiEvent;
    }

    public void register(String fullName, String email, String password, String confirmPassword) {
        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            uiEvent.setValue(new Event<>(SignUpUiEvent.toast("Vui lòng nhập đầy đủ thông tin")));
            return;
        }
        if (!password.equals(confirmPassword)) {
            uiEvent.setValue(new Event<>(SignUpUiEvent.toast("Mật khẩu xác nhận không khớp")));
            return;
        }
        if (password.length() < 6) {
            uiEvent.setValue(new Event<>(SignUpUiEvent.toast("Mật khẩu phải có ít nhất 6 ký tự")));
            return;
        }

        updateState(true);
        repository.register(fullName, email, password, new AuthRepository.DataCallback<UserResponse>() {
            @Override
            public void onSuccess(UserResponse data) {
                updateState(false);
                uiEvent.postValue(new Event<>(SignUpUiEvent.toast("Đăng ký thành công! Vui lòng đăng nhập.")));
                uiEvent.postValue(new Event<>(SignUpUiEvent.navigateLogin(email)));
            }

            @Override
            public void onError(String message) {
                updateState(false);
                uiEvent.postValue(new Event<>(SignUpUiEvent.toast(message)));
            }
        });
    }

    private void updateState(boolean loading) {
        SignUpUiState current = uiState.getValue();
        SignUpUiState copy = current != null ? new SignUpUiState(current) : new SignUpUiState();
        copy.setLoading(loading);
        uiState.postValue(copy);
    }
}
