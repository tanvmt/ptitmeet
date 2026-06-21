package com.ptithcm.ptitmeet.repository;

import android.content.Context;

import com.ptithcm.ptitmeet.api.SessionManager;
import com.ptithcm.ptitmeet.api.dto.chat.ChatMessageResponse;
import com.ptithcm.ptitmeet.api.dto.common.ApiResponse;
import com.ptithcm.ptitmeet.api.dto.common.PageResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.JoinMeetingRequest;
import com.ptithcm.ptitmeet.api.dto.meeting.JoinMeetingResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingHistoryResponse;
import com.ptithcm.ptitmeet.api.services.ApiService;
import com.ptithcm.ptitmeet.api.services.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MeetingsRepository {

    public interface DataCallback<T> {
        void onSuccess(T data);

        void onError(String message);
    }

    private final ApiService apiService;
    private final SessionManager sessionManager;

    public MeetingsRepository(Context context) {
        Context appContext = context.getApplicationContext();
        this.apiService = RetrofitClient.getApiService(appContext);
        this.sessionManager = new SessionManager(appContext);
    }

    public String getUserName() {
        return sessionManager.getUserName();
    }

    public void getMeetingHistory(int page, int size, String role, String status, DataCallback<PageResponse<MeetingHistoryResponse>> callback) {
        apiService.getMeetingHistory(page, size, role, status).enqueue(new Callback<ApiResponse<PageResponse<MeetingHistoryResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<PageResponse<MeetingHistoryResponse>>> call, Response<ApiResponse<PageResponse<MeetingHistoryResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError("Không thể tải danh sách, vui lòng thử lại");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<PageResponse<MeetingHistoryResponse>>> call, Throwable t) {
                callback.onError("Lỗi kết nối máy chủ");
            }
        });
    }

    public void joinMeeting(String meetingCode, DataCallback<JoinMeetingResponse> callback) {
        apiService.joinMeeting(meetingCode, new JoinMeetingRequest(null, getUserName()))
                .enqueue(new Callback<ApiResponse<JoinMeetingResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<JoinMeetingResponse>> call, Response<ApiResponse<JoinMeetingResponse>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            callback.onSuccess(response.body().getData());
                        } else {
                            callback.onError(response.body() != null ? response.body().getMessage() : "Lỗi không xác định");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<JoinMeetingResponse>> call, Throwable t) {
                        callback.onError("Lỗi kết nối mạng");
                    }
                });
    }

    public void cancelMeeting(String meetingCode, DataCallback<Void> callback) {
        apiService.cancelMeeting(meetingCode).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError(response.body() != null ? response.body().getMessage() : "Không thể hủy cuộc họp");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                callback.onError("Lỗi kết nối");
            }
        });
    }

    public void getChatHistory(String meetingCode, DataCallback<List<ChatMessageResponse>> callback) {
        apiService.getChatHistory(meetingCode).enqueue(new Callback<ApiResponse<List<ChatMessageResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ChatMessageResponse>>> call, Response<ApiResponse<List<ChatMessageResponse>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError("Không thể tải lịch sử chat");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<ChatMessageResponse>>> call, Throwable t) {
                callback.onError("Lỗi mạng, vui lòng thử lại");
            }
        });
    }
}
