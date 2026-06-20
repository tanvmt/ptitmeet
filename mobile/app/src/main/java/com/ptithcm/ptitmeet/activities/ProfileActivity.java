package com.ptithcm.ptitmeet.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.api.SessionManager;
import com.ptithcm.ptitmeet.api.dto.common.ApiResponse;
import com.ptithcm.ptitmeet.api.dto.user.UpdateProfileRequest;
import com.ptithcm.ptitmeet.api.dto.user.UserResponse;
import com.ptithcm.ptitmeet.api.services.ApiService;
import com.ptithcm.ptitmeet.api.services.RetrofitClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvInitial;
    private TextView tvName;
    private TextView tvEmail;
    private TextView tvStatus;
    private ImageView ivAvatar;
    private EditText etFullName;
    private EditText etAvatarUrl;
    private ProgressBar progressBar;
    private AppCompatButton btnSave;
    private AppCompatButton btnUploadAvatar;
    private SessionManager sessionManager;
    private ApiService apiService;
    private Uri selectedAvatarUri;
    private final ActivityResultLauncher<String> avatarPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedAvatarUri = uri;
                    ivAvatar.setImageURI(uri);
                    ivAvatar.setVisibility(View.VISIBLE);
                    tvInitial.setVisibility(View.GONE);
                    btnUploadAvatar.setEnabled(true);
                    tvStatus.setText("Photo selected. Tap Upload to update your avatar.");
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getApiService(this);

        tvInitial = findViewById(R.id.tvProfileInitial);
        ivAvatar = findViewById(R.id.ivProfileAvatar);
        tvName = findViewById(R.id.tvProfileName);
        tvEmail = findViewById(R.id.tvProfileEmail);
        etFullName = findViewById(R.id.etProfileFullName);
        etAvatarUrl = findViewById(R.id.etProfileAvatarUrl);
        tvStatus = findViewById(R.id.tvProfileStatus);
        progressBar = findViewById(R.id.progressProfile);
        btnSave = findViewById(R.id.btnSaveProfile);
        AppCompatButton btnChooseAvatar = findViewById(R.id.btnChooseAvatar);
        btnUploadAvatar = findViewById(R.id.btnUploadAvatar);
        AppCompatButton btnLogout = findViewById(R.id.btnLogout);

        setupBottomNavigation();
        btnSave.setOnClickListener(v -> updateProfile());
        btnChooseAvatar.setOnClickListener(v -> avatarPickerLauncher.launch("image/*"));
        btnUploadAvatar.setOnClickListener(v -> uploadSelectedAvatar());
        btnLogout.setOnClickListener(v -> logout());
        loadProfile();
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
            if (id == R.id.nav_recordings) {
                startActivity(new Intent(this, RecordingsActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void loadProfile() {
        setLoading(true, "Loading profile...");
        apiService.getProfile().enqueue(new Callback<ApiResponse<UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserResponse>> call, Response<ApiResponse<UserResponse>> response) {
                setLoading(false, "Keep your display name and avatar link up to date.");
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    bindProfile(response.body().getData());
                    return;
                }
                String message = response.body() != null
                        ? response.body().getMessage()
                        : "Unable to load profile.";
                tvStatus.setText(message);
            }

            @Override
            public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {
                setLoading(false, "Cannot connect to server. Pull back later and try again.");
            }
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
        if (etAvatarUrl != null) {
            etAvatarUrl.setText(avatarUrl);
        }
        if (selectedAvatarUri == null) {
            showRemoteAvatarIfPossible(avatarUrl);
        }
        sessionManager.updateUserName(fullName);
    }

    private void uploadSelectedAvatar() {
        if (selectedAvatarUri == null) {
            tvStatus.setText("Choose a photo first.");
            return;
        }

        MultipartBody.Part avatarPart;
        try {
            avatarPart = createAvatarPart(selectedAvatarUri);
        } catch (Exception exception) {
            tvStatus.setText("Unable to read selected photo.");
            return;
        }

        setAvatarUploading(true);
        apiService.uploadAvatar(avatarPart).enqueue(new Callback<ApiResponse<UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserResponse>> call, Response<ApiResponse<UserResponse>> response) {
                setAvatarUploading(false);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    UserResponse user = response.body().getData();
                    bindProfile(user);
                    tvStatus.setText("Avatar updated.");
                    Toast.makeText(ProfileActivity.this, "Avatar updated", Toast.LENGTH_SHORT).show();
                    return;
                }
                String message = response.body() != null
                        ? response.body().getMessage()
                        : "Unable to upload avatar.";
                tvStatus.setText(message);
            }

            @Override
            public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {
                setAvatarUploading(false);
                tvStatus.setText("Cannot connect to server. Check your network and try again.");
            }
        });
    }

    private MultipartBody.Part createAvatarPart(Uri uri) throws Exception {
        String mimeType = getContentResolver().getType(uri);
        if (mimeType == null || !mimeType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            mimeType = "image/jpeg";
        }
        String extension = mimeType.endsWith("png") ? ".png" : ".jpg";
        File file = File.createTempFile("avatar_", extension, getCacheDir());

        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(file)) {
            if (inputStream == null) {
                throw new IllegalStateException("Selected photo cannot be opened");
            }
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        RequestBody body = RequestBody.create(file, MediaType.parse(mimeType));
        return MultipartBody.Part.createFormData("file", file.getName(), body);
    }

    private void updateProfile() {
        String fullName = etFullName.getText().toString().trim();
        String avatarUrl = etAvatarUrl.getText().toString().trim();
        if (fullName.length() < 2) {
            tvStatus.setText("Full name must contain at least 2 characters.");
            return;
        }
        if (avatarUrl.isEmpty()) {
            avatarUrl = null;
        }

        setSaving(true);
        apiService.updateProfile(new UpdateProfileRequest(fullName, avatarUrl))
                .enqueue(new Callback<ApiResponse<UserResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<UserResponse>> call, Response<ApiResponse<UserResponse>> response) {
                        setSaving(false);
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            bindProfile(response.body().getData());
                            tvStatus.setText("Profile updated.");
                            Toast.makeText(ProfileActivity.this, "Profile updated", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String message = response.body() != null
                                ? response.body().getMessage()
                                : "Unable to update profile.";
                        tvStatus.setText(message);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {
                        setSaving(false);
                        tvStatus.setText("Cannot connect to server. Check your network and try again.");
                    }
                });
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
        btnUploadAvatar.setEnabled(!uploading && selectedAvatarUri != null);
        btnUploadAvatar.setText(uploading ? "Uploading..." : "Upload");
    }

    private void showRemoteAvatarIfPossible(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.trim().isEmpty() || !avatarUrl.startsWith("http")) {
            ivAvatar.setVisibility(View.GONE);
            tvInitial.setVisibility(View.VISIBLE);
            return;
        }
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(avatarUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                try (InputStream inputStream = connection.getInputStream()) {
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    if (bitmap != null) {
                        runOnUiThread(() -> {
                            ivAvatar.setImageBitmap(bitmap);
                            ivAvatar.setVisibility(View.VISIBLE);
                            tvInitial.setVisibility(View.GONE);
                        });
                    }
                }
            } catch (Exception ignored) {
                runOnUiThread(() -> {
                    ivAvatar.setVisibility(View.GONE);
                    tvInitial.setVisibility(View.VISIBLE);
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private void logout() {
        sessionManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
