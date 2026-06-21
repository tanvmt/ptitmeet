package com.ptithcm.ptitmeet.repository;

import android.content.Context;

import com.ptithcm.ptitmeet.api.SessionManager;
import com.ptithcm.ptitmeet.api.dto.common.ApiResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.FeedbackRequest;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingSummaryResponse;
import com.ptithcm.ptitmeet.api.services.ApiService;
import com.ptithcm.ptitmeet.api.services.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SummaryRepository {

    public interface DataCallback<T> {
        void onSuccess(T data);

        void onError(String message);
    }

    private final ApiService apiService;
    private final SessionManager sessionManager;

    public SummaryRepository(Context context) {
        Context appContext = context.getApplicationContext();
        this.apiService = RetrofitClient.getApiService(appContext);
        this.sessionManager = new SessionManager(appContext);
    }

    public String getUserName() {
        return sessionManager.getUserName();
    }

    public void getMeetingSummary(String meetingCode, String actionTaken, DataCallback<MeetingSummaryResponse> callback) {
        apiService.getMeetingSummary(meetingCode, actionTaken).enqueue(new Callback<ApiResponse<MeetingSummaryResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<MeetingSummaryResponse>> call, Response<ApiResponse<MeetingSummaryResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError("summary_unavailable");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MeetingSummaryResponse>> call, Throwable t) {
                callback.onError("summary_unavailable");
            }
        });
    }

    public void submitFeedback(String meetingCode, int rating, DataCallback<Void> callback) {
        apiService.submitFeedback(meetingCode, new FeedbackRequest(rating)).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError("Unable to submit feedback");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                callback.onError("Connection error, feedback not submitted");
            }
        });
    }
}
