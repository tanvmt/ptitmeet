package com.ptithcm.ptitmeet.repository;

import android.content.Context;

import com.ptithcm.ptitmeet.api.SessionManager;
import com.ptithcm.ptitmeet.api.dto.common.ApiResponse;
import com.ptithcm.ptitmeet.api.dto.common.PageResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.CreateMeetingRequest;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingHistoryResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingResponse;
import com.ptithcm.ptitmeet.api.services.ApiService;
import com.ptithcm.ptitmeet.api.services.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainRepository {

    public interface DataCallback<T> {
        void onSuccess(T data);

        void onError(String message);
    }

    private final ApiService apiService;
    private final SessionManager sessionManager;

    public MainRepository(Context context) {
        Context appContext = context.getApplicationContext();
        this.apiService = RetrofitClient.getApiService(appContext);
        this.sessionManager = new SessionManager(appContext);
    }

    public String getUserName() {
        return sessionManager.getUserName();
    }

    public String getAvatarUrl() {
        return sessionManager.getAvatarUrl();
    }

    public void createInstantMeeting(CreateMeetingRequest request, DataCallback<MeetingResponse> callback) {
        apiService.createInstantMeeting(request).enqueue(new Callback<ApiResponse<MeetingResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<MeetingResponse>> call, Response<ApiResponse<MeetingResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(response.body() != null ? response.body().getMessage() : "Không thể tạo phòng");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MeetingResponse>> call, Throwable t) {
                callback.onError("Lỗi kết nối máy chủ");
            }
        });
    }

    public void getUpNextMeeting(DataCallback<MeetingHistoryResponse> callback) {
        apiService.getUpNextMeeting().enqueue(new Callback<ApiResponse<MeetingHistoryResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<MeetingHistoryResponse>> call, Response<ApiResponse<MeetingHistoryResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError("empty");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MeetingHistoryResponse>> call, Throwable t) {
                callback.onError("empty");
            }
        });
    }

    public void getRecentActivity(DataCallback<List<MeetingHistoryResponse>> callback) {
        apiService.getMeetingHistory(1, 10, "ALL", "ALL")
                .enqueue(new Callback<ApiResponse<PageResponse<MeetingHistoryResponse>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<PageResponse<MeetingHistoryResponse>>> call, Response<ApiResponse<PageResponse<MeetingHistoryResponse>>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            callback.onSuccess(response.body().getData().getContent());
                        } else {
                            callback.onError("empty");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<PageResponse<MeetingHistoryResponse>>> call, Throwable t) {
                        callback.onError("empty");
                    }
                });
    }
}
