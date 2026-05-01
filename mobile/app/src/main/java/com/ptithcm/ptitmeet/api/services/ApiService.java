package com.ptithcm.ptitmeet.api.services;

import com.ptithcm.ptitmeet.api.dto.ApiResponse;
import com.ptithcm.ptitmeet.api.dto.CreateMeetingRequest;
import com.ptithcm.ptitmeet.api.dto.JoinMeetingRequest;
import com.ptithcm.ptitmeet.api.dto.JoinMeetingResponse;
import com.ptithcm.ptitmeet.api.dto.LoginRequest;
import com.ptithcm.ptitmeet.api.dto.AuthResponse;
import com.ptithcm.ptitmeet.api.dto.MeetingInfoResponse;
import com.ptithcm.ptitmeet.api.dto.RegisterRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.*;

public interface ApiService {
    @POST("/api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);
    @POST("/api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);
    @POST("api/meetings/create")
    Call<ApiResponse<MeetingInfoResponse>> createMeeting(@Body CreateMeetingRequest request);
    @POST("/api/meetings/{meetingCode}/join")
    Call<ApiResponse<JoinMeetingResponse>> joinMeeting(
            @Path("meetingCode") String meetingCode,
            @Body JoinMeetingRequest request
    );
}