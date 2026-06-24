package com.ptithcm.ptitmeet.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.adapters.ChatMessageAdapter;
import com.ptithcm.ptitmeet.adapters.MeetingHistoryAdapter;
import com.ptithcm.ptitmeet.api.dto.chat.ChatMessageResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.JoinMeetingResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingHistoryResponse;
import com.ptithcm.ptitmeet.viewmodel.MeetingsUiEvent;
import com.ptithcm.ptitmeet.viewmodel.MeetingsUiState;
import com.ptithcm.ptitmeet.viewmodel.MeetingsViewModel;

import java.util.List;

public class MeetingsActivity extends AppCompatActivity implements MeetingHistoryAdapter.OnMeetingActionListener {

    private TextView tvSummary, tvPageNum;
    private Spinner spRoleFilter, spStatusFilter;
    private RecyclerView rvMeetings;
    private ProgressBar progressMeetings;
    private AppCompatButton btnPrevPage, btnNextPage, btnRefresh;

    private MeetingsViewModel viewModel;
    private MeetingHistoryAdapter adapter;

    private int currentPage = 1; // 1-indexed for display, mapped to currentPage - 1 for API
    private int totalPages = 1;
    private final int pageSize = 6;

    private final String[] roleValues = {"ALL", "HOST", "GUEST"};
    private final String[] statusValues = {"ALL", "ACTIVE", "SCHEDULED", "FINISHED", "CANCELED"};

