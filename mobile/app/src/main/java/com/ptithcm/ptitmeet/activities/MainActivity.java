package com.ptithcm.ptitmeet.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.api.SessionManager;
import com.ptithcm.ptitmeet.api.dto.ApiResponse;
import com.ptithcm.ptitmeet.api.dto.CreateMeetingRequest;
import com.ptithcm.ptitmeet.api.dto.JoinMeetingRequest;
import com.ptithcm.ptitmeet.api.dto.JoinMeetingResponse;
import com.ptithcm.ptitmeet.api.dto.MeetingInfoResponse;
import com.ptithcm.ptitmeet.api.services.ApiService;
import com.ptithcm.ptitmeet.api.services.RetrofitClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private LinearLayout btnNewMeeting, btnJoinMeeting;
    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getApiService(this);

        // Ánh xạ View
        tvWelcome = findViewById(R.id.tvWelcome); // Nhớ thêm ID tvWelcome cho TextView chào mừng trong XML
        btnNewMeeting = findViewById(R.id.btnNewMeeting);
        btnJoinMeeting = findViewById(R.id.btnJoinMeeting);

        // Hiển thị tên người dùng
        String fullName = sessionManager.getUserName();
        if (tvWelcome != null) {
            tvWelcome.setText("Welcome back, " + fullName);
        }

        // Bắt sự kiện Click
        btnNewMeeting.setOnClickListener(v -> createNewMeeting());
        btnJoinMeeting.setOnClickListener(v -> showJoinMeetingDialog());
    }

    // --- CHỨC NĂNG 1: TẠO PHÒNG MỚI ---
    private void createNewMeeting() {
        btnNewMeeting.setEnabled(false); // Khóa nút tránh click nhiều lần
        Toast.makeText(this, "Đang khởi tạo phòng họp...", Toast.LENGTH_SHORT).show();

        String fullName = sessionManager.getUserName();
        CreateMeetingRequest request = new CreateMeetingRequest("Phòng họp của " + fullName);

        // Thêm thời gian bắt đầu (Backend yêu cầu Future time)
        String nowPlus1Min = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .format(new Date(System.currentTimeMillis() + 60000));
        request.setStartTime(nowPlus1Min);

        apiService.createMeeting(request).enqueue(new Callback<ApiResponse<MeetingInfoResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<MeetingInfoResponse>> call, Response<ApiResponse<MeetingInfoResponse>> response) {
                btnNewMeeting.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 1000) {

                    // Tạo thành công, lấy được mã phòng
                    String newMeetingCode = response.body().getData().getMeetingCode();

                    // Ngay lập tức gọi API Join để vào phòng với tư cách Host
                    performJoinRequest(newMeetingCode);

                } else {
                    Toast.makeText(MainActivity.this, "Không thể tạo phòng: " + response.body().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MeetingInfoResponse>> call, Throwable t) {
                btnNewMeeting.setEnabled(true);
                Toast.makeText(MainActivity.this, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- CHỨC NĂNG 2: HIỂN THỊ DIALOG NHẬP MÃ ĐỂ THAM GIA ---
    private void showJoinMeetingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tham gia cuộc họp");

        // Tạo một ô nhập liệu (EditText) ngay trong Code
        final EditText input = new EditText(this);
        input.setHint("Nhập mã phòng (VD: abc-xyz)");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Nút Tham gia
        builder.setPositiveButton("Tham gia", (dialog, which) -> {
            String meetingCode = input.getText().toString().trim();
            if (!meetingCode.isEmpty()) {
                performJoinRequest(meetingCode);
            } else {
                Toast.makeText(MainActivity.this, "Mã phòng không được để trống", Toast.LENGTH_SHORT).show();
            }
        });

        // Nút Hủy
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // --- HÀM DÙNG CHUNG: GỌI API JOIN VÀ CHUYỂN TRANG ---
    private void performJoinRequest(String meetingCode) {
        Toast.makeText(this, "Đang kết nối...", Toast.LENGTH_SHORT).show();

        String displayName = sessionManager.getUserName();
        JoinMeetingRequest request = new JoinMeetingRequest(null, displayName);

        apiService.joinMeeting(meetingCode, request).enqueue(new Callback<ApiResponse<JoinMeetingResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<JoinMeetingResponse>> call, Response<ApiResponse<JoinMeetingResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 1000) {

                    JoinMeetingResponse joinData = response.body().getData();

                    // Chuyển sang MeetingActivity và mang theo toàn bộ dữ liệu cần thiết
                    Intent intent = new Intent(MainActivity.this, MeetingActivity.class);
                    intent.putExtra("LIVEKIT_TOKEN", joinData.getToken());
                    intent.putExtra("LIVEKIT_URL", joinData.getServerUrl());
                    intent.putExtra("USER_ROLE", joinData.getRole());
                    intent.putExtra("MEETING_CODE", meetingCode);

                    startActivity(intent);

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
}