package com.ptithcm.ptitmeet.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.adapters.RecentActivityAdapter;
import com.ptithcm.ptitmeet.api.SessionManager;
import com.ptithcm.ptitmeet.api.dto.common.ApiResponse;
import com.ptithcm.ptitmeet.api.dto.common.PageResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.CreateMeetingRequest;
import com.ptithcm.ptitmeet.api.dto.meeting.JoinMeetingRequest;
import com.ptithcm.ptitmeet.api.dto.meeting.JoinMeetingResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingHistoryResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingResponse;
import com.ptithcm.ptitmeet.api.services.ApiService;
import com.ptithcm.ptitmeet.api.services.RetrofitClient;
import com.ptithcm.ptitmeet.utils.MeetingUiFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private TextView tvUpNextTitle;
    private TextView tvUpNextTime;
    private TextView tvUpNextCode;
    private LinearLayout btnNewMeeting, btnJoinMeeting, btnScheduleMeeting, btnMeetingHistory;
    private EditText etMeetingCode;
    private AppCompatButton btnJoinNow;
    private RecyclerView rvRecentActivity;
    private SessionManager sessionManager;
    private ApiService apiService;
    private RecentActivityAdapter recentActivityAdapter;
    private String upNextMeetingCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getApiService(this);

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
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        recentActivityAdapter = new RecentActivityAdapter();
        rvRecentActivity.setLayoutManager(new LinearLayoutManager(this));
        rvRecentActivity.setAdapter(recentActivityAdapter);

        String fullName = sessionManager.getUserName();
        if (tvWelcome != null) {
            tvWelcome.setText("Welcome back, " + fullName);
        }

        btnNewMeeting.setOnClickListener(v -> createNewMeeting());
        btnJoinMeeting.setOnClickListener(v -> handleJoinFromInputOrDialog());
        btnScheduleMeeting.setOnClickListener(v -> startActivity(new Intent(this, ScheduleMeetingActivity.class)));
        btnMeetingHistory.setOnClickListener(v -> startActivity(new Intent(this, MeetingsActivity.class)));
        btnJoinNow.setOnClickListener(v -> joinUpNextMeeting());

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void createNewMeeting() {
        btnNewMeeting.setEnabled(false);
        Toast.makeText(this, "Đang khởi tạo phòng họp...", Toast.LENGTH_SHORT).show();

        String fullName = sessionManager.getUserName();
        CreateMeetingRequest request = new CreateMeetingRequest("Phòng họp của " + fullName);

        String nowPlus1Min = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .format(new Date(System.currentTimeMillis() + 60000));
        request.setStartTime(nowPlus1Min);

        apiService.createInstantMeeting(request).enqueue(new Callback<ApiResponse<MeetingResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<MeetingResponse>> call, Response<ApiResponse<MeetingResponse>> response) {
                btnNewMeeting.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    String newMeetingCode = response.body().getData().getMeetingCode();
                    performJoinRequest(newMeetingCode);
                } else {
                    String errorMessage = response.body() != null ? response.body().getMessage() : "Không thể tạo phòng";
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MeetingResponse>> call, Throwable t) {
                btnNewMeeting.setEnabled(true);
                Toast.makeText(MainActivity.this, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleJoinFromInputOrDialog() {
        String meetingCode = etMeetingCode.getText().toString().trim();
        if (!meetingCode.isEmpty()) {
            performJoinRequest(meetingCode);
            return;
        }
        showJoinMeetingDialog();
    }

    private void showJoinMeetingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tham gia cuộc họp");

        final EditText input = new EditText(this);
        input.setHint("Nhập mã phòng (VD: abc-xyz)");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Tham gia", (dialog, which) -> {
            String meetingCode = input.getText().toString().trim();
            if (!meetingCode.isEmpty()) {
                performJoinRequest(meetingCode);
            } else {
                Toast.makeText(MainActivity.this, "Mã phòng không được để trống", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void joinUpNextMeeting() {
        if (upNextMeetingCode == null || upNextMeetingCode.trim().isEmpty()) {
            Toast.makeText(this, "Hiện chưa có cuộc họp sắp diễn ra", Toast.LENGTH_SHORT).show();
            return;
        }
        performJoinRequest(upNextMeetingCode);
    }

    private void loadDashboardData() {
        loadUpNextMeeting();
        loadRecentActivity();
    }

    private void loadUpNextMeeting() {
        apiService.getUpNextMeeting().enqueue(new Callback<ApiResponse<MeetingHistoryResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<MeetingHistoryResponse>> call, Response<ApiResponse<MeetingHistoryResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    bindUpNext(response.body().getData());
                    return;
                }
                showEmptyUpNext();
            }

            @Override
            public void onFailure(Call<ApiResponse<MeetingHistoryResponse>> call, Throwable t) {
                showEmptyUpNext();
            }
        });
    }

    private void loadRecentActivity() {
        apiService.getMeetingHistory(1, 10, "ALL", "ALL")
                .enqueue(new Callback<ApiResponse<PageResponse<MeetingHistoryResponse>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<PageResponse<MeetingHistoryResponse>>> call, Response<ApiResponse<PageResponse<MeetingHistoryResponse>>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            List<MeetingHistoryResponse> items = response.body().getData().getContent();
                            recentActivityAdapter.submitList(items);
                            return;
                        }
                        recentActivityAdapter.submitList(null);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<PageResponse<MeetingHistoryResponse>>> call, Throwable t) {
                        recentActivityAdapter.submitList(null);
                    }
                });
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
        tvUpNextTitle.setText("Chưa có cuộc họp sắp tới");
        tvUpNextTime.setText("Bạn có thể tạo phòng mới hoặc nhập mã để tham gia.");
        tvUpNextCode.setText("Meeting code: ---");
        btnJoinNow.setEnabled(false);
    }

    private void performJoinRequest(String meetingCode) {
        Toast.makeText(this, "Đang kết nối...", Toast.LENGTH_SHORT).show();

        String displayName = sessionManager.getUserName();
        JoinMeetingRequest request = new JoinMeetingRequest(null, displayName);

        apiService.joinMeeting(meetingCode, request).enqueue(new Callback<ApiResponse<JoinMeetingResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<JoinMeetingResponse>> call, Response<ApiResponse<JoinMeetingResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    JoinMeetingResponse joinData = response.body().getData();
                    if ("PENDING".equalsIgnoreCase(joinData.getStatus())) {
                        openWaitingRoom(meetingCode, displayName, joinData.getMessage());
                    } else {
                        openMeetingRoom(meetingCode, joinData);
                    }
                } else {
                    String errorMsg = response.body() != null ? response.body().getMessage() : "Lỗi không xác định";
                    Toast.makeText(MainActivity.this, "Không thể tham gia: " + errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<JoinMeetingResponse>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Lỗi kết nối mạng", Toast.LENGTH_SHORT).show();
            }
        });
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
        startActivity(intent);
    }
}
