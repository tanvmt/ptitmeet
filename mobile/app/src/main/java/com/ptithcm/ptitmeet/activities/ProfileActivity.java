package com.ptithcm.ptitmeet.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.api.dto.user.UserResponse;
import com.ptithcm.ptitmeet.viewmodel.ProfileUiEvent;
import com.ptithcm.ptitmeet.viewmodel.ProfileUiState;
import com.ptithcm.ptitmeet.viewmodel.ProfileViewModel;

import java.io.InputStream;

public class ProfileActivity extends AppCompatActivity {

    private View cardAvatar;
    private TextView tvInitial;
    private TextView tvName;
    private TextView tvEmail;
    private TextView tvStatus;
    private ImageView ivAvatar;
    private EditText etFullName;
    private ProgressBar progressBar;
    private AppCompatButton btnSave;
    private ProfileViewModel viewModel;
    private Uri selectedAvatarUri;
    private final ActivityResultLauncher<String> avatarPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedAvatarUri = uri;
                    ivAvatar.setImageURI(uri);
                    ivAvatar.setVisibility(View.VISIBLE);
                    tvInitial.setVisibility(View.GONE);
                    tvStatus.setText("Uploading avatar...");
                    uploadSelectedAvatar();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        cardAvatar = findViewById(R.id.cardProfileAvatar);
        tvInitial = findViewById(R.id.tvProfileInitial);
        ivAvatar = findViewById(R.id.ivProfileAvatar);
        tvName = findViewById(R.id.tvProfileName);
        tvEmail = findViewById(R.id.tvProfileEmail);
        etFullName = findViewById(R.id.etProfileFullName);
        tvStatus = findViewById(R.id.tvProfileStatus);
        progressBar = findViewById(R.id.progressProfile);
        btnSave = findViewById(R.id.btnSaveProfile);
        AppCompatButton btnLogout = findViewById(R.id.btnLogout);

        setupBottomNavigation();
        btnSave.setOnClickListener(v -> updateProfile());
        cardAvatar.setOnClickListener(v -> avatarPickerLauncher.launch("image/*"));
        btnLogout.setOnClickListener(v -> logout());
        viewModel.getUiState().observe(this, this::applyState);
        viewModel.getUiEvent().observe(this, event -> {
            if (event == null) {
                return;
            }
            ProfileUiEvent uiEvent = event.getContentIfNotHandled();
            if (uiEvent == null) {
                return;
            }
            handleEvent(uiEvent);
        });
        viewModel.loadProfile();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_profile);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
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
            return false;
        });
    }

    private void bindProfile(UserResponse user) {
        String fullName = safe(user.getFullName(), "User");
        String email = safe(user.getEmail(), "-");
        String avatarUrl = safe(user.getAvatarUrl(), "");

        tvName.setText(fullName);
        tvEmail.setText(email);
        if (!fullName.isEmpty()) {
            tvInitial.setText(fullName.substring(0, 1).toUpperCase());
        } else {
            tvInitial.setText("U");
        }
        etFullName.setText(fullName);
        if (selectedAvatarUri == null) {
            showRemoteAvatarIfPossible(avatarUrl);
        }
    }

    private void uploadSelectedAvatar() {
        if (selectedAvatarUri == null) {
            tvStatus.setText("Choose a photo first.");
            return;
        }
        viewModel.uploadAvatar(selectedAvatarUri);
    }

    private void updateProfile() {
        String fullName = etFullName.getText().toString().trim();
        viewModel.updateProfile(fullName, null);
    }

    private void setLoading(boolean loading, String message) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (btnSave != null) {
            btnSave.setEnabled(!loading);
        }
        if (tvStatus != null) {
            tvStatus.setText(message);
        }
    }

    private void setSaving(boolean saving) {
        btnSave.setEnabled(!saving);
        btnSave.setText(saving ? "Saving..." : "Save changes");
    }

    private void setAvatarUploading(boolean uploading) {
        if (cardAvatar != null) {
            cardAvatar.setEnabled(!uploading);
            cardAvatar.setAlpha(uploading ? 0.7f : 1f);
        }
    }

    private void showRemoteAvatarIfPossible(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.trim().isEmpty() || !avatarUrl.startsWith("http")) {
            ivAvatar.setVisibility(View.GONE);
            tvInitial.setVisibility(View.VISIBLE);
            return;
        }

        Glide.with(this)
                .load(avatarUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(ivAvatar);

        ivAvatar.setVisibility(View.VISIBLE);
        tvInitial.setVisibility(View.GONE);
    }

    private void logout() {
        viewModel.logout();
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private void applyState(ProfileUiState state) {
        if (state == null) {
            return;
        }
        setLoading(state.isLoading(), state.getStatusMessage());
        setSaving(state.isSaving());
        setAvatarUploading(state.isAvatarUploading());
        if (state.getUser() != null) {
            bindProfile(state.getUser());
        }
        if (state.getStatusMessage() != null && !state.getStatusMessage().isEmpty()) {
            tvStatus.setText(state.getStatusMessage());
        }
    }

    private void handleEvent(ProfileUiEvent event) {
        switch (event.getType()) {
            case ProfileUiEvent.SHOW_TOAST:
                Toast.makeText(this, event.getMessage(), Toast.LENGTH_SHORT).show();
                break;
            case ProfileUiEvent.OPEN_LOGIN:
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                break;
            default:
                break;
        }
    }
}