    private boolean isInitialLoading = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meetings);

        viewModel = new ViewModelProvider(this).get(MeetingsViewModel.class);

        tvSummary = findViewById(R.id.tvSummary);
        tvPageNum = findViewById(R.id.tvPageNum);
        spRoleFilter = findViewById(R.id.spRoleFilter);
        spStatusFilter = findViewById(R.id.spStatusFilter);
        rvMeetings = findViewById(R.id.rvMeetings);
        progressMeetings = findViewById(R.id.progressMeetings);
        btnPrevPage = findViewById(R.id.btnPrevPage);
        btnNextPage = findViewById(R.id.btnNextPage);
        btnRefresh = findViewById(R.id.btnRefresh);

        adapter = new MeetingHistoryAdapter(this);
        rvMeetings.setLayoutManager(new LinearLayoutManager(this));
        rvMeetings.setAdapter(adapter);

        String[] roleLabels = {"All roles", "Host", "Guest"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_selected, roleLabels);
        roleAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spRoleFilter.setAdapter(roleAdapter);

        String[] statusLabels = {"All statuses", "Live", "Scheduled", "Completed", "Canceled"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_selected, statusLabels);
        statusAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spStatusFilter.setAdapter(statusAdapter);

        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isInitialLoading) {
                    return;
                }
                currentPage = 1;
                loadMeetings();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        spRoleFilter.setOnItemSelectedListener(spinnerListener);
        spStatusFilter.setOnItemSelectedListener(spinnerListener);

        btnRefresh.setOnClickListener(v -> loadMeetings());
        btnPrevPage.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                loadMeetings();
            }
        });
        btnNextPage.setOnClickListener(v -> {
            if (currentPage < totalPages) {
                currentPage++;
                loadMeetings();
            }
        });

        setupBottomNavigation();

        viewModel.getUiState().observe(this, this::applyState);
        viewModel.getUiEvent().observe(this, event -> {
            if (event == null) {
                return;
            }
            MeetingsUiEvent uiEvent = event.getContentIfNotHandled();
            if (uiEvent == null) {
                return;
            }
            handleEvent(uiEvent);
        });

        isInitialLoading = false;
        loadMeetings();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_meetings);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_meetings) {
                return true;
            }
            if (id == R.id.nav_dashboard) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            }
            if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
                return true;
            }
            if (id == R.id.nav_recordings) {
                startActivity(new Intent(this, RecordingsActivity.class));
                finish();
                return true;
            }
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void loadMeetings() {
        String role = roleValues[spRoleFilter.getSelectedItemPosition()];
        String status = statusValues[spStatusFilter.getSelectedItemPosition()];
        viewModel.loadMeetings(currentPage, pageSize, role, status);
    }

    private void setLoading(boolean loading) {
        progressMeetings.setVisibility(loading ? View.VISIBLE : View.GONE);
        rvMeetings.setVisibility(loading ? View.GONE : View.VISIBLE);
        btnRefresh.setEnabled(!loading);
    }

    // OnMeetingActionListener Implementations

    @Override
    public void onJoin(MeetingHistoryResponse meeting) {
        Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show();
        viewModel.joinMeeting(meeting);
    }

    private void openWaitingRoom(String meetingCode, String displayName, String waitingMessage) {
        Intent intent = new Intent(this, WaitingRoomActivity.class);
        intent.putExtra("MEETING_CODE", meetingCode);
        intent.putExtra("DISPLAY_NAME", displayName);
        intent.putExtra("WAITING_MESSAGE", waitingMessage);
        startActivity(intent);
    }

    private void openMeetingRoom(String meetingCode, JoinMeetingResponse joinData) {
        Intent intent = new Intent(this, MeetingActivity.class);
        intent.putExtra("LIVEKIT_TOKEN", joinData.getToken());
        intent.putExtra("LIVEKIT_URL", joinData.getServerUrl());
        intent.putExtra("USER_ROLE", joinData.getRole());
        intent.putExtra("MEETING_CODE", meetingCode);
        intent.putExtra("IS_OWNER", joinData.isOwner());

        boolean micOn = true;
        boolean videoOn = true;
        boolean isHost = "HOST".equalsIgnoreCase(joinData.getRole()) || joinData.isOwner();
        if (!isHost && joinData.getSettings() != null) {
            try {
                org.json.JSONObject settingsObj = new org.json.JSONObject(joinData.getSettings());
                if (settingsObj.optBoolean("muteAudioOnEntry", false)) {
                    micOn = false;
                }
                if (settingsObj.optBoolean("muteVideoOnEntry", false)) {
                    videoOn = false;
                }
            } catch (Exception ignored) {}
        }

        intent.putExtra("MIC_ON", micOn);
        intent.putExtra("VIDEO_ON", videoOn);
        startActivity(intent);
    }

    @Override
    public void onCancel(MeetingHistoryResponse meeting) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel meeting")
                .setMessage("Cancel \"" + meeting.getTitle() + "\"? This action cannot be undone.")
                .setPositiveButton("Cancel meeting", (dialog, which) -> {
                    Toast.makeText(this, "Canceling meeting...", Toast.LENGTH_SHORT).show();
                    String role = roleValues[spRoleFilter.getSelectedItemPosition()];
                    String status = statusValues[spStatusFilter.getSelectedItemPosition()];
                    viewModel.cancelMeeting(meeting, currentPage, pageSize, role, status);
                })
                .setNegativeButton("Keep it", null)
                .show();
    }

    @Override
    public void onViewChat(MeetingHistoryResponse meeting) {
        Toast.makeText(this, "Loading chat history...", Toast.LENGTH_SHORT).show();
        viewModel.loadChatHistory(meeting);
    }

    private void showChatHistoryDialog(String meetingTitle, List<ChatMessageResponse> chatList) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_chat_history, null, false);
        TextView tvTitle = dialogView.findViewById(R.id.tvChatHistoryTitle);
        TextView tvSubtitle = dialogView.findViewById(R.id.tvChatHistorySubtitle);
        TextView tvEmpty = dialogView.findViewById(R.id.tvChatHistoryEmpty);
        RecyclerView rvChatHistory = dialogView.findViewById(R.id.rvChatHistory);

        tvTitle.setText(meetingTitle != null && !meetingTitle.trim().isEmpty() ? meetingTitle : "Meeting chat");
        tvSubtitle.setText(chatList == null || chatList.isEmpty()
                ? "No saved messages from this meeting."
                : "Messages shared during the meeting.");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (chatList == null || chatList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvChatHistory.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvChatHistory.setLayoutManager(new LinearLayoutManager(this));
            ChatMessageAdapter chatAdapter = new ChatMessageAdapter(null);
            chatAdapter.submitList(chatList);
            rvChatHistory.setAdapter(chatAdapter);
        }

        dialogView.findViewById(R.id.btnCloseChatHistory).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void applyState(MeetingsUiState state) {
        if (state == null) {
            return;
        }
        currentPage = state.getCurrentPage();
        totalPages = state.getTotalPages();
        setLoading(state.isLoading());
        tvSummary.setText(state.getSummaryText());
        tvPageNum.setText("Page " + currentPage + " of " + totalPages);
        btnPrevPage.setEnabled(currentPage > 1 && !state.isLoading());
        btnNextPage.setEnabled(currentPage < totalPages && !state.isLoading());
        adapter.submitList(state.getMeetings().isEmpty() ? null : state.getMeetings());
    }

    private void handleEvent(MeetingsUiEvent event) {
        switch (event.getType()) {
            case MeetingsUiEvent.SHOW_TOAST:
                Toast.makeText(this, event.getMessage(), Toast.LENGTH_SHORT).show();
                break;
            case MeetingsUiEvent.OPEN_WAITING_ROOM:
                openWaitingRoom(event.getMeetingCode(), event.getDisplayName(), event.getWaitingMessage());
                break;
            case MeetingsUiEvent.OPEN_MEETING_ROOM:
                openMeetingRoom(event.getMeetingCode(), event.getJoinMeetingResponse());
                break;
            case MeetingsUiEvent.SHOW_CHAT_HISTORY:
                showChatHistoryDialog(event.getMeetingTitle(), event.getChatHistory());
                break;
            default:
                break;
        }
    }
}
