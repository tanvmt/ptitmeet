package com.ptithcm.ptitmeet.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.adapters.MeetingHistoryAdapter;
import com.ptithcm.ptitmeet.api.SessionManager;
import com.ptithcm.ptitmeet.api.dto.chat.ChatMessageResponse;
import com.ptithcm.ptitmeet.api.dto.common.ApiResponse;
import com.ptithcm.ptitmeet.api.dto.common.PageResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.JoinMeetingRequest;
import com.ptithcm.ptitmeet.api.dto.meeting.JoinMeetingResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingHistoryResponse;
import com.ptithcm.ptitmeet.api.services.ApiService;
import com.ptithcm.ptitmeet.api.services.RetrofitClient;
import com.ptithcm.ptitmeet.utils.MeetingUiFormatter;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MeetingsActivity extends AppCompatActivity implements MeetingHistoryAdapter.OnMeetingActionListener {

    private TextView tvSummary, tvPageNum;
    private Spinner spRoleFilter, spStatusFilter;
    private RecyclerView rvMeetings;
    private ProgressBar progressMeetings;
    private AppCompatButton btnPrevPage, btnNextPage, btnRefresh;

    private ApiService apiService;
    private SessionManager sessionManager;
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

        apiService = RetrofitClient.getApiService(this);
        sessionManager = new SessionManager(this);

        // Bind Views
        tvSummary = findViewById(R.id.tvSummary);
        tvPageNum = findViewById(R.id.tvPageNum);
        spRoleFilter = findViewById(R.id.spRoleFilter);
        spStatusFilter = findViewById(R.id.spStatusFilter);
        rvMeetings = findViewById(R.id.rvMeetings);
        progressMeetings = findViewById(R.id.progressMeetings);
        btnPrevPage = findViewById(R.id.btnPrevPage);
        btnNextPage = findViewById(R.id.btnNextPage);
        btnRefresh = findViewById(R.id.btnRefresh);

        // Setup RecyclerView
        adapter = new MeetingHistoryAdapter(this);
        rvMeetings.setLayoutManager(new LinearLayoutManager(this));
        rvMeetings.setAdapter(adapter);

        // Spinners Setup
        String[] roleLabels = {"Tất cả vai trò", "Chủ phòng (Host)", "Khách mời (Guest)"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roleLabels);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRoleFilter.setAdapter(roleAdapter);

        String[] statusLabels = {"Tất cả trạng thái", "Đang diễn ra (Live)", "Sắp diễn ra", "Đã kết thúc", "Đã hủy"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statusLabels);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStatusFilter.setAdapter(statusAdapter);

        // Setup Spinners Listeners
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

        // Buttons Listeners
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

        // Initial Data Load
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
        setLoading(true);

        String role = roleValues[spRoleFilter.getSelectedItemPosition()];
        String status = statusValues[spStatusFilter.getSelectedItemPosition()];

        apiService.getMeetingHistory(currentPage, pageSize, role, status)
                .enqueue(new Callback<ApiResponse<PageResponse<MeetingHistoryResponse>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<PageResponse<MeetingHistoryResponse>>> call,
                                           Response<ApiResponse<PageResponse<MeetingHistoryResponse>>> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            PageResponse<MeetingHistoryResponse> pageData = response.body().getData();

                            totalPages = pageData.getTotalPages();
                            if (totalPages < 1) totalPages = 1;

                            tvPageNum.setText("Trang " + currentPage + " / " + totalPages);
                            btnPrevPage.setEnabled(currentPage > 1);
                            btnNextPage.setEnabled(currentPage < totalPages);

                            List<MeetingHistoryResponse> content = pageData.getContent();
                            if (content == null || content.isEmpty()) {
                                adapter.submitList(null);
                                tvSummary.setText("Không có cuộc họp nào phù hợp");
                            } else {
                                adapter.submitList(content);
                                tvSummary.setText("Tìm thấy " + pageData.getTotalElements() + " cuộc họp");
                            }
                        } else {
                            adapter.submitList(null);
                            tvSummary.setText("Không thể tải danh sách, vui lòng thử lại");
                            tvPageNum.setText("Trang 1 / 1");
                            btnPrevPage.setEnabled(false);
                            btnNextPage.setEnabled(false);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<PageResponse<MeetingHistoryResponse>>> call, Throwable t) {
                        setLoading(false);
                        adapter.submitList(null);
                        tvSummary.setText("Lỗi kết nối máy chủ");
                        btnPrevPage.setEnabled(false);
                        btnNextPage.setEnabled(false);
                    }
                });
    }

    private void setLoading(boolean loading) {
        progressMeetings.setVisibility(loading ? View.VISIBLE : View.GONE);
        rvMeetings.setVisibility(loading ? View.GONE : View.VISIBLE);
        btnRefresh.setEnabled(!loading);
    }

    // OnMeetingActionListener Implementations

    @Override
    public void onJoin(MeetingHistoryResponse meeting) {
        Toast.makeText(this, "Đang kết nối...", Toast.LENGTH_SHORT).show();

        String meetingCode = meeting.getMeetingCode();
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
                    Toast.makeText(MeetingsActivity.this, "Không thể tham gia: " + errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<JoinMeetingResponse>> call, Throwable t) {
                Toast.makeText(MeetingsActivity.this, "Lỗi kết nối mạng", Toast.LENGTH_SHORT).show();
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
                .setTitle("Hủy cuộc họp")
                .setMessage("Bạn có chắc chắn muốn hủy cuộc họp \"" + meeting.getTitle() + "\"? Hành động này không thể hoàn tác.")
                .setPositiveButton("Hủy họp", (dialog, which) -> {
                    Toast.makeText(this, "Đang xử lý hủy cuộc họp...", Toast.LENGTH_SHORT).show();
                    apiService.cancelMeeting(meeting.getMeetingCode()).enqueue(new Callback<ApiResponse<Void>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(MeetingsActivity.this, "Đã hủy cuộc họp thành công", Toast.LENGTH_SHORT).show();
                                loadMeetings();
                            } else {
                                String error = response.body() != null ? response.body().getMessage() : "Không thể hủy cuộc họp";
                                Toast.makeText(MeetingsActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                            Toast.makeText(MeetingsActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Quay lại", null)
                .show();
    }

    @Override
    public void onViewChat(MeetingHistoryResponse meeting) {
        Toast.makeText(this, "Đang tải lịch sử trò chuyện...", Toast.LENGTH_SHORT).show();

        apiService.getChatHistory(meeting.getMeetingCode()).enqueue(new Callback<ApiResponse<List<ChatMessageResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ChatMessageResponse>>> call, Response<ApiResponse<List<ChatMessageResponse>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    showChatHistoryDialog(meeting.getTitle(), response.body().getData());
                } else {
                    Toast.makeText(MeetingsActivity.this, "Không thể tải lịch sử chat", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<ChatMessageResponse>>> call, Throwable t) {
                Toast.makeText(MeetingsActivity.this, "Lỗi mạng, vui lòng thử lại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showChatHistoryDialog(String meetingTitle, List<ChatMessageResponse> chatList) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(meetingTitle != null ? meetingTitle : "Lịch sử trò chuyện");

        if (chatList == null || chatList.isEmpty()) {
            builder.setMessage("Không có tin nhắn nào được lưu cho cuộc họp này.");
            builder.setPositiveButton("Đóng", null);
            builder.show();
            return;
        }

        // Dynamically build list inside ScrollView
        ScrollView scrollView = new ScrollView(this);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(24, 24, 24, 24);
        scrollView.addView(linearLayout);

        for (ChatMessageResponse msg : chatList) {
            View item = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_2, linearLayout, false);
            TextView text1 = item.findViewById(android.R.id.text1);
            TextView text2 = item.findViewById(android.R.id.text2);

            String formattedTime = MeetingUiFormatter.formatDateTime(msg.getTimestamp());
            text1.setText(msg.getSenderName() + " (" + formattedTime + ")");
            text1.setTextColor(Color.parseColor("#FB923C")); // nice orange accent
            text1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);

            text2.setText(msg.getContent());
            text2.setTextColor(Color.parseColor("#E2E8F0")); // slate white
            text2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            text2.setPadding(0, 4, 0, 16);

            linearLayout.addView(item);
        }

        builder.setView(scrollView);
        builder.setPositiveButton("Đóng", null);
        AlertDialog dialog = builder.create();
        dialog.show();

        // Style dialog background to match app dark style if possible
        if (dialog.getWindow() != null) {
            dialog.getWindow().getDecorView().setBackgroundColor(Color.parseColor("#1E293B"));
        }
    }
}
