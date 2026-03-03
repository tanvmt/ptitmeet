package com.ptithcm.ptitmeet;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class WaitingRoomActivity extends AppCompatActivity {

    // Trạng thái mặc định
    private boolean isMicOn = true;
    private boolean isVideoOn = true;

    // Các thành phần giao diện
    private FloatingActionButton fabToggleMic;
    private FloatingActionButton fabToggleVideo;
    private FrameLayout videoPreviewContainer;
    private MaterialButton btnJoin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_room); // Đảm bảo bạn đã có file XML này

        // Ánh xạ View từ XML
        fabToggleMic = findViewById(R.id.fabToggleMic);
        fabToggleVideo = findViewById(R.id.fabToggleVideo);
        videoPreviewContainer = findViewById(R.id.videoPreviewContainer);
        btnJoin = findViewById(R.id.btnJoin);

        // Xử lý sự kiện click nút Mic
        fabToggleMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isMicOn = !isMicOn;
                if (isMicOn) {
                    fabToggleMic.setImageResource(R.drawable.ic_mic);
                    fabToggleMic.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")));
                } else {
                    // Bạn cần chắc chắn có R.drawable.ic_mic_off trong folder drawable
                    fabToggleMic.setImageResource(R.drawable.ic_mic); // Tạm dùng ic_mic nếu chưa có ic_mic_off
                    fabToggleMic.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E53935"))); // Đỏ
                }
                // TODO: Gọi hàm SDK tắt/bật mic ở đây
            }
        });

        // Xử lý sự kiện click nút Video
        fabToggleVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isVideoOn = !isVideoOn;
                if (isVideoOn) {
                    fabToggleVideo.setImageResource(R.drawable.ic_videocam);
                    fabToggleVideo.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#137fec"))); // Xanh primary
                    videoPreviewContainer.setVisibility(View.VISIBLE);
                } else {
                    // Bạn cần chắc chắn có R.drawable.ic_videocam_off trong folder drawable
                    fabToggleVideo.setImageResource(R.drawable.ic_videocam); // Tạm dùng ic_videocam nếu chưa có
                    fabToggleVideo.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E53935"))); // Đỏ
                    videoPreviewContainer.setVisibility(View.GONE);
                }
                // TODO: Gọi hàm SDK tắt/bật camera ở đây
            }
        });

        // Xử lý sự kiện click nút Join Now
        btnJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Gọi API Join Meeting (giống logic React handleAskToJoin)
                btnJoin.setText("Joining...");
                btnJoin.setEnabled(false);
            }
        });
    }
}