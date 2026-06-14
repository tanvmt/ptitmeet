package com.ptithcm.ptitmeet.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Handler;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.ptithcm.ptitmeet.adapters.ChatMessageAdapter;
import com.ptithcm.ptitmeet.ParticipantAdapter;
import com.ptithcm.ptitmeet.ParticipantData;
import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.api.SessionManager;
import com.ptithcm.ptitmeet.api.dto.chat.ChatMessageResponse;
import com.ptithcm.ptitmeet.api.dto.common.ApiResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.ApprovalRequest;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingInfoResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.ParticipantResponse;
import com.ptithcm.ptitmeet.api.dto.recording.MeetingRecordingResponse;
import com.ptithcm.ptitmeet.api.realtime.MeetingRealtimeClient;
import com.ptithcm.ptitmeet.api.realtime.StompSocketClient;
import com.ptithcm.ptitmeet.api.services.ApiService;
import com.ptithcm.ptitmeet.api.services.RetrofitClient;
import com.ptithcm.ptitmeet.live.LiveKitRoomManager;
import com.ptithcm.ptitmeet.live.LiveParticipantState;
import com.ptithcm.ptitmeet.utils.DevicePermissionHelper;
import com.ptithcm.ptitmeet.utils.SystemActionHelper;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MeetingActivity extends AppCompatActivity {

    private RecyclerView rvParticipants;
    private ParticipantAdapter participantAdapter;
    private final List<ParticipantData> participantList = new ArrayList<>();

    private View badgeWaiting;
    private TextView tvMeetingCode;
    private View btnLeave;
    private ImageButton btnMic;
    private ImageButton btnVideo;
    private ImageButton btnParticipants;
    private ImageButton btnChat;
    private ImageButton btnSwitchCamera;
    private ImageButton btnRecord;
    private TextView tvRecordingStatus;

    private ApiService apiService;
    private SessionManager sessionManager;
    private String meetingCode;
    private String userRole;
    private String livekitUrl;
    private boolean isMeetingOwner;
    private boolean recordingActive;
    private boolean recordingRequestInFlight;
    private String recordingEgressId;
    private final List<ParticipantResponse> waitingParticipants = new ArrayList<>();
    private MeetingRealtimeClient meetingRealtimeClient;
    private String adminSubscriptionId;
    private String waitingRoomSubscriptionId;
    private String systemSubscriptionId;
    private String chatSubscriptionId;
    private final List<ChatMessageResponse> chatMessages = new ArrayList<>();
    private ChatMessageAdapter chatMessageAdapter;
    private Dialog chatDialog;
    private boolean localMicEnabled = true;
    private boolean localVideoEnabled = true;
    private String currentMeetingSettings = "{}";
    private String pendingPermissionRequest;
    private LiveKitRoomManager liveKitRoomManager;
    private String liveKitToken;
    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            this::handlePermissionResult
    );
    private final Handler waitingRoomHandler = new Handler(Looper.getMainLooper());
    private final Handler recordingStatusHandler = new Handler(Looper.getMainLooper());
    private boolean waitingRoomPollingActive;
    private final Runnable waitingRoomPollingRunnable = new Runnable() {
        @Override
        public void run() {
            if (!waitingRoomPollingActive || isFinishing()) {
                return;
            }
            loadWaitingParticipants();
            waitingRoomHandler.postDelayed(this, 5000);
        }
    };
    private final Runnable recordingStatusRunnable = new Runnable() {
        @Override
        public void run() {
            pollRecordingStatus();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting);

        apiService = RetrofitClient.getApiService(this);
        sessionManager = new SessionManager(this);
        meetingCode = getIntent().getStringExtra("MEETING_CODE");
        userRole = getIntent().getStringExtra("USER_ROLE");
        livekitUrl = getIntent().getStringExtra("LIVEKIT_URL");
        liveKitToken = getIntent().getStringExtra("LIVEKIT_TOKEN");
        isMeetingOwner = getIntent().getBooleanExtra("IS_OWNER", false)
                || "OWNER".equalsIgnoreCase(userRole);
        meetingRealtimeClient = new MeetingRealtimeClient(this, meetingCode);
        liveKitRoomManager = new LiveKitRoomManager(this);

        rvParticipants = findViewById(R.id.rvParticipants);
        tvMeetingCode = findViewById(R.id.tvMeetingCode);
        badgeWaiting = findViewById(R.id.badgeWaiting);
        btnLeave = findViewById(R.id.btnLeave);
        btnMic = findViewById(R.id.btnMic);
        btnVideo = findViewById(R.id.btnVideo);
        btnParticipants = findViewById(R.id.btnParticipants);
        btnChat = findViewById(R.id.btnChat);
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera);
        btnRecord = findViewById(R.id.btnRecord);
        tvRecordingStatus = findViewById(R.id.tvRecordingStatus);

        tvMeetingCode.setText(meetingCode != null ? meetingCode : "---");
        seedLocalParticipant();

        participantAdapter = new ParticipantAdapter(participantList);
        participantAdapter.setVideoBinder((participant, videoContainer) -> {
            if (liveKitRoomManager != null) {
                liveKitRoomManager.attachVideo(participant.getIdentity(), videoContainer);
            }
        });
        rvParticipants.setLayoutManager(new GridLayoutManager(this, 2));
        rvParticipants.setAdapter(participantAdapter);

        btnLeave.setOnClickListener(v -> showLeaveOptions());
        btnMic.setOnClickListener(v -> toggleLocalMic());
        btnVideo.setOnClickListener(v -> toggleLocalVideo());
        btnParticipants.setOnClickListener(v -> {
            if (!isHostLikeRole()) {
                Toast.makeText(this, "Chi host moi duyet phong cho", Toast.LENGTH_SHORT).show();
                return;
            }
            openWaitingRoomDialog();
        });
        btnChat.setOnClickListener(v -> openChatDialog());
        btnRecord.setOnClickListener(v -> toggleRecording());
        btnSwitchCamera.setOnClickListener(v -> {
            if (isHostLikeRole()) {
                showHostControlsDialog();
            } else {
                showMeetingSettings();
            }
        });
        updateLocalControlsUi();
        updateRecordingUi("IDLE");
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMeetingInfo();
        if (isHostLikeRole()) {
            loadWaitingParticipants();
        } else {
            updateWaitingBadge(false);
        }
        startWaitingRoomPolling();
        connectRealtime();
        connectLiveKitRoomIfPossible();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopWaitingRoomPolling();
        disconnectRealtime();
        disconnectLiveKitRoom();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (liveKitRoomManager != null) {
            liveKitRoomManager.release();
        }
        stopRecordingStatusPolling();
    }

    private void seedLocalParticipant() {
        participantList.clear();
        participantList.add(new ParticipantData(
                sessionManager.getUserId() != null ? sessionManager.getUserId() : "self",
                sessionManager.getUserId() != null ? sessionManager.getUserId() : "self",
                sessionManager.getUserName(),
                true,
                true,
                false,
                true
        ));
    }

    private void loadMeetingInfo() {
        if (meetingCode == null || meetingCode.trim().isEmpty()) {
            return;
        }

        apiService.getMeetingInfo(meetingCode).enqueue(new Callback<ApiResponse<MeetingInfoResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<MeetingInfoResponse>> call, Response<ApiResponse<MeetingInfoResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    MeetingInfoResponse info = response.body().getData();
                    tvMeetingCode.setText(info.getMeetingCode() + " • " + info.getTitle());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MeetingInfoResponse>> call, Throwable t) {
            }
        });
    }

    private void loadWaitingParticipants() {
        if (meetingCode == null || meetingCode.trim().isEmpty()) {
            updateWaitingBadge(false);
            return;
        }

        apiService.getWaitingRoom(meetingCode).enqueue(new Callback<ApiResponse<List<ParticipantResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ParticipantResponse>>> call, Response<ApiResponse<List<ParticipantResponse>>> response) {
                waitingParticipants.clear();
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    waitingParticipants.addAll(response.body().getData());
                }
                updateWaitingBadge(!waitingParticipants.isEmpty());
            }

            @Override
            public void onFailure(Call<ApiResponse<List<ParticipantResponse>>> call, Throwable t) {
                updateWaitingBadge(false);
            }
        });
    }

    private void showLeaveOptions() {
        boolean isHost = isHostLikeRole();
        List<String> options = new ArrayList<>();
        options.add("Roi phong");
        if (isHost) {
            options.add("Ket thuc cho tat ca");
        }

        new AlertDialog.Builder(this)
                .setTitle("Tuy chon cuoc hop")
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    if (which == 0) {
                        leaveMeeting();
                    } else if (isHost) {
                        endMeetingForAll();
                    }
                })
                .setNegativeButton("Dong", null)
                .show();
    }

    private void leaveMeeting() {
        apiService.leaveMeeting(meetingCode).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                Toast.makeText(MeetingActivity.this, "Da roi phong", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(MeetingActivity.this, "Khong the roi phong luc nay", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void endMeetingForAll() {
        apiService.endMeeting(meetingCode).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                Toast.makeText(MeetingActivity.this, "Da ket thuc cuoc hop", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(MeetingActivity.this, "Khong the ket thuc cuoc hop", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openWaitingRoomDialog() {
        if (waitingParticipants.isEmpty()) {
            Toast.makeText(this, "Phong cho hien dang trong", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[waitingParticipants.size()];
        for (int index = 0; index < waitingParticipants.size(); index++) {
            ParticipantResponse item = waitingParticipants.get(index);
            String email = item.getEmail() == null ? "" : " • " + item.getEmail();
            names[index] = item.getDisplayName() + email;
        }

        new AlertDialog.Builder(this)
                .setTitle("Phong cho")
                .setItems(names, (dialog, which) -> showApprovalActions(waitingParticipants.get(which)))
                .setNegativeButton("Dong", null)
                .show();
    }

    private void showApprovalActions(ParticipantResponse participant) {
        String[] actions = new String[]{"Duyet vao phong", "Tu choi"};
        new AlertDialog.Builder(this)
                .setTitle(participant.getDisplayName())
                .setItems(actions, (dialog, which) -> {
                    String action = which == 0 ? "APPROVED" : "REJECTED";
                    processWaitingApproval(participant, action);
                })
                .setNegativeButton("Huy", null)
                .show();
    }

    private void processWaitingApproval(ParticipantResponse participant, String action) {
        apiService.approveParticipant(meetingCode, new ApprovalRequest(participant.getParticipantId(), action))
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        Toast.makeText(MeetingActivity.this, response.body() != null ? response.body().getMessage() : "Da xu ly", Toast.LENGTH_SHORT).show();
                        loadWaitingParticipants();
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        Toast.makeText(MeetingActivity.this, "Khong the xu ly phong cho", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showMeetingSettings() {
        apiService.getMeetingSettings(meetingCode).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                String settings = response.isSuccessful() && response.body() != null && response.body().getData() != null
                        ? response.body().getData()
                        : "Chua co settings";
                currentMeetingSettings = settings;

                new AlertDialog.Builder(MeetingActivity.this)
                        .setTitle("Meeting settings")
                        .setMessage(settings)
                        .setPositiveButton("Dong", null)
                        .show();
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                Toast.makeText(MeetingActivity.this, "Khong the tai settings", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isHostLikeRole() {
        if (userRole == null) {
            return false;
        }
        String normalized = userRole.trim().toUpperCase();
        return "HOST".equals(normalized) || "OWNER".equals(normalized) || "ADMIN".equals(normalized);
    }

    private void connectRealtime() {
        if (meetingRealtimeClient == null || meetingRealtimeClient.isConnected()) {
            return;
        }

        meetingRealtimeClient.connect(new StompSocketClient.ConnectionListener() {
            @Override
            public void onConnected() {
                subscribeRealtimeTopics();
                loadChatHistory();
            }

            @Override
            public void onError(String message) {
            }

            @Override
            public void onDisconnected() {
            }
        });
    }

    private void subscribeRealtimeTopics() {
        if (systemSubscriptionId == null) {
            systemSubscriptionId = meetingRealtimeClient.subscribeToSystem((destination, body) -> handleSystemRealtime(body));
        }
        if (chatSubscriptionId == null) {
            chatSubscriptionId = meetingRealtimeClient.subscribeToChat((destination, body) -> handleChatRealtime(body));
        }

        if (isHostLikeRole()) {
            if (adminSubscriptionId == null) {
                adminSubscriptionId = meetingRealtimeClient.subscribeToAdmin((destination, body) -> {
                    loadWaitingParticipants();
                    showJoinRequestToast(body);
                });
            }
            if (waitingRoomSubscriptionId == null) {
                waitingRoomSubscriptionId = meetingRealtimeClient.subscribeToWaitingRoom((destination, body) -> {
                    if ("SETTINGS_CHANGED".equalsIgnoreCase(body) || "HOST_JOINED".equalsIgnoreCase(body)) {
                        loadWaitingParticipants();
                    }
                });
            }
        }
    }

    private void disconnectRealtime() {
        adminSubscriptionId = null;
        waitingRoomSubscriptionId = null;
        systemSubscriptionId = null;
        chatSubscriptionId = null;
        if (meetingRealtimeClient != null) {
            meetingRealtimeClient.disconnect();
        }
    }

    private void showJoinRequestToast(String body) {
        try {
            JSONObject jsonObject = new JSONObject(body);
            String displayName = jsonObject.optString("displayName", "Nguoi dung");
            Toast.makeText(this, displayName + " dang xin vao phong", Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
        }
    }

    private void handleSystemRealtime(String body) {
        if ("MEETING_ENDED".equalsIgnoreCase(body)) {
            Toast.makeText(this, "Cuoc hop da ket thuc", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(body);
            String type = jsonObject.optString("type");
            if ("HOST_TRANSFERRED".equalsIgnoreCase(type)) {
                String newHostId = jsonObject.optString("newHostId");
                if (newHostId != null && newHostId.equals(sessionManager.getUserId())) {
                    userRole = "HOST";
                    loadWaitingParticipants();
                    subscribeRealtimeTopics();
                    Toast.makeText(this, "Ban vua duoc chuyen quyen host", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            if ("RECORDING_STARTED".equalsIgnoreCase(type)) {
                recordingActive = true;
                updateRecordingUi("RECORDING");
                return;
            }
            if ("RECORDING_STOPPED".equalsIgnoreCase(type)) {
                recordingActive = false;
                updateRecordingUi(isMeetingOwner && recordingEgressId != null ? "STOPPING" : "IDLE");
                return;
            }
            if ("MUTE_ALL".equalsIgnoreCase(type)) {
                applyRemoteMicMute("Host da tat mic cua moi nguoi.");
                return;
            }
            if ("STOP_CAMERA_ALL".equalsIgnoreCase(type)) {
                applyRemoteCameraOff("Host da tat camera cua moi nguoi.");
                return;
            }
            if ("KICK_ALL".equalsIgnoreCase(type)) {
                Toast.makeText(this, "Ban da bi moi roi khoi cuoc hop", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            String targetParticipantId = jsonObject.optString("targetParticipantId");
            if (targetParticipantId != null && targetParticipantId.equals(sessionManager.getUserId())) {
                if ("MUTE_PARTICIPANT".equalsIgnoreCase(type)) {
                    applyRemoteMicMute("Host da tat mic cua ban.");
                } else if ("STOP_CAMERA_PARTICIPANT".equalsIgnoreCase(type)) {
                    applyRemoteCameraOff("Host da tat camera cua ban.");
                } else if ("KICK_PARTICIPANT".equalsIgnoreCase(type)) {
                    Toast.makeText(this, "Ban da bi host moi ra khoi cuoc hop", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void handleChatRealtime(String body) {
        try {
            JSONObject jsonObject = new JSONObject(body);
            ChatMessageResponse message = new ChatMessageResponse(
                    jsonObject.optString("senderId"),
                    jsonObject.optString("senderName"),
                    jsonObject.optString("content")
            );
            chatMessages.add(message);
            if (chatMessageAdapter != null) {
                chatMessageAdapter.addMessage(message);
            }
        } catch (Exception ignored) {
        }
    }

    private void loadChatHistory() {
        apiService.getChatHistory(meetingCode).enqueue(new Callback<ApiResponse<List<ChatMessageResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ChatMessageResponse>>> call, Response<ApiResponse<List<ChatMessageResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    chatMessages.clear();
                    chatMessages.addAll(response.body().getData());
                    if (chatMessageAdapter != null) {
                        chatMessageAdapter.submitList(chatMessages);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<ChatMessageResponse>>> call, Throwable t) {
            }
        });
    }

    private void openChatDialog() {
        View chatView = LayoutInflater.from(this).inflate(R.layout.dialog_chat, null, false);
        RecyclerView rvChatMessages = chatView.findViewById(R.id.rvChatMessages);
        EditText etChatMessage = chatView.findViewById(R.id.etChatMessage);
        MaterialButton btnSendChat = chatView.findViewById(R.id.btnSendChat);

        chatMessageAdapter = new ChatMessageAdapter(sessionManager.getUserId());
        rvChatMessages.setLayoutManager(new LinearLayoutManager(this));
        rvChatMessages.setAdapter(chatMessageAdapter);
        chatMessageAdapter.submitList(chatMessages);

        btnSendChat.setOnClickListener(v -> {
            String message = etChatMessage.getText().toString().trim();
            if (message.isEmpty()) {
                return;
            }
            sendChatMessage(message);
            etChatMessage.setText("");
        });

        chatDialog = new AlertDialog.Builder(this)
                .setView(chatView)
                .setPositiveButton("Close", null)
                .create();
        chatDialog.show();
        loadChatHistory();
    }

    private void sendChatMessage(String content) {
        if (meetingRealtimeClient == null || !meetingRealtimeClient.isConnected()) {
            Toast.makeText(this, "Chat socket chua san sang", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("senderId", sessionManager.getUserId());
            jsonObject.put("senderName", sessionManager.getUserName());
            jsonObject.put("content", content);
            meetingRealtimeClient.sendChatMessage(jsonObject.toString());
        } catch (Exception ignored) {
        }
    }

    private void showHostControlsDialog() {
        String[] options = new String[]{
                "Meeting settings",
                "Toggle waiting room",
                "Mute all participants",
                "Stop camera all",
                "Kick all participants"
        };

        new AlertDialog.Builder(this)
                .setTitle("Host controls")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showMeetingSettings();
                    } else if (which == 1) {
                        toggleWaitingRoomSetting();
                    } else if (which == 2) {
                        sendSystemAction(SystemActionHelper.createPayload("MUTE_ALL"));
                    } else if (which == 3) {
                        sendSystemAction(SystemActionHelper.createPayload("STOP_CAMERA_ALL"));
                    } else if (which == 4) {
                        sendSystemAction(SystemActionHelper.createPayload("KICK_ALL"));
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void toggleWaitingRoomSetting() {
        apiService.getMeetingSettings(meetingCode).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                String settings = response.isSuccessful() && response.body() != null && response.body().getData() != null
                        ? response.body().getData()
                        : "{}";
                currentMeetingSettings = settings;
                Map<String, Object> updated = parseSettingsToMap(settings);
                boolean currentValue = Boolean.TRUE.equals(updated.get("waitingRoom"));
                updated.put("waitingRoom", !currentValue);
                updateMeetingSettings(updated, !currentValue);
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                Toast.makeText(MeetingActivity.this, "Khong the tai settings hien tai", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Map<String, Object> parseSettingsToMap(String settingsJson) {
        Map<String, Object> settingsMap = new LinkedHashMap<>();
        settingsMap.put("waitingRoom", true);
        settingsMap.put("muteAudioOnEntry", false);
        settingsMap.put("muteVideoOnEntry", false);
        settingsMap.put("chatEnabled", true);
        settingsMap.put("screenShareEnabled", true);

        try {
            JSONObject jsonObject = new JSONObject(settingsJson);
            String[] keys = new String[]{"waitingRoom", "muteAudioOnEntry", "muteVideoOnEntry", "chatEnabled", "screenShareEnabled"};
            for (String key : keys) {
                if (jsonObject.has(key)) {
                    settingsMap.put(key, jsonObject.get(key));
                }
            }
        } catch (Exception ignored) {
        }
        return settingsMap;
    }

    private void updateMeetingSettings(Map<String, Object> settings, boolean waitingRoomEnabled) {
        apiService.updateMeetingSettings(meetingCode, settings).enqueue(new Callback<ApiResponse<com.ptithcm.ptitmeet.api.dto.meeting.MeetingResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<com.ptithcm.ptitmeet.api.dto.meeting.MeetingResponse>> call, Response<ApiResponse<com.ptithcm.ptitmeet.api.dto.meeting.MeetingResponse>> response) {
                Toast.makeText(MeetingActivity.this,
                        waitingRoomEnabled ? "Da bat waiting room" : "Da tat waiting room",
                        Toast.LENGTH_SHORT).show();
                loadWaitingParticipants();
            }

            @Override
            public void onFailure(Call<ApiResponse<com.ptithcm.ptitmeet.api.dto.meeting.MeetingResponse>> call, Throwable t) {
                Toast.makeText(MeetingActivity.this, "Khong the cap nhat settings", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendSystemAction(String payload) {
        if (meetingRealtimeClient == null || !meetingRealtimeClient.isConnected()) {
            Toast.makeText(this, "Socket system chua san sang", Toast.LENGTH_SHORT).show();
            return;
        }
        meetingRealtimeClient.sendSystemAction(payload);
    }

    private void publishRecordingAction(String type) {
        if (meetingRealtimeClient != null && meetingRealtimeClient.isConnected()) {
            meetingRealtimeClient.sendSystemAction(SystemActionHelper.createPayload(type));
        }
    }

    private void toggleRecording() {
        if (!isMeetingOwner) {
            Toast.makeText(this, "Only the meeting owner can record", Toast.LENGTH_SHORT).show();
            return;
        }
        if (recordingRequestInFlight) {
            return;
        }
        if (recordingActive) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        if (meetingCode == null || meetingCode.trim().isEmpty()) {
            Toast.makeText(this, "Missing meeting code", Toast.LENGTH_SHORT).show();
            return;
        }
        recordingRequestInFlight = true;
        updateRecordingUi("STARTING");

        apiService.startRecording(meetingCode).enqueue(new Callback<ApiResponse<MeetingRecordingResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<MeetingRecordingResponse>> call, Response<ApiResponse<MeetingRecordingResponse>> response) {
                recordingRequestInFlight = false;
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    MeetingRecordingResponse recording = response.body().getData();
                    recordingEgressId = recording.getEgressId();
                    recordingActive = true;
                    updateRecordingUi(safeRecordingStatus(recording.getStatus(), "RECORDING"));
                    publishRecordingAction("RECORDING_STARTED");
                    Toast.makeText(MeetingActivity.this, "Recording started", Toast.LENGTH_SHORT).show();
                    return;
                }
                recordingActive = false;
                updateRecordingUi("FAILED");
                String message = response.body() != null
                        ? response.body().getMessage()
                        : "Unable to start recording";
                Toast.makeText(MeetingActivity.this, message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<ApiResponse<MeetingRecordingResponse>> call, Throwable t) {
                recordingRequestInFlight = false;
                recordingActive = false;
                updateRecordingUi("FAILED");
                Toast.makeText(MeetingActivity.this, "Cannot start recording", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void stopRecording() {
        if (recordingEgressId == null || recordingEgressId.trim().isEmpty()) {
            recordingActive = false;
            updateRecordingUi("FAILED");
            Toast.makeText(this, "Missing recording session id", Toast.LENGTH_SHORT).show();
            return;
        }

        recordingRequestInFlight = true;
        updateRecordingUi("STOPPING");
        apiService.stopRecording(recordingEgressId).enqueue(new Callback<MeetingRecordingResponse>() {
            @Override
            public void onResponse(Call<MeetingRecordingResponse> call, Response<MeetingRecordingResponse> response) {
                recordingRequestInFlight = false;
                recordingActive = false;
                String status = response.isSuccessful() && response.body() != null
                        ? safeRecordingStatus(response.body().getStatus(), "STOPPING")
                        : "STOPPING";
                updateRecordingUi(status);
                publishRecordingAction("RECORDING_STOPPED");
                startRecordingStatusPolling();
                Toast.makeText(MeetingActivity.this, "Stopping recording", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<MeetingRecordingResponse> call, Throwable t) {
                recordingRequestInFlight = false;
                updateRecordingUi("FAILED");
                Toast.makeText(MeetingActivity.this, "Cannot stop recording", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startRecordingStatusPolling() {
        recordingStatusHandler.removeCallbacks(recordingStatusRunnable);
        recordingStatusHandler.postDelayed(recordingStatusRunnable, 3000);
    }

    private void stopRecordingStatusPolling() {
        recordingStatusHandler.removeCallbacks(recordingStatusRunnable);
    }

    private void pollRecordingStatus() {
        if (recordingEgressId == null || recordingEgressId.trim().isEmpty()) {
            stopRecordingStatusPolling();
            return;
        }

        apiService.getRecordingStatus(recordingEgressId).enqueue(new Callback<MeetingRecordingResponse>() {
            @Override
            public void onResponse(Call<MeetingRecordingResponse> call, Response<MeetingRecordingResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String status = safeRecordingStatus(response.body().getStatus(), "STOPPING");
                    updateRecordingUi(status);
                    if ("COMPLETED".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status)) {
                        stopRecordingStatusPolling();
                        if ("COMPLETED".equalsIgnoreCase(status)) {
                            Toast.makeText(MeetingActivity.this, "Recording saved", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }
                    recordingStatusHandler.postDelayed(recordingStatusRunnable, 5000);
                    return;
                }
                updateRecordingUi("FAILED");
                stopRecordingStatusPolling();
            }

            @Override
            public void onFailure(Call<MeetingRecordingResponse> call, Throwable t) {
                updateRecordingUi("FAILED");
                stopRecordingStatusPolling();
            }
        });
    }

    private String safeRecordingStatus(String status, String fallback) {
        return status == null || status.trim().isEmpty() ? fallback : status.trim().toUpperCase();
    }

    private void updateRecordingUi(String status) {
        String normalized = safeRecordingStatus(status, "IDLE");
        boolean showStatus = !"IDLE".equalsIgnoreCase(normalized);

        if (tvRecordingStatus != null) {
            tvRecordingStatus.setVisibility(showStatus ? View.VISIBLE : View.GONE);
            if ("RECORDING".equalsIgnoreCase(normalized)) {
                tvRecordingStatus.setText("REC");
            } else {
                tvRecordingStatus.setText(normalized);
            }
        }

        if (btnRecord != null) {
            btnRecord.setEnabled(isMeetingOwner && !recordingRequestInFlight);
            btnRecord.setAlpha(isMeetingOwner ? 1f : 0.35f);
            int color = recordingActive
                    ? Color.parseColor("#E53935")
                    : Color.parseColor("#2D3748");
            btnRecord.setBackgroundTintList(ColorStateList.valueOf(color));
        }
    }

    private void toggleLocalMic() {
        if (!localMicEnabled) {
            if (!DevicePermissionHelper.hasMicrophonePermission(this)) {
                pendingPermissionRequest = DevicePermissionHelper.REQUEST_MIC;
                permissionLauncher.launch(new String[]{Manifest.permission.RECORD_AUDIO});
                return;
            }
        }
        localMicEnabled = !localMicEnabled;
        if (liveKitRoomManager != null) {
            liveKitRoomManager.setMicrophoneEnabled(localMicEnabled);
        }
        updateLocalControlsUi();
    }

    private void toggleLocalVideo() {
        if (!localVideoEnabled) {
            if (!DevicePermissionHelper.hasCameraPermission(this)) {
                pendingPermissionRequest = DevicePermissionHelper.REQUEST_CAMERA;
                permissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
                return;
            }
        }
        localVideoEnabled = !localVideoEnabled;
        if (liveKitRoomManager != null) {
            liveKitRoomManager.setCameraEnabled(localVideoEnabled);
        }
        updateLocalControlsUi();
    }

    private void applyRemoteMicMute(String message) {
        localMicEnabled = false;
        updateLocalControlsUi();
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void applyRemoteCameraOff(String message) {
        localVideoEnabled = false;
        updateLocalControlsUi();
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void updateLocalControlsUi() {
        btnMic.setAlpha(localMicEnabled ? 1f : 0.5f);
        btnVideo.setAlpha(localVideoEnabled ? 1f : 0.5f);
    }

    private void handlePermissionResult(Map<String, Boolean> permissions) {
        if (DevicePermissionHelper.REQUEST_MIC.equals(pendingPermissionRequest)) {
            boolean granted = Boolean.TRUE.equals(permissions.get(Manifest.permission.RECORD_AUDIO));
            if (granted) {
                localMicEnabled = true;
                updateLocalControlsUi();
            } else {
                Toast.makeText(this, "Can quyen microphone de bat mic", Toast.LENGTH_SHORT).show();
            }
        } else if (DevicePermissionHelper.REQUEST_CAMERA.equals(pendingPermissionRequest)) {
            boolean granted = Boolean.TRUE.equals(permissions.get(Manifest.permission.CAMERA));
            if (granted) {
                localVideoEnabled = true;
                updateLocalControlsUi();
            } else {
                Toast.makeText(this, "Can quyen camera de bat video", Toast.LENGTH_SHORT).show();
            }
        }
        pendingPermissionRequest = null;
    }

    private void startWaitingRoomPolling() {
        if (!isHostLikeRole() || waitingRoomPollingActive) {
            return;
        }
        waitingRoomPollingActive = true;
        waitingRoomHandler.removeCallbacks(waitingRoomPollingRunnable);
        waitingRoomHandler.postDelayed(waitingRoomPollingRunnable, 5000);
    }

    private void stopWaitingRoomPolling() {
        waitingRoomPollingActive = false;
        waitingRoomHandler.removeCallbacks(waitingRoomPollingRunnable);
    }

    private void updateWaitingBadge(boolean hasWaitingUsers) {
        runOnUiThread(() -> {
            if (hasWaitingUsers) {
                badgeWaiting.setVisibility(View.VISIBLE);
            } else {
                badgeWaiting.setVisibility(View.GONE);
            }
        });
    }

    private void connectLiveKitRoomIfPossible() {
        if (liveKitRoomManager == null) {
            return;
        }
        if (livekitUrl == null || livekitUrl.trim().isEmpty() || liveKitToken == null || liveKitToken.trim().isEmpty()) {
            Toast.makeText(this, "Chua co du lieu LiveKit de vao phong media", Toast.LENGTH_SHORT).show();
            return;
        }

        liveKitRoomManager.connect(
                livekitUrl,
                liveKitToken,
                localMicEnabled,
                localVideoEnabled,
                new LiveKitRoomManager.Listener() {
                    @Override
                    public void onConnected() {
                        runOnUiThread(() -> Toast.makeText(MeetingActivity.this, "Da vao phong hop", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onConnectionError(String message) {
                        runOnUiThread(() -> Toast.makeText(MeetingActivity.this, message, Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onDisconnected() {
                        runOnUiThread(() -> Toast.makeText(MeetingActivity.this, "Da ngat ket noi phong", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onParticipantsUpdated(List<LiveParticipantState> participants) {
                        runOnUiThread(() -> bindParticipants(participants));
                    }
                }
        );
    }

    private void disconnectLiveKitRoom() {
        if (liveKitRoomManager != null) {
            liveKitRoomManager.disconnect();
        }
    }

    private void bindParticipants(List<LiveParticipantState> participants) {
        participantList.clear();
        for (LiveParticipantState participant : participants) {
            participantList.add(new ParticipantData(
                    participant.getIdentity(),
                    participant.getIdentity(),
                    participant.getDisplayName(),
                    participant.getHasVideo(),
                    participant.isMicOn(),
                    participant.isSpeaking(),
                    participant.isLocal()
            ));
        }
        participantAdapter.setParticipantList(participantList);
    }
}
