package com.ptithcm.ptitmeet.api.services;

import com.ptithcm.ptitmeet.api.dto.auth.AuthEnvelope;
import com.ptithcm.ptitmeet.api.dto.auth.ForgotPasswordRequest;
import com.ptithcm.ptitmeet.api.dto.auth.GoogleLoginRequest;
import com.ptithcm.ptitmeet.api.dto.auth.LoginRequest;
import com.ptithcm.ptitmeet.api.dto.auth.RegisterRequest;
import com.ptithcm.ptitmeet.api.dto.auth.ResetPasswordRequest;
import com.ptithcm.ptitmeet.api.dto.auth.VerifyResetOtpRequest;
import com.ptithcm.ptitmeet.api.dto.auth.VerifyResetOtpResponse;
import com.ptithcm.ptitmeet.api.dto.chat.ChatMessageResponse;
import com.ptithcm.ptitmeet.api.dto.common.ApiResponse;
import com.ptithcm.ptitmeet.api.dto.common.PageResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.ApprovalRequest;
import com.ptithcm.ptitmeet.api.dto.meeting.CreateMeetingRequest;
import com.ptithcm.ptitmeet.api.dto.meeting.FeedbackRequest;
import com.ptithcm.ptitmeet.api.dto.meeting.JoinMeetingRequest;
import com.ptithcm.ptitmeet.api.dto.meeting.JoinMeetingResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingHistoryResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingInfoResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingSummaryResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.ParticipantResponse;
import com.ptithcm.ptitmeet.api.dto.recording.MeetingRecordingResponse;
import com.ptithcm.ptitmeet.api.dto.user.UpdateProfileRequest;
import com.ptithcm.ptitmeet.api.dto.user.UserResponse;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @POST("/api/auth/login")
    Call<AuthEnvelope> login(@Body LoginRequest request);

    @POST("/api/auth/register")
    Call<ApiResponse<UserResponse>> register(@Body RegisterRequest request);

    @POST("/api/auth/google")
    Call<AuthEnvelope> loginWithGoogle(@Body GoogleLoginRequest request);

    @POST("/api/auth/refresh-token")
    Call<AuthEnvelope> refreshToken();

    @POST("/api/auth/logout")
    Call<ApiResponse<Void>> logout();

    @POST("/api/auth/forgot-password")
    Call<ApiResponse<Void>> forgotPassword(@Body ForgotPasswordRequest request);

    @POST("/api/auth/forgot-password-mobile")
    Call<ApiResponse<Object>> forgotPasswordMobile(@Body ForgotPasswordRequest request);

    @POST("/api/auth/verify-reset-otp")
    Call<ApiResponse<VerifyResetOtpResponse>> verifyResetOtp(@Body VerifyResetOtpRequest request);

    @POST("/api/auth/reset-password")
    Call<ApiResponse<Void>> resetPassword(@Body ResetPasswordRequest request);

    @GET("/api/users/me")
    Call<ApiResponse<UserResponse>> getCurrentUser();

    @GET("/api/users/profile")
    Call<ApiResponse<UserResponse>> getProfile();

    @PUT("/api/users/profile")
    Call<ApiResponse<UserResponse>> updateProfile(@Body UpdateProfileRequest request);

    @Multipart
    @POST("/api/users/avatar")
    Call<ApiResponse<UserResponse>> uploadAvatar(@Part MultipartBody.Part file);

    @POST("/api/meetings/instant")
    Call<ApiResponse<MeetingResponse>> createInstantMeeting();

    @POST("/api/meetings/schedule")
    Call<ApiResponse<MeetingResponse>> scheduleMeeting(@Body CreateMeetingRequest request);

    @GET("/api/meetings/history")
    Call<ApiResponse<PageResponse<MeetingHistoryResponse>>> getMeetingHistory(
            @Query("page") int page,
            @Query("size") int size,
            @Query("role") String role,
            @Query("status") String status
    );

    @GET("/api/meetings/up-next")
    Call<ApiResponse<MeetingHistoryResponse>> getUpNextMeeting();

    @GET("/api/meetings/my-meetings")
    Call<ApiResponse<List<MeetingResponse>>> getMyMeetings();

    @GET("/api/meetings/{meetingCode}/info")
    Call<ApiResponse<MeetingInfoResponse>> getMeetingInfo(@Path("meetingCode") String meetingCode);

    @DELETE("/api/meetings/{meetingCode}")
    Call<ApiResponse<Void>> cancelMeeting(@Path("meetingCode") String meetingCode);

    @POST("/api/meetings/{meetingCode}/join")
    Call<ApiResponse<JoinMeetingResponse>> joinMeeting(
            @Path("meetingCode") String meetingCode,
            @Body JoinMeetingRequest request
    );

    @GET("/api/meetings/{meetingCode}/waiting-room")
    Call<ApiResponse<List<ParticipantResponse>>> getWaitingRoom(@Path("meetingCode") String meetingCode);

    @POST("/api/meetings/{meetingCode}/approval")
    Call<ApiResponse<Void>> approveParticipant(
            @Path("meetingCode") String meetingCode,
            @Body ApprovalRequest request
    );

    @GET("/api/meetings/{meetingCode}/chat/history")
    Call<ApiResponse<List<ChatMessageResponse>>> getChatHistory(@Path("meetingCode") String meetingCode);

    @POST("/api/meetings/{meetingCode}/leave")
    Call<ApiResponse<Void>> leaveMeeting(@Path("meetingCode") String meetingCode);

    @POST("/api/meetings/{meetingCode}/end")
    Call<ApiResponse<Void>> endMeeting(@Path("meetingCode") String meetingCode);

    @GET("/api/meetings/{meetingCode}/summary")
    Call<ApiResponse<MeetingSummaryResponse>> getMeetingSummary(
            @Path("meetingCode") String meetingCode,
            @Query("action") String action
    );

    @POST("/api/meetings/{meetingCode}/feedback")
    Call<ApiResponse<Void>> submitFeedback(
            @Path("meetingCode") String meetingCode,
            @Body FeedbackRequest request
    );

    @GET("/api/meetings/{meetingCode}/settings")
    Call<ApiResponse<String>> getMeetingSettings(@Path("meetingCode") String meetingCode);

    @PUT("/api/meetings/{meetingCode}/settings")
    Call<ApiResponse<MeetingResponse>> updateMeetingSettings(
            @Path("meetingCode") String meetingCode,
            @Body Map<String, Object> settings
    );

    @POST("/api/livekit/recordings/start")
    Call<ApiResponse<MeetingRecordingResponse>> startRecording(@Query("meetingCode") String meetingCode);

    @POST("/api/livekit/recordings/stop")
    Call<ApiResponse<Void>> stopRecording(@Query("egressId") String egressId);

    @GET("/api/livekit/recordings/my")
    Call<ApiResponse<List<MeetingRecordingResponse>>> getMyRecordings();
}
