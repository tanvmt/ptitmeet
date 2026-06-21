package com.ptithcm.ptitmeet.activities;

import android.Manifest;
import android.content.Intent;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.view.Gravity;
import android.view.animation.TranslateAnimation;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.AccelerateInterpolator;
import android.media.projection.MediaProjectionManager;
import android.content.Context;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.ptithcm.ptitmeet.adapters.ChatMessageAdapter;
import com.ptithcm.ptitmeet.adapters.WaitingParticipantAdapter;
import com.ptithcm.ptitmeet.ParticipantAdapter;
import com.ptithcm.ptitmeet.ActiveParticipantAdapter;
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
import com.ptithcm.ptitmeet.utils.MeetingSoundPlayer;

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
    private ImageButton btnSettings;
    private ImageButton btnMore;
    private TextView tvRecordingStatus;

    private LiveKitRoomManager liveKitRoomManager;
    private boolean isLocalHandRaised = false;
    private boolean isLocalScreenSharing = false;
    private BottomSheetDialog moreOptionsDialog;
    private View bottomSheetRecordLayout;
    private ImageView bottomSheetRecordIcon;
    private TextView bottomSheetRecordText;

    private final ActivityResultLauncher<Intent> screenShareLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    isLocalScreenSharing = true;
                    if (liveKitRoomManager != null) {
                        liveKitRoomManager.setScreenShareEnabled(true, result.getData());
                    }
                    Toast.makeText(this, "Screen sharing started", Toast.LENGTH_SHORT).show();
                    updateMoreOptionsBottomSheetUi();
                } else {
                    Toast.makeText(this, "Screen sharing permission denied", Toast.LENGTH_SHORT).show();
                }
            }
    );

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
    private com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog;
    private com.ptithcm.ptitmeet.adapters.WaitingParticipantAdapter waitingAdapter;
    private ActiveParticipantAdapter activeParticipantAdapter;
    private View dialogWaitingHeader;
    private View dialogWaitingList;
    private View cardJoinRequest;
    private TextView tvAvatarInitial;
    private TextView tvRequestName;
    private MaterialButton btnDeclineRequest;
    private MaterialButton btnAcceptRequest;
    private final Handler toastDismissHandler = new Handler(Looper.getMainLooper());
    private final Runnable toastDismissRunnable = new Runnable() {
        @Override
        public void run() {
            if (cardJoinRequest != null) {
                cardJoinRequest.setVisibility(View.GONE);
            }
        }
    };
    private String currentMeetingSettings = "{}";
    private String pendingPermissionRequest;
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

        localMicEnabled = getIntent().getBooleanExtra("MIC_ON", true);
        localVideoEnabled = getIntent().getBooleanExtra("VIDEO_ON", true);

        rvParticipants = findViewById(R.id.rvParticipants);
        tvMeetingCode = findViewById(R.id.tvMeetingCode);
        badgeWaiting = findViewById(R.id.badgeWaiting);
        btnLeave = findViewById(R.id.btnLeave);
        btnMic = findViewById(R.id.btnMic);
        btnVideo = findViewById(R.id.btnVideo);
        btnParticipants = findViewById(R.id.btnParticipants);
        btnChat = findViewById(R.id.btnChat);
        btnSettings = findViewById(R.id.btnSettings);
        btnMore = findViewById(R.id.btnMore);
        tvRecordingStatus = findViewById(R.id.tvRecordingStatus);

        cardJoinRequest = findViewById(R.id.cardJoinRequest);
        tvAvatarInitial = findViewById(R.id.tvAvatarInitial);
        tvRequestName = findViewById(R.id.tvRequestName);
        btnDeclineRequest = findViewById(R.id.btnDeclineRequest);
        btnAcceptRequest = findViewById(R.id.btnAcceptRequest);

        tvMeetingCode.setText(meetingCode != null ? meetingCode : "---");
        seedLocalParticipant();

        participantAdapter = new ParticipantAdapter(participantList);
        participantAdapter.setVideoBinder((participant, videoContainer) -> {
            if (liveKitRoomManager != null) {
                liveKitRoomManager.attachVideo(participant.getIdentity(), videoContainer);
            }
        });
        final GridLayoutManager glm = new GridLayoutManager(this, 2);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int totalItems = participantAdapter.getItemCount();
                if (glm.getSpanCount() > 1 && totalItems % 2 != 0 && position == 0) {
                    return 2; // Odd count: make the first item full-width
                }
                return 1;
            }
        });
        rvParticipants.setLayoutManager(glm);
        rvParticipants.setAdapter(participantAdapter);

        btnLeave.setOnClickListener(v -> showLeaveOptions());
        btnMic.setOnClickListener(v -> toggleLocalMic());
        btnVideo.setOnClickListener(v -> toggleLocalVideo());
        btnParticipants.setOnClickListener(v -> {
            openParticipantsDialog();
        });
        btnChat.setOnClickListener(v -> openChatDialog());
        btnMore.setOnClickListener(v -> showMoreOptionsBottomSheet());
        btnSettings.setOnClickListener(v -> {
            if (isHostLikeRole()) {
                showMeetingSettingsBottomSheet();
            } else {
                showAppAudioSettingsDialog();
            }
        });
        updateLocalControlsUi();
        updateRecordingUi("IDLE");
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMeetingInfo();
        loadMeetingSettings();
        if (isHostLikeRole()) {
            loadWaitingParticipants();
        } else {
            updateWaitingBadge(false);
        }
        startWaitingRoomPolling();
        connectRealtime();
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
        MeetingSoundPlayer.release();
    }

    private void seedLocalParticipant() {
        participantList.clear();
        participantList.add(new ParticipantData(
                sessionManager.getUserId() != null ? sessionManager.getUserId() : "self",
                sessionManager.getUserId() != null ? sessionManager.getUserId() : "self",
                sessionManager.getUserName(),
                localVideoEnabled,
                localMicEnabled,
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

    private void loadMeetingSettings() {
        if (meetingCode == null || meetingCode.trim().isEmpty()) {
            return;
        }
        apiService.getMeetingSettings(meetingCode).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    String json = response.body().getData();
                    currentMeetingSettings = json;

                    // Apply mute on entry if we are not the host
                    if (!isHostLikeRole()) {
                        java.util.Map<String, Object> settingsMap = parseSettingsToMap(json);
                        boolean muteAudio = Boolean.TRUE.equals(settingsMap.get("muteAudioOnEntry"));
                        boolean muteVideo = Boolean.TRUE.equals(settingsMap.get("muteVideoOnEntry"));

                        if (muteAudio) {
                            localMicEnabled = false;
                        }
                        if (muteVideo) {
                            localVideoEnabled = false;
                        }
                        runOnUiThread(() -> updateLocalControlsUi());
                    }
                }
                connectLiveKitRoomIfPossible();
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                connectLiveKitRoomIfPossible();
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

                runOnUiThread(() -> {
                    if (waitingAdapter != null) {
                        waitingAdapter.submitList(new ArrayList<>(waitingParticipants));
                    }
                    if (dialogWaitingHeader != null && dialogWaitingList != null) {
                        if (isHostLikeRole() && !waitingParticipants.isEmpty()) {
                            dialogWaitingHeader.setVisibility(View.VISIBLE);
                            dialogWaitingList.setVisibility(View.VISIBLE);
                        } else {
                            dialogWaitingHeader.setVisibility(View.GONE);
                            dialogWaitingList.setVisibility(View.GONE);
                        }
                    }
                });
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
        options.add("Leave meeting");
        if (isHost) {
            options.add("End meeting for all");
        }

        new AlertDialog.Builder(this)
                .setTitle("Meeting options")
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    if (which == 0) {
                        leaveMeeting();
                    } else if (isHost) {
                        endMeetingForAll();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void navigateToSummary(String actionTaken) {
        Intent intent = new Intent(MeetingActivity.this, SummaryActivity.class);
        intent.putExtra("MEETING_CODE", meetingCode);
        intent.putExtra("ACTION_TAKEN", actionTaken);
        startActivity(intent);
        finish();
    }

    private void leaveMeeting() {
        apiService.leaveMeeting(meetingCode).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                Toast.makeText(MeetingActivity.this, "Left the meeting", Toast.LENGTH_SHORT).show();
                navigateToSummary("LEAVE");
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(MeetingActivity.this, "Unable to leave the meeting at this time", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void endMeetingForAll() {
        apiService.endMeeting(meetingCode).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                Toast.makeText(MeetingActivity.this, "Ended the meeting", Toast.LENGTH_SHORT).show();
                navigateToSummary("END");
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(MeetingActivity.this, "Unable to end the meeting", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openParticipantsDialog() {
        if (bottomSheetDialog == null) {
            bottomSheetDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_waiting_list, null);
            bottomSheetDialog.setContentView(dialogView);

            RecyclerView rvWaitingList = dialogView.findViewById(R.id.rvWaitingList);
            RecyclerView rvActiveList = dialogView.findViewById(R.id.rvActiveList);
            dialogWaitingHeader = dialogView.findViewById(R.id.tvWaitingHeader);
            dialogWaitingList = rvWaitingList;
            MaterialButton btnCloseWaitingList = dialogView.findViewById(R.id.btnCloseWaitingList);

            boolean isHost = isHostLikeRole();

            waitingAdapter = new WaitingParticipantAdapter(
                    new WaitingParticipantAdapter.OnWaitingActionClickListener() {
                        @Override
                        public void onApprove(ParticipantResponse participant) {
                            processWaitingApproval(participant.getParticipantId(), participant.getDisplayName(), "APPROVED");
                        }

                        @Override
                        public void onReject(ParticipantResponse participant) {
                            processWaitingApproval(participant.getParticipantId(), participant.getDisplayName(), "REJECTED");
                        }
                    }
            );
            rvWaitingList.setLayoutManager(new LinearLayoutManager(this));
            rvWaitingList.setAdapter(waitingAdapter);

            activeParticipantAdapter = new ActiveParticipantAdapter(
                    new ArrayList<>(participantList),
                    isHost,
                    new ActiveParticipantAdapter.OnActiveParticipantActionListener() {
                        @Override
                        public void onMute(ParticipantData participant) {
                            sendSystemAction(SystemActionHelper.createPayload("MUTE_PARTICIPANT", participant.getIdentity(), participant.getName()));
                        }

                        @Override
                        public void onStopCam(ParticipantData participant) {
                            sendSystemAction(SystemActionHelper.createPayload("STOP_CAMERA_PARTICIPANT", participant.getIdentity(), participant.getName()));
                        }

                        @Override
                        public void onKick(ParticipantData participant) {
                            new AlertDialog.Builder(MeetingActivity.this)
                                    .setTitle("Kick Participant")
                                    .setMessage("Are you sure you want to kick " + participant.getName() + "?")
                                    .setPositiveButton("Kick", (d, w) -> {
                                        sendSystemAction(SystemActionHelper.createPayload("KICK_PARTICIPANT", participant.getIdentity(), participant.getName()));
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        }
                    }
            );
            rvActiveList.setLayoutManager(new LinearLayoutManager(this));
            rvActiveList.setAdapter(activeParticipantAdapter);

            btnCloseWaitingList.setOnClickListener(v -> bottomSheetDialog.dismiss());

            bottomSheetDialog.setOnDismissListener(dialog -> {
                bottomSheetDialog = null;
                waitingAdapter = null;
                activeParticipantAdapter = null;
                dialogWaitingHeader = null;
                dialogWaitingList = null;
            });
        }

        boolean isHost = isHostLikeRole();
        if (isHost && !waitingParticipants.isEmpty()) {
            dialogWaitingHeader.setVisibility(View.VISIBLE);
            dialogWaitingList.setVisibility(View.VISIBLE);
        } else {
            dialogWaitingHeader.setVisibility(View.GONE);
            dialogWaitingList.setVisibility(View.GONE);
        }

        waitingAdapter.submitList(new ArrayList<>(waitingParticipants));
        activeParticipantAdapter.setParticipantList(new ArrayList<>(participantList));
        bottomSheetDialog.show();
    }

    private void processWaitingApproval(String participantId, String displayName, String action) {
        apiService.approveParticipant(meetingCode, new ApprovalRequest(participantId, action))
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        Toast.makeText(MeetingActivity.this, response.body() != null ? response.body().getMessage() : "Processed successfully", Toast.LENGTH_SHORT).show();
                        loadWaitingParticipants();
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        Toast.makeText(MeetingActivity.this, "Unable to process waiting room request", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void performUpdateMeetingSettings(Map<String, Object> settings, String successMessage) {
        apiService.updateMeetingSettings(meetingCode, settings).enqueue(new Callback<ApiResponse<com.ptithcm.ptitmeet.api.dto.meeting.MeetingResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<com.ptithcm.ptitmeet.api.dto.meeting.MeetingResponse>> call, Response<ApiResponse<com.ptithcm.ptitmeet.api.dto.meeting.MeetingResponse>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MeetingActivity.this, successMessage, Toast.LENGTH_SHORT).show();
                    loadWaitingParticipants();
                } else {
                    Toast.makeText(MeetingActivity.this, "Failed to update settings", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<com.ptithcm.ptitmeet.api.dto.meeting.MeetingResponse>> call, Throwable t) {
                Toast.makeText(MeetingActivity.this, "Network error updating settings", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showMeetingSettingsBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_meeting_settings_bottom_sheet, null);
        dialog.setContentView(view);

        com.google.android.material.materialswitch.MaterialSwitch switchWaitingRoom = view.findViewById(R.id.switchMeetingWaitingRoom);
        com.google.android.material.materialswitch.MaterialSwitch switchMuteAudio = view.findViewById(R.id.switchMuteAudioOnEntry);
        com.google.android.material.materialswitch.MaterialSwitch switchMuteVideo = view.findViewById(R.id.switchMuteVideoOnEntry);
        com.google.android.material.materialswitch.MaterialSwitch switchAllowChat = view.findViewById(R.id.switchAllowChat);
        com.google.android.material.materialswitch.MaterialSwitch switchAllowScreenShare = view.findViewById(R.id.switchAllowScreenShare);

        View layoutHostActions = view.findViewById(R.id.layoutHostActions);
        View btnMuteAll = view.findViewById(R.id.btnMuteAll);
        View btnStopCameraAll = view.findViewById(R.id.btnStopCameraAll);
        View btnKickAll = view.findViewById(R.id.btnKickAll);
        View btnOpenAppSettings = view.findViewById(R.id.btnOpenAppSettings);

        boolean isHost = isHostLikeRole();

        switchWaitingRoom.setEnabled(isHost);
        switchMuteAudio.setEnabled(isHost);
        switchMuteVideo.setEnabled(isHost);
        switchAllowChat.setEnabled(isHost);
        switchAllowScreenShare.setEnabled(isHost);

        layoutHostActions.setVisibility(isHost ? View.VISIBLE : View.GONE);

        apiService.getMeetingSettings(meetingCode).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    String json = response.body().getData();
                    currentMeetingSettings = json;
                    Map<String, Object> settingsMap = parseSettingsToMap(json);

                    switchWaitingRoom.setOnCheckedChangeListener(null);
                    switchMuteAudio.setOnCheckedChangeListener(null);
                    switchMuteVideo.setOnCheckedChangeListener(null);
                    switchAllowChat.setOnCheckedChangeListener(null);
                    switchAllowScreenShare.setOnCheckedChangeListener(null);

                    switchWaitingRoom.setChecked(Boolean.TRUE.equals(settingsMap.get("waitingRoom")));
                    switchMuteAudio.setChecked(Boolean.TRUE.equals(settingsMap.get("muteAudioOnEntry")));
                    switchMuteVideo.setChecked(Boolean.TRUE.equals(settingsMap.get("muteVideoOnEntry")));
                    switchAllowChat.setChecked(Boolean.TRUE.equals(settingsMap.get("chatEnabled")));
                    switchAllowScreenShare.setChecked(Boolean.TRUE.equals(settingsMap.get("screenShareEnabled")));

                    if (isHost) {
                        switchWaitingRoom.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            settingsMap.put("waitingRoom", isChecked);
                            performUpdateMeetingSettings(settingsMap, isChecked ? "Waiting room enabled" : "Waiting room disabled");
                        });
                        switchMuteAudio.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            settingsMap.put("muteAudioOnEntry", isChecked);
                            performUpdateMeetingSettings(settingsMap, isChecked ? "Mute audio on entry enabled" : "Mute audio on entry disabled");
                        });
                        switchMuteVideo.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            settingsMap.put("muteVideoOnEntry", isChecked);
                            performUpdateMeetingSettings(settingsMap, isChecked ? "Mute video on entry enabled" : "Mute video on entry disabled");
                        });
                        switchAllowChat.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            settingsMap.put("chatEnabled", isChecked);
                            performUpdateMeetingSettings(settingsMap, isChecked ? "Chat enabled" : "Chat disabled");
                        });
                        switchAllowScreenShare.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            settingsMap.put("screenShareEnabled", isChecked);
                            performUpdateMeetingSettings(settingsMap, isChecked ? "Screen sharing enabled" : "Screen sharing disabled");
                        });
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                Toast.makeText(MeetingActivity.this, "Failed to load settings", Toast.LENGTH_SHORT).show();
            }
        });

        if (isHost) {
            btnMuteAll.setOnClickListener(v -> {
                sendSystemAction(SystemActionHelper.createPayload("MUTE_ALL"));
                Toast.makeText(this, "Requested mute everyone", Toast.LENGTH_SHORT).show();
            });
            btnStopCameraAll.setOnClickListener(v -> {
                sendSystemAction(SystemActionHelper.createPayload("STOP_CAMERA_ALL"));
                Toast.makeText(this, "Requested disable everyone's camera", Toast.LENGTH_SHORT).show();
            });
            btnKickAll.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Kick Everyone")
                        .setMessage("Are you sure you want to kick all participants?")
                        .setPositiveButton("Yes", (dialogInterface, i) -> {
                            sendSystemAction(SystemActionHelper.createPayload("KICK_ALL"));
                            dialog.dismiss();
                        })
                        .setNegativeButton("No", null)
                        .show();
            });
        }

        btnOpenAppSettings.setOnClickListener(v -> {
            dialog.dismiss();
            showAppAudioSettingsDialog();
        });

        dialog.show();
    }

    private void showAppAudioSettingsDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_app_audio_settings, null);
        com.google.android.material.materialswitch.MaterialSwitch switchChat = view.findViewById(R.id.switchChatNotif);
        com.google.android.material.materialswitch.MaterialSwitch switchJoinLeave = view.findViewById(R.id.switchJoinLeaveNotif);
        com.google.android.material.materialswitch.MaterialSwitch switchRaiseHand = view.findViewById(R.id.switchRaiseHandNotif);
        com.google.android.material.materialswitch.MaterialSwitch switchReminder = view.findViewById(R.id.switchReminderNotif);

        switchChat.setChecked(com.ptithcm.ptitmeet.utils.SettingsManager.isChatNotifEnabled(this));
        switchJoinLeave.setChecked(com.ptithcm.ptitmeet.utils.SettingsManager.isJoinLeaveNotifEnabled(this));
        switchRaiseHand.setChecked(com.ptithcm.ptitmeet.utils.SettingsManager.isRaiseHandNotifEnabled(this));
        switchReminder.setChecked(com.ptithcm.ptitmeet.utils.SettingsManager.isReminderNotifEnabled(this));

        switchChat.setOnCheckedChangeListener((btn, isChecked) -> com.ptithcm.ptitmeet.utils.SettingsManager.setChatNotifEnabled(this, isChecked));
        switchJoinLeave.setOnCheckedChangeListener((btn, isChecked) -> com.ptithcm.ptitmeet.utils.SettingsManager.setJoinLeaveNotifEnabled(this, isChecked));
        switchRaiseHand.setOnCheckedChangeListener((btn, isChecked) -> com.ptithcm.ptitmeet.utils.SettingsManager.setRaiseHandNotifEnabled(this, isChecked));
        switchReminder.setOnCheckedChangeListener((btn, isChecked) -> com.ptithcm.ptitmeet.utils.SettingsManager.setReminderNotifEnabled(this, isChecked));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        view.findViewById(R.id.btnDone).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().getDecorView().setBackgroundColor(Color.parseColor("#0F172A"));
        }
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
        runOnUiThread(() -> {
            try {
                JSONObject jsonObject = new JSONObject(body);
                String status = jsonObject.optString("status");
                String participantId = jsonObject.optString("participantId");
                if ("LEFT".equalsIgnoreCase(status)) {
                    if (cardJoinRequest != null && cardJoinRequest.getVisibility() == View.VISIBLE) {
                        Object tag = cardJoinRequest.getTag();
                        if (tag != null && tag.equals(participantId)) {
                            cardJoinRequest.setVisibility(View.GONE);
                            toastDismissHandler.removeCallbacks(toastDismissRunnable);
                        }
                    }
                    return;
                }
                String displayName = jsonObject.optString("displayName", "User");
                showFloatingJoinRequest(participantId, displayName);
            } catch (Exception ignored) {
            }
        });
    }

    private void showFloatingJoinRequest(String participantId, String displayName) {
        if (cardJoinRequest == null) {
            return;
        }

        tvRequestName.setText(displayName);
        String initial = displayName != null && !displayName.isEmpty()
                ? displayName.substring(0, 1).toUpperCase()
                : "U";
        tvAvatarInitial.setText(initial);

        btnAcceptRequest.setOnClickListener(v -> {
            processWaitingApproval(participantId, displayName, "APPROVED");
            cardJoinRequest.setVisibility(View.GONE);
            toastDismissHandler.removeCallbacks(toastDismissRunnable);
        });

        btnDeclineRequest.setOnClickListener(v -> {
            processWaitingApproval(participantId, displayName, "REJECTED");
            cardJoinRequest.setVisibility(View.GONE);
            toastDismissHandler.removeCallbacks(toastDismissRunnable);
        });

        cardJoinRequest.setTag(participantId);
        toastDismissHandler.removeCallbacks(toastDismissRunnable);
        cardJoinRequest.setVisibility(View.VISIBLE);
        toastDismissHandler.postDelayed(toastDismissRunnable, 8000);
    }

    private void handleSystemRealtime(String body) {
        if ("MEETING_ENDED".equalsIgnoreCase(body)) {
            runOnUiThread(() -> {
                Toast.makeText(this, "The meeting has ended", Toast.LENGTH_SHORT).show();
                navigateToSummary("ENDED_BY_HOST");
            });
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(body);
            String type = jsonObject.optString("type");
            if ("SETTINGS_UPDATED".equalsIgnoreCase(type)) {
                JSONObject settingsObj = jsonObject.optJSONObject("settings");
                if (settingsObj != null) {
                    currentMeetingSettings = settingsObj.toString();
                    runOnUiThread(() -> applyUpdatedSettingsInMeeting());
                }
                return;
            }
            if ("HOST_TRANSFERRED".equalsIgnoreCase(type)) {
                String newHostId = jsonObject.optString("newHostId");
                if (newHostId != null && newHostId.equals(sessionManager.getUserId())) {
                    userRole = "HOST";
                    runOnUiThread(() -> {
                        loadWaitingParticipants();
                        subscribeRealtimeTopics();
                        Toast.makeText(this, "You have been transferred host privileges", Toast.LENGTH_SHORT).show();
                    });
                }
                return;
            }
            if ("RECORDING_STARTED".equalsIgnoreCase(type)) {
                recordingActive = true;
                runOnUiThread(() -> updateRecordingUi("RECORDING"));
                return;
            }
            if ("RECORDING_STOPPED".equalsIgnoreCase(type)) {
                recordingActive = false;
                runOnUiThread(() -> updateRecordingUi(isMeetingOwner && recordingEgressId != null ? "STOPPING" : "IDLE"));
                return;
            }
            if ("MUTE_ALL".equalsIgnoreCase(type)) {
                applyRemoteMicMute("The host muted everyone's microphone.");
                return;
            }
            if ("STOP_CAMERA_ALL".equalsIgnoreCase(type)) {
                applyRemoteCameraOff("The host disabled everyone's camera.");
                return;
            }
            if ("KICK_ALL".equalsIgnoreCase(type)) {
                if (!isHostLikeRole()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "You have been removed from the meeting", Toast.LENGTH_SHORT).show();
                        navigateToSummary("KICKED");
                    });
                }
                return;
            }

            String targetParticipantId = jsonObject.optString("targetParticipantId");
            if (targetParticipantId != null && targetParticipantId.equals(sessionManager.getUserId())) {
                if ("MUTE_PARTICIPANT".equalsIgnoreCase(type)) {
                    applyRemoteMicMute("The host muted your microphone.");
                } else if ("STOP_CAMERA_PARTICIPANT".equalsIgnoreCase(type)) {
                    applyRemoteCameraOff("The host disabled your camera.");
                } else if ("KICK_PARTICIPANT".equalsIgnoreCase(type)) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "You have been removed from the meeting by the host", Toast.LENGTH_SHORT).show();
                        navigateToSummary("KICKED");
                    });
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void handleChatRealtime(String body) {
        try {
            JSONObject jsonObject = new JSONObject(body);
            String senderId = jsonObject.optString("senderId");
            ChatMessageResponse message = new ChatMessageResponse(
                    senderId,
                    jsonObject.optString("senderName"),
                    jsonObject.optString("content")
            );
            chatMessages.add(message);
            if (chatMessageAdapter != null) {
                chatMessageAdapter.addMessage(message);
            }
            if (senderId != null && !senderId.equals(sessionManager.getUserId())) {
                MeetingSoundPlayer.playChatSound(this);
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

    private boolean isChatEnabled() {
        if (isHostLikeRole()) {
            return true;
        }
        try {
            JSONObject jsonObject = new JSONObject(currentMeetingSettings);
            return jsonObject.optBoolean("chatEnabled", true);
        } catch (Exception e) {
            return true;
        }
    }

    private void applyUpdatedSettingsInMeeting() {
        boolean chatEnabled = isChatEnabled();
        if (chatDialog != null && chatDialog.isShowing()) {
            EditText etChatMessage = chatDialog.findViewById(R.id.etChatMessage);
            View btnSendChat = chatDialog.findViewById(R.id.btnSendChat);
            if (etChatMessage != null) {
                etChatMessage.setEnabled(chatEnabled);
                etChatMessage.setHint(chatEnabled ? "Enter Message" : "Chat has been disabled by the host");
            }
            if (btnSendChat != null) {
                btnSendChat.setEnabled(chatEnabled);
            }
        }
    }

    private void openChatDialog() {
        View chatView = LayoutInflater.from(this).inflate(R.layout.dialog_chat, null, false);
        RecyclerView rvChatMessages = chatView.findViewById(R.id.rvChatMessages);
        EditText etChatMessage = chatView.findViewById(R.id.etChatMessage);
        MaterialButton btnSendChat = chatView.findViewById(R.id.btnSendChat);

        boolean chatEnabled = isChatEnabled();
        etChatMessage.setEnabled(chatEnabled);
        btnSendChat.setEnabled(chatEnabled);
        if (!chatEnabled) {
            etChatMessage.setHint("Chat has been disabled by the host");
        }

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
        if (!isChatEnabled()) {
            Toast.makeText(this, "Chat has been disabled by the host", Toast.LENGTH_SHORT).show();
            return;
        }
        if (meetingRealtimeClient == null || !meetingRealtimeClient.isConnected()) {
            Toast.makeText(this, "Chat is not ready yet", Toast.LENGTH_SHORT).show();
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



    private void sendSystemAction(String payload) {
        if (meetingRealtimeClient == null || !meetingRealtimeClient.isConnected()) {
            Toast.makeText(this, "System socket is not ready yet", Toast.LENGTH_SHORT).show();
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

        if (moreOptionsDialog != null && moreOptionsDialog.isShowing() && bottomSheetRecordLayout != null) {
            runOnUiThread(() -> {
                bottomSheetRecordLayout.setEnabled(isMeetingOwner && !recordingRequestInFlight);
                bottomSheetRecordLayout.setAlpha(isMeetingOwner ? 1f : 0.35f);
                if (recordingActive) {
                    bottomSheetRecordIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#E53935")));
                    bottomSheetRecordText.setText("Stop Recording");
                } else {
                    bottomSheetRecordIcon.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                    bottomSheetRecordText.setText("Record Meeting");
                }
            });
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
        if (isHostLikeRole()) {
            return;
        }
        runOnUiThread(() -> {
            localMicEnabled = false;
            if (liveKitRoomManager != null) {
                liveKitRoomManager.setMicrophoneEnabled(false);
            }
            updateLocalControlsUi();
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    private void applyRemoteCameraOff(String message) {
        if (isHostLikeRole()) {
            return;
        }
        runOnUiThread(() -> {
            localVideoEnabled = false;
            if (liveKitRoomManager != null) {
                liveKitRoomManager.setCameraEnabled(false);
            }
            updateLocalControlsUi();
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
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
                Toast.makeText(this, "Microphone permission is required to unmute", Toast.LENGTH_SHORT).show();
            }
        } else if (DevicePermissionHelper.REQUEST_CAMERA.equals(pendingPermissionRequest)) {
            boolean granted = Boolean.TRUE.equals(permissions.get(Manifest.permission.CAMERA));
            if (granted) {
                localVideoEnabled = true;
                updateLocalControlsUi();
            } else {
                Toast.makeText(this, "Camera permission is required to turn on video", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "LiveKit data is not available to join the media room", Toast.LENGTH_SHORT).show();
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
                        runOnUiThread(() -> Toast.makeText(MeetingActivity.this, "Joined the meeting room", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onConnectionError(String message) {
                        runOnUiThread(() -> Toast.makeText(MeetingActivity.this, message, Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onDisconnected() {
                        runOnUiThread(() -> Toast.makeText(MeetingActivity.this, "Disconnected from the meeting room", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onParticipantsUpdated(List<LiveParticipantState> participants) {
                        runOnUiThread(() -> bindParticipants(participants));
                    }

                    @Override
                    public void onReactionReceived(String senderId, String senderName, String emoji) {
                        runOnUiThread(() -> showFloatingReaction(senderName, emoji));
                    }

                    @Override
                    public void onHandRaiseReceived(String senderId, boolean isRaised) {
                        runOnUiThread(() -> {
                            if (isRaised) {
                                MeetingSoundPlayer.playHandRaiseSound(MeetingActivity.this);
                            }
                        });
                    }
                }
        );
    }

    private void disconnectLiveKitRoom() {
        if (liveKitRoomManager != null) {
            liveKitRoomManager.disconnect();
        }
    }

    private List<String> lastKnownParticipantIds = null;

    private void bindParticipants(List<LiveParticipantState> participants) {
        List<String> currentIds = new java.util.ArrayList<>();
        boolean isFirstLoad = (lastKnownParticipantIds == null);
        if (isFirstLoad) {
            lastKnownParticipantIds = new java.util.ArrayList<>();
        }

        participantList.clear();
        for (LiveParticipantState participant : participants) {
            participantList.add(new ParticipantData(
                    participant.getIdentity(),
                    participant.getIdentity(),
                    participant.getDisplayName(),
                    participant.getHasVideo(),
                    participant.isMicOn(),
                    participant.isSpeaking(),
                    participant.isLocal(),
                    participant.isHandRaised(),
                    participant.isScreenSharing()
            ));
            currentIds.add(participant.getIdentity());
        }

        updateGridSpanCount(participantList.size());

        participantAdapter.setParticipantList(participantList);
        if (activeParticipantAdapter != null) {
            activeParticipantAdapter.setParticipantList(new ArrayList<>(participantList));
        }

        if (!isFirstLoad) {
            boolean hasChange = false;
            // Check if anyone joined (is in currentIds but not in lastKnownParticipantIds)
            for (String id : currentIds) {
                if (!lastKnownParticipantIds.contains(id)) {
                    hasChange = true;
                    break;
                }
            }
            // Check if anyone left (is in lastKnownParticipantIds but not in currentIds)
            if (!hasChange) {
                for (String id : lastKnownParticipantIds) {
                    if (!currentIds.contains(id)) {
                        hasChange = true;
                        break;
                    }
                }
            }
            if (hasChange) {
                MeetingSoundPlayer.playJoinLeaveSound(this);
            }
        }

        lastKnownParticipantIds = currentIds;
    }

    private void showFloatingReaction(String senderName, String emoji) {
        View reactionView = LayoutInflater.from(this).inflate(R.layout.item_reaction_float, null);
        TextView tvName = reactionView.findViewById(R.id.tvReactionSenderName);
        TextView tvEmoji = reactionView.findViewById(R.id.tvReactionEmoji);

        tvName.setText(senderName);
        tvEmoji.setText(emoji);

        FrameLayout rootLayout = findViewById(android.R.id.content);
        if (rootLayout == null) {
            return;
        }

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int randomLeft = (int) (0.1 * screenWidth + Math.random() * (0.6 * screenWidth));
        
        lp.leftMargin = randomLeft;
        lp.topMargin = getResources().getDisplayMetrics().heightPixels - 350; // near the bottom
        lp.gravity = Gravity.TOP | Gravity.START;
        
        reactionView.setLayoutParams(lp);
        rootLayout.addView(reactionView);

        float floatDistance = -1 * (400 * getResources().getDisplayMetrics().density);
        TranslateAnimation translate = new TranslateAnimation(0, 0, 0, floatDistance);
        translate.setDuration(3000);
        translate.setFillAfter(true);

        AlphaAnimation fade = new AlphaAnimation(1.0f, 0.0f);
        fade.setDuration(3000);
        fade.setFillAfter(true);

        AnimationSet animSet = new AnimationSet(true);
        animSet.setInterpolator(new AccelerateInterpolator());
        animSet.addAnimation(translate);
        animSet.addAnimation(fade);
        
        animSet.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
            @Override
            public void onAnimationStart(android.view.animation.Animation animation) {}

            @Override
            public void onAnimationEnd(android.view.animation.Animation animation) {
                rootLayout.post(() -> rootLayout.removeView(reactionView));
            }

            @Override
            public void onAnimationRepeat(android.view.animation.Animation animation) {}
        });

        reactionView.startAnimation(animSet);
    }

    private void showMoreOptionsBottomSheet() {
        moreOptionsDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_meeting_more_options, null);
        moreOptionsDialog.setContentView(view);

        TextView btnEmojiLike = view.findViewById(R.id.btnEmojiLike);
        TextView btnEmojiLove = view.findViewById(R.id.btnEmojiLove);
        TextView btnEmojiClap = view.findViewById(R.id.btnEmojiClap);
        TextView btnEmojiLaugh = view.findViewById(R.id.btnEmojiLaugh);
        TextView btnEmojiParty = view.findViewById(R.id.btnEmojiParty);
        TextView btnEmojiShock = view.findViewById(R.id.btnEmojiShock);

        View layoutRaiseHand = view.findViewById(R.id.layoutRaiseHand);
        View layoutShareScreen = view.findViewById(R.id.layoutShareScreen);
        bottomSheetRecordLayout = view.findViewById(R.id.layoutRecordMeeting);
        bottomSheetRecordIcon = view.findViewById(R.id.ivActionRecordMeetingIcon);
        bottomSheetRecordText = view.findViewById(R.id.tvActionRecordMeetingText);
        View dividerRecordSettings = view.findViewById(R.id.dividerRecordSettings);
        View layoutAudioSettings = view.findViewById(R.id.layoutAudioSettings);

        String[] emojis = {"👍", "❤️", "👏", "😂", "🎉", "😮"};
        TextView[] emojiButtons = {btnEmojiLike, btnEmojiLove, btnEmojiClap, btnEmojiLaugh, btnEmojiParty, btnEmojiShock};
        for (int i = 0; i < emojis.length; i++) {
            final String emoji = emojis[i];
            emojiButtons[i].setOnClickListener(v -> {
                sendReaction(emoji);
                moreOptionsDialog.dismiss();
            });
        }

        layoutRaiseHand.setOnClickListener(v -> {
            toggleHandRaise();
            moreOptionsDialog.dismiss();
        });

        layoutShareScreen.setOnClickListener(v -> {
            toggleScreenSharing();
            moreOptionsDialog.dismiss();
        });

        if (isMeetingOwner) {
            bottomSheetRecordLayout.setVisibility(View.VISIBLE);
            dividerRecordSettings.setVisibility(View.VISIBLE);
            bottomSheetRecordLayout.setOnClickListener(v -> {
                toggleRecording();
                moreOptionsDialog.dismiss();
            });
        } else {
            bottomSheetRecordLayout.setVisibility(View.GONE);
            dividerRecordSettings.setVisibility(View.GONE);
        }

        layoutAudioSettings.setOnClickListener(v -> {
            moreOptionsDialog.dismiss();
            showAppAudioSettingsDialog();
        });

        updateMoreOptionsBottomSheetUi(view);

        moreOptionsDialog.setOnDismissListener(dialog -> {
            moreOptionsDialog = null;
            bottomSheetRecordLayout = null;
            bottomSheetRecordIcon = null;
            bottomSheetRecordText = null;
        });

        moreOptionsDialog.show();
    }

    private void updateMoreOptionsBottomSheetUi() {
        if (moreOptionsDialog != null && moreOptionsDialog.isShowing()) {
            View view = moreOptionsDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (view != null) {
                updateMoreOptionsBottomSheetUi(view);
            }
        }
    }

    private void updateMoreOptionsBottomSheetUi(View view) {
        ImageView ivActionRaiseHandIcon = view.findViewById(R.id.ivActionRaiseHandIcon);
        TextView tvActionRaiseHandText = view.findViewById(R.id.tvActionRaiseHandText);
        ImageView ivActionShareScreenIcon = view.findViewById(R.id.ivActionShareScreenIcon);
        TextView tvActionShareScreenText = view.findViewById(R.id.tvActionShareScreenText);

        if (ivActionRaiseHandIcon != null && tvActionRaiseHandText != null) {
            if (isLocalHandRaised) {
                ivActionRaiseHandIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FFA000")));
                tvActionRaiseHandText.setText("Lower Hand");
            } else {
                ivActionRaiseHandIcon.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                tvActionRaiseHandText.setText("Raise Hand");
            }
        }

        if (ivActionShareScreenIcon != null && tvActionShareScreenText != null) {
            if (isLocalScreenSharing) {
                ivActionShareScreenIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#3B82F6")));
                tvActionShareScreenText.setText("Stop Screen Share");
            } else {
                ivActionShareScreenIcon.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                tvActionShareScreenText.setText("Share Screen");
            }
        }

        if (bottomSheetRecordLayout != null && bottomSheetRecordIcon != null && bottomSheetRecordText != null) {
            bottomSheetRecordLayout.setEnabled(isMeetingOwner && !recordingRequestInFlight);
            bottomSheetRecordLayout.setAlpha(isMeetingOwner ? 1f : 0.35f);
            if (recordingActive) {
                bottomSheetRecordIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#E53935")));
                bottomSheetRecordText.setText("Stop Recording");
            } else {
                bottomSheetRecordIcon.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                bottomSheetRecordText.setText("Record Meeting");
            }
        }
    }

    private void toggleHandRaise() {
        isLocalHandRaised = !isLocalHandRaised;
        if (liveKitRoomManager != null) {
            liveKitRoomManager.publishHandRaise(isLocalHandRaised);
        }
        if (isLocalHandRaised) {
            Toast.makeText(this, "You raised your hand", Toast.LENGTH_SHORT).show();
            MeetingSoundPlayer.playHandRaiseSound(this);
        } else {
            Toast.makeText(this, "You lowered your hand", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendReaction(String emoji) {
        if (liveKitRoomManager != null) {
            String userId = sessionManager.getUserId() != null ? sessionManager.getUserId() : "self";
            String userName = sessionManager.getUserName() != null ? sessionManager.getUserName() : "You";
            liveKitRoomManager.publishReaction(emoji, userId, userName);
        }
        showFloatingReaction("You", emoji);
    }

    private void toggleScreenSharing() {
        boolean screenShareAllowed = isHostLikeRole();
        if (!screenShareAllowed) {
            try {
                JSONObject jsonObject = new JSONObject(currentMeetingSettings);
                screenShareAllowed = jsonObject.optBoolean("screenShareEnabled", true);
            } catch (Exception e) {
                screenShareAllowed = true;
            }
        }

        if (!screenShareAllowed) {
            Toast.makeText(this, "Screen sharing is disabled by the host", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isLocalScreenSharing) {
            isLocalScreenSharing = false;
            if (liveKitRoomManager != null) {
                liveKitRoomManager.setScreenShareEnabled(false, null);
            }
            Toast.makeText(this, "Screen sharing stopped", Toast.LENGTH_SHORT).show();
            updateMoreOptionsBottomSheetUi();
        } else {
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if (mediaProjectionManager != null) {
                Intent intent = mediaProjectionManager.createScreenCaptureIntent();
                screenShareLauncher.launch(intent);
            }
        }
    }

    private void updateGridSpanCount(int itemCount) {
        if (rvParticipants == null) return;
        RecyclerView.LayoutManager lm = rvParticipants.getLayoutManager();
        if (lm instanceof GridLayoutManager) {
            GridLayoutManager glm = (GridLayoutManager) lm;
            int newSpanCount = (itemCount <= 2) ? 1 : 2;
            if (glm.getSpanCount() != newSpanCount) {
                glm.setSpanCount(newSpanCount);
            }
        }
    }
}
