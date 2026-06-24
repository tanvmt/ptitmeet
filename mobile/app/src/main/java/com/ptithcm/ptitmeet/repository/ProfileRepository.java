package com.ptithcm.ptitmeet.repository;

import android.content.Context;
import android.net.Uri;

import com.ptithcm.ptitmeet.api.SessionManager;
import com.ptithcm.ptitmeet.api.dto.common.ApiResponse;
import com.ptithcm.ptitmeet.api.dto.user.UpdateProfileRequest;
import com.ptithcm.ptitmeet.api.dto.user.UserResponse;
import com.ptithcm.ptitmeet.api.services.ApiService;
import com.ptithcm.ptitmeet.api.services.RetrofitClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileRepository {

    public interface DataCallback<T> {
        void onSuccess(T data);

        void onError(String message);
    }

    private final Context context;
    private final ApiService apiService;
    private final SessionManager sessionManager;

    public ProfileRepository(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = RetrofitClient.getApiService(this.context);
        this.sessionManager = new SessionManager(this.context);
    }

    public void getProfile(DataCallback<UserResponse> callback) {
        apiService.getProfile().enqueue(new Callback<ApiResponse<UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserResponse>> call, Response<ApiResponse<UserResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(response.body() != null ? response.body().getMessage() : "Unable to load profile.");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {
                callback.onError("Cannot connect to server. Pull back later and try again.");
            }
        });
    }

    public void uploadAvatar(Uri uri, DataCallback<UserResponse> callback) {
        MultipartBody.Part avatarPart;
        try {
            avatarPart = createAvatarPart(uri);
        } catch (Exception exception) {
            callback.onError("Unable to read selected photo.");
            return;
        }

        apiService.uploadAvatar(avatarPart).enqueue(new Callback<ApiResponse<UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserResponse>> call, Response<ApiResponse<UserResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    UserResponse data = response.body().getData();
                    sessionManager.updateAvatarUrl(data.getAvatarUrl());
                    callback.onSuccess(data);
                } else {
                    callback.onError(response.body() != null ? response.body().getMessage() : "Unable to upload avatar.");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {
                callback.onError("Cannot connect to server. Check your network and try again.");
            }
        });
    }

    public void updateProfile(String fullName, String avatarUrl, DataCallback<UserResponse> callback) {
        apiService.updateProfile(new UpdateProfileRequest(fullName, avatarUrl))
                .enqueue(new Callback<ApiResponse<UserResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<UserResponse>> call, Response<ApiResponse<UserResponse>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            UserResponse data = response.body().getData();
                            sessionManager.updateUserName(data.getFullName());
                            sessionManager.updateAvatarUrl(data.getAvatarUrl());
                            callback.onSuccess(data);
                        } else {
                            callback.onError(response.body() != null ? response.body().getMessage() : "Unable to update profile.");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {
                        callback.onError("Cannot connect to server. Check your network and try again.");
                    }
                });
    }

    public void updateCachedUserName(String fullName) {
        sessionManager.updateUserName(fullName);
    }

    public void logout() {
        sessionManager.logout();
    }

    private MultipartBody.Part createAvatarPart(Uri uri) throws Exception {
        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType == null || !mimeType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            mimeType = "image/jpeg";
        }
        String extension = mimeType.endsWith("png") ? ".png" : ".jpg";
        File file = File.createTempFile("avatar_", extension, context.getCacheDir());

        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(file)) {
            if (inputStream == null) {
                throw new IllegalStateException("Selected photo cannot be opened");
            }
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        RequestBody body = RequestBody.create(file, MediaType.parse(mimeType));
        return MultipartBody.Part.createFormData("file", file.getName(), body);
    }
}
