package com.ptithcm.ptitmeet.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.ptithcm.ptitmeet.adapters.RecentActivityAdapter;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingHistoryResponse;
import com.ptithcm.ptitmeet.utils.MeetingUiFormatter;
import com.ptithcm.ptitmeet.viewmodel.MainUiEvent;
import com.ptithcm.ptitmeet.viewmodel.MainUiState;
import com.ptithcm.ptitmeet.viewmodel.MainViewModel;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private TextView tvUpNextTitle;
    private TextView tvUpNextTime;
    private TextView tvUpNextCode;
    private LinearLayout btnNewMeeting, btnJoinMeeting, btnScheduleMeeting, btnMeetingHistory;
    private EditText etMeetingCode;
    private AppCompatButton btnJoinNow;
    private RecyclerView rvRecentActivity;
    private RecentActivityAdapter recentActivityAdapter;
    private String upNextMeetingCode;
    private MainViewModel viewModel;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        tvWelcome = findViewById(R.id.tvWelcome);
        tvUpNextTitle = findViewById(R.id.tvUpNextTitle);
        tvUpNextTime = findViewById(R.id.tvUpNextTime);
        tvUpNextCode = findViewById(R.id.tvUpNextCode);
        btnNewMeeting = findViewById(R.id.btnNewMeeting);
        btnJoinMeeting = findViewById(R.id.btnJoinMeeting);
        btnScheduleMeeting = findViewById(R.id.btnScheduleMeeting);
        btnMeetingHistory = findViewById(R.id.btnMeetingHistory);
        etMeetingCode = findViewById(R.id.etMeetingCode);
        btnJoinNow = findViewById(R.id.btnJoinNow);
        rvRecentActivity = findViewById(R.id.rvRecentActivity);
        bottomNav = findViewById(R.id.bottomNav);

        recentActivityAdapter = new RecentActivityAdapter();
        rvRecentActivity.setLayoutManager(new LinearLayoutManager(this));
        rvRecentActivity.setAdapter(recentActivityAdapter);

        btnNewMeeting.setOnClickListener(v -> {
            Toast.makeText(this, "Creating meeting...", Toast.LENGTH_SHORT).show();
            viewModel.createNewMeeting();
        });
        btnJoinMeeting.setOnClickListener(v -> handleJoinFromInputOrDialog());
        btnScheduleMeeting.setOnClickListener(v -> startActivity(new Intent(this, ScheduleMeetingActivity.class)));
        btnMeetingHistory.setOnClickListener(v -> startActivity(new Intent(this, MeetingsActivity.class)));
        btnJoinNow.setOnClickListener(v -> viewModel.joinUpNextMeeting());

        bottomNav.setSelectedItemId(R.id.nav_dashboard);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                return true;
            }
            if (id == R.id.nav_meetings) {
                startActivity(new Intent(this, MeetingsActivity.class));
                return true;
            }
            if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            if (id == R.id.nav_recordings) {
                startActivity(new Intent(this, RecordingsActivity.class));
                return true;
            }
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });

        viewModel.getUiState().observe(this, this::applyState);
        viewModel.getUiEvent().observe(this, event -> {
            if (event == null) {
                return;
            }
            MainUiEvent uiEvent = event.getContentIfNotHandled();
            if (uiEvent == null) {
                return;
            }
            handleEvent(uiEvent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
        }
        viewModel.loadDashboardData();
    }

    private void handleJoinFromInputOrDialog() {
        String meetingCode = etMeetingCode.getText().toString().trim();
        if (!meetingCode.isEmpty()) {
            viewModel.joinMeeting(meetingCode);
            return;
        }
        showJoinMeetingDialog();
    }

    private void showJoinMeetingDialog() {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_join_meeting, null, false);
        EditText input = dialogView.findViewById(R.id.etJoinMeetingCode);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btnCancelJoinMeeting).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnConfirmJoinMeeting).setOnClickListener(v -> {
            String meetingCode = input.getText().toString().trim();
            if (!meetingCode.isEmpty()) {
                viewModel.joinMeeting(meetingCode);
                dialog.dismiss();
            } else {
                Toast.makeText(MainActivity.this, "Meeting code cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    private void bindUpNext(MeetingHistoryResponse meeting) {
        upNextMeetingCode = meeting.getMeetingCode();
        tvUpNextTitle.setText(meeting.getTitle());
        tvUpNextTime.setText(MeetingUiFormatter.formatTimeRange(meeting.getStartTime(), meeting.getEndTime()));
        tvUpNextCode.setText("Meeting code: " + meeting.getMeetingCode());
        btnJoinNow.setEnabled(true);
    }

    private void showEmptyUpNext() {
        upNextMeetingCode = null;
        tvUpNextTitle.setText("No upcoming meetings");
        tvUpNextTime.setText("Create a room or join one with a meeting code.");
        tvUpNextCode.setText("Meeting code: ---");
        btnJoinNow.setEnabled(false);
    }

    private void openWaitingRoom(String meetingCode, String displayName, boolean isHostSetup) {
        Intent intent = new Intent(this, WaitingRoomActivity.class);
        intent.putExtra("MEETING_CODE", meetingCode);
        intent.putExtra("DISPLAY_NAME", displayName);
        intent.putExtra("IS_HOST_SETUP", isHostSetup);
        startActivity(intent);
    }

    private void applyState(MainUiState state) {
        if (state == null) {
            return;
        }
        tvWelcome.setText(state.getWelcomeText());
        btnNewMeeting.setEnabled(!state.isCreatingMeeting());
        MeetingHistoryResponse upNextMeeting = state.getUpNextMeeting();
        if (upNextMeeting != null) {
            bindUpNext(upNextMeeting);
        } else {
            showEmptyUpNext();
        }
        List<MeetingHistoryResponse> items = state.getRecentActivity();
        recentActivityAdapter.submitList(items == null || items.isEmpty() ? null : items);
    }

    private void handleEvent(MainUiEvent event) {
        switch (event.getType()) {
            case MainUiEvent.SHOW_TOAST:
                Toast.makeText(this, event.getMessage(), Toast.LENGTH_SHORT).show();
                break;
            case MainUiEvent.OPEN_WAITING_ROOM:
                openWaitingRoom(event.getMeetingCode(), event.getDisplayName(), event.isHostSetup());
                break;
            default:
                break;
        }
    }
}
