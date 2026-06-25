package com.ptithcm.ptitmeet.activities;

import android.Manifest;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.core.Preview;
import androidx.camera.core.CameraSelector;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.api.dto.meeting.JoinMeetingResponse;
import com.ptithcm.ptitmeet.utils.DevicePermissionHelper;
import com.ptithcm.ptitmeet.viewmodel.WaitingRoomUiEvent;
import com.ptithcm.ptitmeet.viewmodel.WaitingRoomUiState;
import com.ptithcm.ptitmeet.viewmodel.WaitingRoomViewModel;

import java.util.Map;

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

    private WaitingRoomViewModel viewModel;
    private String meetingCode;
    private String displayName;
    private String waitingMessage;
    private String pendingPermissionRequest;

    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private boolean isHostSetup;

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            this::handlePermissionResult
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_room);

        meetingCode = getIntent().getStringExtra("MEETING_CODE");
        displayName = getIntent().getStringExtra("DISPLAY_NAME");
        waitingMessage = getIntent().getStringExtra("WAITING_MESSAGE");
        isHostSetup = getIntent().getBooleanExtra("IS_HOST_SETUP", false);
        viewModel = new ViewModelProvider(
                this,
                new WaitingRoomViewModel.Factory(getApplication(), meetingCode, displayName, waitingMessage, isHostSetup)
        ).get(WaitingRoomViewModel.class);
        displayName = viewModel.getDisplayName();

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

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                WaitingRoomUiState state = viewModel.getUiState().getValue();
                if (state != null && state.isWaitingState()) {
                    cancelWaitingAndExit();
                } else {
                    finish();
                }
            });
        }



        previewView = new PreviewView(this);
        videoPreviewContainer.addView(previewView);

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

        observeViewModel();
        loadMeetingInfo();
        updatePreviewControlsUi();
    }

    @Override
    protected void onStart() {
        super.onStart();
        connectRealtime();
        WaitingRoomUiState state = viewModel.getUiState().getValue();
        if (state != null && state.isWaitingState()) {
            viewModel.startPolling();
        }
        if (isVideoOn && DevicePermissionHelper.hasCameraPermission(this)) {
            startCameraPreview();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        viewModel.stopPolling();
        disconnectRealtime();
        stopCameraPreview();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCameraPreview();
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(this, this::applyWaitingRoomState);
        viewModel.getUiEvent().observe(this, event -> {
            if (event == null) {
                return;
            }
            WaitingRoomUiEvent uiEvent = event.getContentIfNotHandled();
            if (uiEvent == null) {
                return;
            }
            handleWaitingRoomEvent(uiEvent);
        });
    }

    private void applyWaitingRoomState(WaitingRoomUiState state) {
        if (state == null) {
            return;
        }
        tvReady.setText(state.getReadyTitle());
        tvParticipantsCount.setText(state.getMeetingDetails());
        btnJoin.setEnabled(state.isJoinButtonEnabled());
        btnJoin.setText(state.getJoinButtonText());
        if (state.isWaitingState()) {
            tvWaitingMessage.setVisibility(View.VISIBLE);
            tvPollingStatus.setVisibility(View.GONE);
            btnCancelWaiting.setVisibility(View.VISIBLE);
            tvWaitingMessage.setText(state.getWaitingMessage());
            viewModel.startPolling();
        } else {
            tvWaitingMessage.setVisibility(View.GONE);
            tvPollingStatus.setVisibility(View.GONE);
            btnCancelWaiting.setVisibility(View.GONE);
            viewModel.stopPolling();
        }
    }

    private void handleWaitingRoomEvent(WaitingRoomUiEvent event) {
        switch (event.getType()) {
            case WaitingRoomUiEvent.SHOW_TOAST:
                Toast.makeText(this, event.getMessage(), Toast.LENGTH_SHORT).show();
                break;
            case WaitingRoomUiEvent.OPEN_MEETING_ROOM:
                openMeetingRoom(event.getJoinMeetingResponse());
                break;
            case WaitingRoomUiEvent.FINISH:
                finish();
                break;
            default:
                break;
        }
    }

    private void loadMeetingInfo() {
        if (meetingCode == null || meetingCode.trim().isEmpty()) {
            tvReady.setText("Missing meeting code");
            tvParticipantsCount.setText("Unable to load meeting info.");
            btnJoin.setEnabled(false);
            return;
        }
        viewModel.loadMeetingInfo();
    }

    private void requestJoinMeeting(boolean fromPolling) {
        viewModel.requestJoin();
    }

    private void openMeetingRoom(JoinMeetingResponse joinData) {
        Intent intent = new Intent(this, MeetingActivity.class);
        intent.putExtra("LIVEKIT_TOKEN", joinData.getToken());
        intent.putExtra("LIVEKIT_URL", joinData.getServerUrl());
        intent.putExtra("USER_ROLE", joinData.getRole());
        intent.putExtra("MEETING_CODE", meetingCode);
        intent.putExtra("IS_OWNER", joinData.isOwner());

        boolean micOn = isMicOn;
        boolean videoOn = isVideoOn;
        boolean isHost = "HOST".equalsIgnoreCase(joinData.getRole()) || joinData.isOwner();
        if (!isHost && joinData.getSettings() != null) {
            try {
                org.json.JSONObject settingsObj = new org.json.JSONObject(joinData.getSettings());
                if (settingsObj.optBoolean("muteAudioOnEntry", settingsObj.optBoolean("muteOnEntry", false))) {
                    micOn = false;
                }
                if (settingsObj.optBoolean("muteVideoOnEntry", settingsObj.optBoolean("cameraOffOnEntry", false))) {
                    videoOn = false;
                }
            } catch (Exception ignored) {}
        }

        intent.putExtra("MIC_ON", micOn);
        intent.putExtra("VIDEO_ON", videoOn);
        startActivity(intent);
        finish();
    }

    private void cancelWaitingAndExit() {
        viewModel.cancelWaiting();
    }

    @Override
    public void onBackPressed() {
        WaitingRoomUiState state = viewModel.getUiState().getValue();
        if (state != null && state.isWaitingState()) {
            cancelWaitingAndExit();
        } else {
            super.onBackPressed();
        }
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
            startCameraPreview();
        } else {
            fabToggleVideo.setImageResource(R.drawable.ic_videocam);
            fabToggleVideo.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E53935")));
            videoPreviewContainer.setVisibility(View.GONE);
            stopCameraPreview();
        }
    }

    private void startCameraPreview() {
        if (!DevicePermissionHelper.hasCameraPermission(this)) {
            return;
        }
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        cameraProvider.bindToLifecycle(this, cameraSelector, preview);
    }

    private void stopCameraPreview() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                future.get().unbindAll();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void handlePermissionResult(Map<String, Boolean> permissions) {
        if (DevicePermissionHelper.REQUEST_MIC.equals(pendingPermissionRequest)) {
            boolean granted = Boolean.TRUE.equals(permissions.get(Manifest.permission.RECORD_AUDIO));
            if (granted) {
                isMicOn = true;
                updatePreviewControlsUi();
            } else {
                Toast.makeText(this, "Microphone permission is required to enable mic", Toast.LENGTH_SHORT).show();
            }
        } else if (DevicePermissionHelper.REQUEST_CAMERA.equals(pendingPermissionRequest)) {
            boolean granted = Boolean.TRUE.equals(permissions.get(Manifest.permission.CAMERA));
            if (granted) {
                isVideoOn = true;
                updatePreviewControlsUi();
            } else {
                Toast.makeText(this, "Camera permission is required to enable video", Toast.LENGTH_SHORT).show();
            }
        }
        pendingPermissionRequest = null;
    }

    private void connectRealtime() {
        viewModel.connectRealtime();
    }

    private void disconnectRealtime() {
        viewModel.disconnectRealtime();
    }
}
