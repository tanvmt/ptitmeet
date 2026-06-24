package com.ptithcm.ptitmeet.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.utils.SettingsManager;

public class SettingsActivity extends AppCompatActivity {

    private MaterialSwitch switchChatNotif;
    private MaterialSwitch switchJoinLeaveNotif;
    private MaterialSwitch switchRaiseHandNotif;
    private MaterialSwitch switchReminderNotif;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        switchChatNotif = findViewById(R.id.switchChatNotif);
        switchJoinLeaveNotif = findViewById(R.id.switchJoinLeaveNotif);
        switchRaiseHandNotif = findViewById(R.id.switchRaiseHandNotif);
        switchReminderNotif = findViewById(R.id.switchReminderNotif);

        android.view.View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        loadSettings();
        setupListeners();
        setupBottomNavigation();
    }

    private void loadSettings() {
        switchChatNotif.setChecked(SettingsManager.isChatNotifEnabled(this));
        switchJoinLeaveNotif.setChecked(SettingsManager.isJoinLeaveNotifEnabled(this));
        switchRaiseHandNotif.setChecked(SettingsManager.isRaiseHandNotifEnabled(this));
        switchReminderNotif.setChecked(SettingsManager.isReminderNotifEnabled(this));
    }

    private void setupListeners() {
        switchChatNotif.setOnCheckedChangeListener((buttonView, isChecked) -> 
            SettingsManager.setChatNotifEnabled(SettingsActivity.this, isChecked));
        
        switchJoinLeaveNotif.setOnCheckedChangeListener((buttonView, isChecked) -> 
            SettingsManager.setJoinLeaveNotifEnabled(SettingsActivity.this, isChecked));

        switchRaiseHandNotif.setOnCheckedChangeListener((buttonView, isChecked) -> 
            SettingsManager.setRaiseHandNotifEnabled(SettingsActivity.this, isChecked));

        switchReminderNotif.setOnCheckedChangeListener((buttonView, isChecked) -> 
            SettingsManager.setReminderNotifEnabled(SettingsActivity.this, isChecked));
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_settings);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_settings) {
                return true;
            }
            if (id == R.id.nav_dashboard) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            }
            if (id == R.id.nav_meetings) {
                startActivity(new Intent(this, MeetingsActivity.class));
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
}
