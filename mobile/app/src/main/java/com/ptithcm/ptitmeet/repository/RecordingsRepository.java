package com.ptithcm.ptitmeet.repository;

import android.content.Context;

import com.ptithcm.ptitmeet.api.dto.common.ApiResponse;
import com.ptithcm.ptitmeet.api.dto.recording.MeetingRecordingResponse;
import com.ptithcm.ptitmeet.api.services.ApiService;
import com.ptithcm.ptitmeet.api.services.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecordingsRepository {

    public interface DataCallback<T> {
        void onSuccess(T data);

        void onError(String message);
    }

    private final ApiService apiService;

    public RecordingsRepository(Context context) {
        this.apiService = RetrofitClient.getApiService(context.getApplicationContext());
    }

    public void getMyRecordings(DataCallback<List<MeetingRecordingResponse>> callback) {
        apiService.getMyRecordings().enqueue(new Callback<ApiResponse<List<MeetingRecordingResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<MeetingRecordingResponse>>> call, Response<ApiResponse<List<MeetingRecordingResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(response.body() != null ? response.body().getMessage() : "Unable to load recordings.");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<MeetingRecordingResponse>>> call, Throwable t) {
                callback.onError("Cannot connect to server. Check your network and try again.");
            }
        });
    }
}
