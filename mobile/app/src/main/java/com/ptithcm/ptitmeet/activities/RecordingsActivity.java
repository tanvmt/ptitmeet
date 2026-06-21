package com.ptithcm.ptitmeet.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.adapters.RecordingAdapter;
import com.ptithcm.ptitmeet.api.dto.common.ApiResponse;
import com.ptithcm.ptitmeet.api.dto.recording.MeetingRecordingResponse;
import com.ptithcm.ptitmeet.api.services.ApiService;
import com.ptithcm.ptitmeet.api.services.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecordingsActivity extends AppCompatActivity {

    private TextView tvState;
    private ProgressBar progressBar;
    private RecordingAdapter adapter;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordings);

        apiService = RetrofitClient.getApiService(this);
        tvState = findViewById(R.id.tvRecordingsState);
        progressBar = findViewById(R.id.progressRecordings);
        AppCompatButton btnRefresh = findViewById(R.id.btnRefreshRecordings);
        RecyclerView rvRecordings = findViewById(R.id.rvRecordings);

        adapter = new RecordingAdapter(this::openRecording);
        rvRecordings.setLayoutManager(new LinearLayoutManager(this));
        rvRecordings.setAdapter(adapter);

        setupBottomNavigation();
        btnRefresh.setOnClickListener(v -> loadRecordings());
        loadRecordings();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_recordings);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_recordings) {
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
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void loadRecordings() {
        setLoading(true, "Loading recordings...");
        apiService.getMyRecordings().enqueue(new Callback<ApiResponse<List<MeetingRecordingResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<MeetingRecordingResponse>>> call,
                                   Response<ApiResponse<List<MeetingRecordingResponse>>> response) {
                setLoading(false, "Your saved meeting captures");
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<MeetingRecordingResponse> recordings = response.body().getData();
                    adapter.submitList(recordings);
                    if (recordings.isEmpty()) {
                        tvState.setText("No recordings yet. Start recording from an active meeting.");
                    } else {
                        tvState.setText(recordings.size() + " recording(s) found");
                    }
                    return;
                }
                adapter.submitList(null);
                String message = response.body() != null
                        ? response.body().getMessage()
                        : "Unable to load recordings.";
                tvState.setText(message);
            }

            @Override
            public void onFailure(Call<ApiResponse<List<MeetingRecordingResponse>>> call, Throwable t) {
                setLoading(false, "Cannot connect to server. Check your network and try again.");
                adapter.submitList(null);
            }
        });
    }

    private void setLoading(boolean loading, String message) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        tvState.setText(message);
    }

    private void openRecording(MeetingRecordingResponse recording) {
        if (recording == null || !recording.hasFile()) {
            Toast.makeText(this, "Recording file is not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(recording.getFileUrl()));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, "No app can open this recording", Toast.LENGTH_SHORT).show();
        }
    }
}
