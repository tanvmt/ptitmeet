package com.ptithcm.ptitmeet;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class LandingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        // Ánh xạ các nút bấm
        TextView btnSignIn = findViewById(R.id.btnSignInNav);
        Button btnStartMeeting = findViewById(R.id.btnStartMeeting);

        // Xử lý sự kiện bấm nút "Sign In"
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LandingActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        // Xử lý sự kiện bấm nút "Start New Meeting"
        btnStartMeeting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Tạm thời chuyển đến Waiting Room hoặc Login
            }
        });
    }
}