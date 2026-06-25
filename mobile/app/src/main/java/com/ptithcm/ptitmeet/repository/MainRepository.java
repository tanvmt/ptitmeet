package com.ptithcm.ptitmeet.repository;

import android.content.Context;
import android.util.Log;

import com.ptithcm.ptitmeet.api.SessionManager;
import com.ptithcm.ptitmeet.api.dto.common.ApiResponse;
import com.ptithcm.ptitmeet.api.dto.common.PageResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingHistoryResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingResponse;
import com.ptithcm.ptitmeet.api.services.ApiService;
import com.ptithcm.ptitmeet.api.services.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainRepository {

    private static final String TAG = "MainRepository";

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

    public void createInstantMeeting(DataCallback<MeetingResponse> callback) {
        apiService.createInstantMeeting().enqueue(new Callback<ApiResponse<MeetingResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<MeetingResponse>> call, Response<ApiResponse<MeetingResponse>> response) {
                ApiResponse<MeetingResponse> body = response.body();
                MeetingResponse meeting = body != null ? body.getData() : null;
                if (response.isSuccessful() && meeting != null && meeting.hasMeetingCode()) {
                    callback.onSuccess(meeting);
                } else if (response.isSuccessful() && meeting != null) {
                    callback.onError("Server không trả mã phòng họp");
                } else {
                    callback.onError(body != null ? body.getMessage() : "Không thể tạo phòng");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MeetingResponse>> call, Throwable t) {
                Log.e(TAG, "createInstantMeeting failed", t);
                callback.onError("Không thể kết nối server");
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
