package com.ptithcm.ptitmeet.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.api.SessionManager;

public class LandingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        SessionManager sessionManager = new SessionManager(this);

        TextView btnSignIn = findViewById(R.id.btnSignInNav);
        Button btnStartMeeting = findViewById(R.id.btnStartMeeting);

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LandingActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        btnStartMeeting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Class<?> nextScreen = sessionManager.getToken() == null ? LoginActivity.class : MainActivity.class;
                Intent intent = new Intent(LandingActivity.this, nextScreen);
                startActivity(intent);
            }
        });
    }
}
