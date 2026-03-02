package com.ptithcm.ptitmeet;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MeetingActivity extends AppCompatActivity {

    private RecyclerView rvParticipants;
    private ParticipantAdapter participantAdapter;
    private List<ParticipantData> participantList;

    private View badgeWaiting;
    private ImageButton btnLeave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting);

        // Ánh xạ
        rvParticipants = findViewById(R.id.rvParticipants);
        badgeWaiting = findViewById(R.id.badgeWaiting);
        btnLeave = findViewById(R.id.btnLeave);

        // Khởi tạo danh sách ảo (Mock Data) để bạn test giao diện
        participantList = new ArrayList<>();
        participantList.add(new ParticipantData("1", "Tân Vũ", true, true, false));
        participantList.add(new ParticipantData("2", "Người dùng A", false, false, true)); // Tắt cam, tắt mic, đang nói (để test viền xanh)

        // Cài đặt RecyclerView (Lưới 2 cột)
        participantAdapter = new ParticipantAdapter(participantList);
        // Nếu có ít người, bạn có thể chỉnh SpanCount dựa theo size của participantList
        rvParticipants.setLayoutManager(new GridLayoutManager(this, 2));
        rvParticipants.setAdapter(participantAdapter);

        // Nút Rời phòng
        btnLeave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Ngắt kết nối LiveKit và STOMP
                finish(); // Đóng Activity, quay về màn hình trước
            }
        });

        // Test bật chấm đỏ thông báo có người ở phòng chờ
        updateWaitingBadge(true);
    }

    // Hàm gọi khi có message từ WebSocket (STOMP) báo có người muốn vào
    private void updateWaitingBadge(boolean hasWaitingUsers) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (hasWaitingUsers) {
                    badgeWaiting.setVisibility(View.VISIBLE);
                } else {
                    badgeWaiting.setVisibility(View.GONE);
                }
            }
        });
    }
}