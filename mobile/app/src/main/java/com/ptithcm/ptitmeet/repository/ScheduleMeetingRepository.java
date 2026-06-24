package com.ptithcm.ptitmeet.repository;

import android.content.Context;

import com.ptithcm.ptitmeet.api.SessionManager;
import com.ptithcm.ptitmeet.api.dto.common.ApiResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.CreateMeetingRequest;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingResponse;
import com.ptithcm.ptitmeet.api.services.ApiService;
import com.ptithcm.ptitmeet.api.services.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScheduleMeetingRepository {

    public interface DataCallback<T> {
        void onSuccess(T data);

        void onError(String message);
    }

    private final ApiService apiService;
    private final SessionManager sessionManager;

    public ScheduleMeetingRepository(Context context) {
        Context appContext = context.getApplicationContext();
        this.apiService = RetrofitClient.getApiService(appContext);
        this.sessionManager = new SessionManager(appContext);
    }

    public String getUserName() {
        return sessionManager.getUserName();
    }

    public void scheduleMeeting(CreateMeetingRequest request, DataCallback<MeetingResponse> callback) {
        apiService.scheduleMeeting(request).enqueue(new Callback<ApiResponse<MeetingResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<MeetingResponse>> call, Response<ApiResponse<MeetingResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(response.body() != null && response.body().getMessage() != null
                            ? response.body().getMessage()
                            : "Không thể lên lịch cuộc họp");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MeetingResponse>> call, Throwable t) {
                callback.onError("Lỗi kết nối máy chủ");
            }
        });
    }
}
