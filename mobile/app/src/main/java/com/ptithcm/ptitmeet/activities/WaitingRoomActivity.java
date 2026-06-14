package com.ptithcm.ptitmeet.activities;

import android.Manifest;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Handler;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.api.SessionManager;
import com.ptithcm.ptitmeet.api.dto.common.ApiResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.JoinMeetingRequest;
import com.ptithcm.ptitmeet.api.dto.meeting.JoinMeetingResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingInfoResponse;
import com.ptithcm.ptitmeet.api.realtime.MeetingRealtimeClient;
import com.ptithcm.ptitmeet.api.realtime.StompSocketClient;
import com.ptithcm.ptitmeet.api.services.ApiService;
import com.ptithcm.ptitmeet.api.services.RetrofitClient;
import com.ptithcm.ptitmeet.utils.DevicePermissionHelper;

import org.json.JSONObject;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WaitingRoomActivity extends AppCompatActivity {

    private boolean isMicOn = true;
    private boolean isVideoOn = true;

    private FloatingActionButton fabToggleMic;
    private FloatingActionButton fabToggleVideo;
    private FrameLayout videoPreviewContainer;
    private MaterialButton btnJoin;
    private MaterialButton btnCancelWaiting;
    private TextView tvReady;
    private TextView tvParticipantsCount;
    private TextView tvDisplayName;
    private TextView tvWaitingMessage;
    private TextView tvPollingStatus;

    private ApiService apiService;
    private SessionManager sessionManager;
    private String meetingCode;
    private String displayName;
    private String waitingMessage;
    private boolean isWaitingState;
    private boolean isPollingActive;
    private boolean isJoinRequestInFlight;
    private MeetingRealtimeClient meetingRealtimeClient;
    private String userRealtimeSubscriptionId;
    private String waitingRoomRealtimeSubscriptionId;
    private boolean realtimeConnected;
    private String pendingPermissionRequest;
    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            this::handlePermissionResult
    );
    private final Handler pollingHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isPollingActive || !isWaitingState || isFinishing()) {
                return;
            }
            if (!realtimeConnected) {
                requestJoinMeeting(true);
            }
            if (isPollingActive && isWaitingState) {
                pollingHandler.postDelayed(this, 10000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_room);

        apiService = RetrofitClient.getApiService(this);
        sessionManager = new SessionManager(this);
        meetingCode = getIntent().getStringExtra("MEETING_CODE");
        displayName = getIntent().getStringExtra("DISPLAY_NAME");
        waitingMessage = getIntent().getStringExtra("WAITING_MESSAGE");
        meetingRealtimeClient = new MeetingRealtimeClient(this, meetingCode);
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = sessionManager.getUserName();
        }

        fabToggleMic = findViewById(R.id.fabToggleMic);
        fabToggleVideo = findViewById(R.id.fabToggleVideo);
        videoPreviewContainer = findViewById(R.id.videoPreviewContainer);
        btnJoin = findViewById(R.id.btnJoin);
        btnCancelWaiting = findViewById(R.id.btnCancelWaiting);
        tvReady = findViewById(R.id.tvReady);
        tvParticipantsCount = findViewById(R.id.tvParticipantsCount);
        tvDisplayName = findViewById(R.id.tvDisplayName);
        tvWaitingMessage = findViewById(R.id.tvWaitingMessage);
        tvPollingStatus = findViewById(R.id.tvPollingStatus);

        tvDisplayName.setText(displayName);
        if (waitingMessage != null && !waitingMessage.trim().isEmpty()) {
            showWaitingState(waitingMessage);
        } else {
            hideWaitingState();
        }

        fabToggleMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isMicOn && !DevicePermissionHelper.hasMicrophonePermission(WaitingRoomActivity.this)) {
                    pendingPermissionRequest = DevicePermissionHelper.REQUEST_MIC;
                    permissionLauncher.launch(new String[]{Manifest.permission.RECORD_AUDIO});
                    return;
                }
                isMicOn = !isMicOn;
                updatePreviewControlsUi();
            }
        });

        fabToggleVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isVideoOn && !DevicePermissionHelper.hasCameraPermission(WaitingRoomActivity.this)) {
                    pendingPermissionRequest = DevicePermissionHelper.REQUEST_CAMERA;
                    permissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
                    return;
                }
                isVideoOn = !isVideoOn;
                updatePreviewControlsUi();
            }
        });

        btnJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestJoinMeeting(false);
            }
        });
        btnCancelWaiting.setOnClickListener(v -> cancelWaitingAndExit());

        loadMeetingInfo();
        updatePreviewControlsUi();
    }

    @Override
    protected void onStart() {
        super.onStart();
        connectRealtime();
        if (isWaitingState) {
            startPolling();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopPolling();
        disconnectRealtime();
    }

    private void loadMeetingInfo() {
        if (meetingCode == null || meetingCode.trim().isEmpty()) {
            tvReady.setText("Missing meeting code");
            tvParticipantsCount.setText("Khong the tai thong tin phong.");
            btnJoin.setEnabled(false);
            return;
        }

        apiService.getMeetingInfo(meetingCode).enqueue(new Callback<ApiResponse<MeetingInfoResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<MeetingInfoResponse>> call, Response<ApiResponse<MeetingInfoResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    MeetingInfoResponse info = response.body().getData();
                    tvReady.setText(info.getTitle());
                    tvParticipantsCount.setText("Host: " + info.getHostName() + " • Status: " + info.getStatus());
                } else {
                    tvParticipantsCount.setText("Khong tai duoc thong tin phong.");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MeetingInfoResponse>> call, Throwable t) {
                tvParticipantsCount.setText("Khong tai duoc thong tin phong.");
            }
        });
    }

    private void requestJoinMeeting(boolean fromPolling) {
        if (meetingCode == null || meetingCode.trim().isEmpty() || isJoinRequestInFlight) {
            return;
        }

        isJoinRequestInFlight = true;
        btnJoin.setEnabled(false);
        btnJoin.setText(isWaitingState ? "Checking..." : "Joining...");
        if (fromPolling) {
            tvPollingStatus.setText("Dang kiem tra host da duyet ban chua...");
        }

        apiService.joinMeeting(meetingCode, new JoinMeetingRequest(null, displayName))
                .enqueue(new Callback<ApiResponse<JoinMeetingResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<JoinMeetingResponse>> call, Response<ApiResponse<JoinMeetingResponse>> response) {
                        isJoinRequestInFlight = false;
                        btnJoin.setEnabled(true);

                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            JoinMeetingResponse joinData = response.body().getData();
                            if ("PENDING".equalsIgnoreCase(joinData.getStatus())) {
                                showWaitingState(joinData.getMessage());
                                return;
                            }
                            if ("REJECTED".equalsIgnoreCase(joinData.getStatus())) {
                                Toast.makeText(WaitingRoomActivity.this, joinData.getMessage(), Toast.LENGTH_SHORT).show();
                                hideWaitingState();
                                return;
                            }
                            stopPolling();
                            openMeetingRoom(joinData);
                            return;
                        }

                        btnJoin.setText(isWaitingState ? "Check Again" : "Join Now");
                        if (isWaitingState) {
                            tvPollingStatus.setText("Van dang cho duyet. App se tiep tuc kiem tra lai.");
                        }
                        Toast.makeText(WaitingRoomActivity.this, response.body() != null ? response.body().getMessage() : "Khong the tham gia phong", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<JoinMeetingResponse>> call, Throwable t) {
                        isJoinRequestInFlight = false;
                        btnJoin.setEnabled(true);
                        btnJoin.setText(isWaitingState ? "Check Again" : "Join Now");
                        if (isWaitingState) {
                            tvPollingStatus.setText("Tam thoi khong kiem tra duoc. App se thu lai sau.");
                        } else {
                            Toast.makeText(WaitingRoomActivity.this, "Khong the ket noi may chu", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void openMeetingRoom(JoinMeetingResponse joinData) {
        Intent intent = new Intent(this, MeetingActivity.class);
        intent.putExtra("LIVEKIT_TOKEN", joinData.getToken());
        intent.putExtra("LIVEKIT_URL", joinData.getServerUrl());
        intent.putExtra("USER_ROLE", joinData.getRole());
        intent.putExtra("MEETING_CODE", meetingCode);
        intent.putExtra("IS_OWNER", joinData.isOwner());
        startActivity(intent);
        finish();
    }

    private void showWaitingState(String message) {
        isWaitingState = true;
        tvWaitingMessage.setVisibility(View.VISIBLE);
        tvPollingStatus.setVisibility(View.VISIBLE);
        btnCancelWaiting.setVisibility(View.VISIBLE);
        tvWaitingMessage.setText(message == null || message.trim().isEmpty()
                ? "Dang cho host duyet vao phong..."
                : message);
        btnJoin.setText("Check Again");
        tvPollingStatus.setText(realtimeConnected
                ? "Dang nghe phe duyet realtime. Polling van duoc giu lam du phong."
                : "App se tu dong kiem tra lai sau moi 10 giay.");
        startPolling();
    }

    private void hideWaitingState() {
        isWaitingState = false;
        stopPolling();
        tvWaitingMessage.setVisibility(View.GONE);
        tvPollingStatus.setVisibility(View.GONE);
        btnCancelWaiting.setVisibility(View.GONE);
        btnJoin.setText("Join Now");
    }

    private void startPolling() {
        if (isPollingActive) {
            return;
        }
        isPollingActive = true;
        pollingHandler.removeCallbacks(pollingRunnable);
        pollingHandler.postDelayed(pollingRunnable, 10000);
    }

    private void stopPolling() {
        isPollingActive = false;
        pollingHandler.removeCallbacks(pollingRunnable);
    }

    private void cancelWaitingAndExit() {
        stopPolling();
        Toast.makeText(this, "Da dung cho duyet", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void updatePreviewControlsUi() {
        if (isMicOn) {
            fabToggleMic.setImageResource(R.drawable.ic_mic);
            fabToggleMic.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")));
        } else {
            fabToggleMic.setImageResource(R.drawable.ic_mic);
            fabToggleMic.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E53935")));
        }

        if (isVideoOn) {
            fabToggleVideo.setImageResource(R.drawable.ic_videocam);
            fabToggleVideo.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#137fec")));
            videoPreviewContainer.setVisibility(View.VISIBLE);
        } else {
            fabToggleVideo.setImageResource(R.drawable.ic_videocam);
            fabToggleVideo.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E53935")));
            videoPreviewContainer.setVisibility(View.GONE);
        }
    }

    private void handlePermissionResult(Map<String, Boolean> permissions) {
        if (DevicePermissionHelper.REQUEST_MIC.equals(pendingPermissionRequest)) {
            boolean granted = Boolean.TRUE.equals(permissions.get(Manifest.permission.RECORD_AUDIO));
            if (granted) {
                isMicOn = true;
                updatePreviewControlsUi();
            } else {
                Toast.makeText(this, "Can quyen microphone de bat mic", Toast.LENGTH_SHORT).show();
            }
        } else if (DevicePermissionHelper.REQUEST_CAMERA.equals(pendingPermissionRequest)) {
            boolean granted = Boolean.TRUE.equals(permissions.get(Manifest.permission.CAMERA));
            if (granted) {
                isVideoOn = true;
                updatePreviewControlsUi();
            } else {
                Toast.makeText(this, "Can quyen camera de bat video", Toast.LENGTH_SHORT).show();
            }
        }
        pendingPermissionRequest = null;
    }

    private void connectRealtime() {
        if (meetingRealtimeClient == null || meetingRealtimeClient.isConnected()) {
            return;
        }

        meetingRealtimeClient.connect(new StompSocketClient.ConnectionListener() {
            @Override
            public void onConnected() {
                realtimeConnected = true;
                subscribeRealtimeTopics();
                if (isWaitingState) {
                    tvPollingStatus.setText("Dang nghe phe duyet realtime. Polling van duoc giu lam du phong.");
                }
            }

            @Override
            public void onError(String message) {
                realtimeConnected = false;
                if (isWaitingState) {
                    tvPollingStatus.setText("Socket tam thoi gian doan. App se tiep tuc polling.");
                }
            }

            @Override
            public void onDisconnected() {
                realtimeConnected = false;
                if (isWaitingState) {
                    tvPollingStatus.setText("Socket da ngat. App se tiep tuc polling.");
                }
            }
        });
    }

    private void subscribeRealtimeTopics() {
        String userId = sessionManager.getUserId();
        if (userId != null && userRealtimeSubscriptionId == null) {
            userRealtimeSubscriptionId = meetingRealtimeClient.subscribeToUserUpdates(userId, (destination, body) -> handleRealtimeUserMessage(body));
        }
        if (waitingRoomRealtimeSubscriptionId == null) {
            waitingRoomRealtimeSubscriptionId = meetingRealtimeClient.subscribeToWaitingRoom((destination, body) -> handleWaitingRoomRealtime(body));
        }
    }

    private void disconnectRealtime() {
        realtimeConnected = false;
        userRealtimeSubscriptionId = null;
        waitingRoomRealtimeSubscriptionId = null;
        if (meetingRealtimeClient != null) {
            meetingRealtimeClient.disconnect();
        }
    }

    private void handleRealtimeUserMessage(String body) {
        try {
            JSONObject jsonObject = new JSONObject(body);
            String status = jsonObject.optString("status");
            if ("APPROVED".equalsIgnoreCase(status)) {
                JoinMeetingResponse joinMeetingResponse = new JoinMeetingResponseParser().parse(jsonObject);
                stopPolling();
                openMeetingRoom(joinMeetingResponse);
                return;
            }
            if ("REJECTED".equalsIgnoreCase(status)) {
                String message = jsonObject.optString("message", "Yeu cau tham gia da bi tu choi.");
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                hideWaitingState();
            }
        } catch (Exception ignored) {
        }
    }

    private void handleWaitingRoomRealtime(String body) {
        if ("HOST_JOINED".equalsIgnoreCase(body)) {
            showWaitingState("Host da vao phong. Vui long doi duyet.");
            requestJoinMeeting(false);
            return;
        }
        if ("SETTINGS_CHANGED".equalsIgnoreCase(body)) {
            requestJoinMeeting(false);
        }
    }

    private static class JoinMeetingResponseParser {
        private JoinMeetingResponse parse(JSONObject jsonObject) {
            return new JoinMeetingResponse(
                    jsonObject.optString("token", null),
                    jsonObject.optString("serverUrl", null),
                    jsonObject.optString("status", null),
                    jsonObject.optString("role", null),
                    jsonObject.optString("message", null),
                    jsonObject.optString("settings", null),
                    jsonObject.optBoolean("isOwner", false),
                    jsonObject.optString("currentHostId", null)
            );
        }
    }
}
