package com.ptithcm.ptitmeet.repository;

import android.content.Context;

import com.google.gson.Gson;
import com.ptithcm.ptitmeet.api.SessionManager;
import com.ptithcm.ptitmeet.api.dto.auth.AuthEnvelope;
import com.ptithcm.ptitmeet.api.dto.auth.AuthResponse;
import com.ptithcm.ptitmeet.api.dto.auth.ForgotPasswordRequest;
import com.ptithcm.ptitmeet.api.dto.auth.LoginRequest;
import com.ptithcm.ptitmeet.api.dto.auth.RegisterRequest;
import com.ptithcm.ptitmeet.api.dto.auth.ResetPasswordRequest;
import com.ptithcm.ptitmeet.api.dto.auth.VerifyResetOtpRequest;
import com.ptithcm.ptitmeet.api.dto.auth.VerifyResetOtpResponse;
import com.ptithcm.ptitmeet.api.dto.common.ApiResponse;
import com.ptithcm.ptitmeet.api.dto.user.UserResponse;
import com.ptithcm.ptitmeet.api.services.ApiService;
import com.ptithcm.ptitmeet.api.services.RetrofitClient;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    public interface DataCallback<T> {
        void onSuccess(T data);

        void onError(String message);
    }

    private final ApiService apiService;
    private final SessionManager sessionManager;
    private final Gson gson = new Gson();

    public AuthRepository(Context context) {
        Context appContext = context.getApplicationContext();
        this.apiService = RetrofitClient.getApiService(appContext);
        this.sessionManager = new SessionManager(appContext);
    }

    public void login(String email, String password, DataCallback<AuthResponse> callback) {
        apiService.login(new LoginRequest(email, password)).enqueue(new Callback<AuthEnvelope>() {
            @Override
            public void onResponse(Call<AuthEnvelope> call, Response<AuthEnvelope> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    AuthResponse authData = response.body().getData();
                    sessionManager.saveAuthData(
                            authData.getAccessToken(),
                            authData.getUser().getUserId(),
                            authData.getUser().getFullName()
                    );
                    callback.onSuccess(authData);
                } else {
                    callback.onError(response.body() != null ? response.body().getMessage() : "Sai tài khoản hoặc mật khẩu");
                }
            }

            @Override
            public void onFailure(Call<AuthEnvelope> call, Throwable t) {
                callback.onError("Fail to connect to server");
            }
        });
    }

    public void register(String fullName, String email, String password, DataCallback<UserResponse> callback) {
        apiService.register(new RegisterRequest(fullName, email, password))
                .enqueue(new Callback<ApiResponse<UserResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<UserResponse>> call, Response<ApiResponse<UserResponse>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            callback.onSuccess(response.body().getData());
                        } else {
                            callback.onError(response.body() != null ? response.body().getMessage() : "Đăng ký thất bại hoặc email đã tồn tại");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {
                        callback.onError("Không thể kết nối tới máy chủ");
                    }
                });
    }

    public void forgotPasswordMobile(String email, DataCallback<Void> callback) {
        apiService.forgotPasswordMobile(new ForgotPasswordRequest(email)).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError(extractErrorMessage(response, "Unable to send reset email."));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                callback.onError("Cannot connect to server. Check your network and try again.");
            }
        });
    }

    public void verifyResetOtp(String email, String otp, DataCallback<VerifyResetOtpResponse> callback) {
        apiService.verifyResetOtp(new VerifyResetOtpRequest(email, otp))
                .enqueue(new Callback<ApiResponse<VerifyResetOtpResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<VerifyResetOtpResponse>> call, Response<ApiResponse<VerifyResetOtpResponse>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().getData() != null
                                && response.body().getData().getResetToken() != null) {
                            callback.onSuccess(response.body().getData());
                        } else {
                            callback.onError(extractErrorMessage(response, "Invalid or expired code."));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<VerifyResetOtpResponse>> call, Throwable t) {
                        callback.onError("Cannot connect to server. Check your network and try again.");
                    }
                });
    }

    public void resetPassword(String token, String password, DataCallback<Void> callback) {
        apiService.resetPassword(new ResetPasswordRequest(token, password)).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError(extractErrorMessage(response, "Unable to update password."));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                callback.onError("Cannot connect to server. Check your network and try again.");
            }
        });
    }

    private String extractErrorMessage(Response<?> response, String fallback) {
        if (response.body() instanceof ApiResponse) {
            String message = ((ApiResponse<?>) response.body()).getMessage();
            if (message != null && !message.trim().isEmpty()) {
                return message;
            }
        }

        ResponseBody errorBody = response.errorBody();
        if (errorBody == null) {
            return fallback;
        }

        try {
            ApiResponse<?> apiResponse = gson.fromJson(errorBody.string(), ApiResponse.class);
            if (apiResponse != null && apiResponse.getMessage() != null && !apiResponse.getMessage().trim().isEmpty()) {
                return apiResponse.getMessage();
            }
        } catch (IOException ignored) {
        } catch (RuntimeException ignored) {
        }
        return fallback;
    }
}
