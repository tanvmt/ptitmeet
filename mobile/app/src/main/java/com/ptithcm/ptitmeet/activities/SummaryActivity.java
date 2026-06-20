package com.ptithcm.ptitmeet.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.api.SessionManager;
import com.ptithcm.ptitmeet.api.dto.common.ApiResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.FeedbackRequest;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingSummaryResponse;
import com.ptithcm.ptitmeet.api.services.ApiService;
import com.ptithcm.ptitmeet.api.services.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SummaryActivity extends AppCompatActivity {

    private TextView tvTitle;
    private TextView tvSubtitle;
    private MaterialButton btnRejoin;
    private MaterialButton btnReturnHome;
    private TextView tvParticipantsVal;
    private TextView tvMessagesVal;
    private TextView tvRecordingVal;
    private TextView tvFeedbackSuccess;
    private ImageView[] ivStars = new ImageView[5];
    private LinearLayout layoutStars;

    private ApiService apiService;
    private SessionManager sessionManager;
    private String meetingCode;
    private String actionTaken;
    private int selectedRating = 0;
    private boolean isRatingSubmitted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        apiService = RetrofitClient.getApiService(this);
        sessionManager = new SessionManager(this);

        meetingCode = getIntent().getStringExtra("MEETING_CODE");
        actionTaken = getIntent().getStringExtra("ACTION_TAKEN");
        if (actionTaken == null) {
            actionTaken = "LEAVE";
        }

        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        btnRejoin = findViewById(R.id.btnRejoin);
        btnReturnHome = findViewById(R.id.btnReturnHome);
        tvParticipantsVal = findViewById(R.id.tvParticipantsVal);
        tvMessagesVal = findViewById(R.id.tvMessagesVal);
        tvRecordingVal = findViewById(R.id.tvRecordingVal);
        tvFeedbackSuccess = findViewById(R.id.tvFeedbackSuccess);
        layoutStars = findViewById(R.id.layoutStars);

        ivStars[0] = findViewById(R.id.ivStar1);
        ivStars[1] = findViewById(R.id.ivStar2);
        ivStars[2] = findViewById(R.id.ivStar3);
        ivStars[3] = findViewById(R.id.ivStar4);
        ivStars[4] = findViewById(R.id.ivStar5);

        setupTitleAndSubtitle();
        setupButtons();
        setupStars();
        fetchSummaryData();
    }

    private void setupTitleAndSubtitle() {
        String titleText = "You left the meeting";
        if ("END".equalsIgnoreCase(actionTaken)) {
            titleText = "You ended the meeting";
        } else if ("ENDED_BY_HOST".equalsIgnoreCase(actionTaken)) {
            titleText = "The host has ended this meeting";
        } else if ("KICKED".equalsIgnoreCase(actionTaken) || "KICKED_BY_HOST".equalsIgnoreCase(actionTaken)) {
            titleText = "You were kicked from the meeting";
        }
        tvTitle.setText(titleText);

        String subtitleText = "Meeting ID: " + (meetingCode != null ? meetingCode : "Unknown") + " • Calculating duration";
        tvSubtitle.setText(subtitleText);
    }

    private void setupButtons() {
        // Only show Rejoin button if user left or was kicked (not ended by host / host ended)
        if ("END".equalsIgnoreCase(actionTaken) || "ENDED_BY_HOST".equalsIgnoreCase(actionTaken)) {
            btnRejoin.setVisibility(View.GONE);
        } else {
            btnRejoin.setVisibility(View.VISIBLE);
            btnRejoin.setOnClickListener(v -> rejoinMeeting());
        }

        btnReturnHome.setOnClickListener(v -> returnHome());
    }

    private void rejoinMeeting() {
        if (meetingCode == null || meetingCode.trim().isEmpty()) {
            Toast.makeText(this, "Meeting code is not available to rejoin", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, WaitingRoomActivity.class);
        intent.putExtra("MEETING_CODE", meetingCode);
        intent.putExtra("DISPLAY_NAME", sessionManager.getUserName());
        intent.putExtra("IS_HOST_SETUP", false);
        startActivity(intent);
        finish();
    }

    private void returnHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void setupStars() {
        for (int i = 0; i < 5; i++) {
            final int starIndex = i;
            ivStars[i].setOnClickListener(v -> {
                if (isRatingSubmitted) return;
                submitRating(starIndex + 1);
            });
        }
    }

    private void updateStarsUi(int rating) {
        for (int i = 0; i < 5; i++) {
            if (i < rating) {
                ivStars[i].setImageResource(R.drawable.ic_star_filled);
            } else {
                ivStars[i].setImageResource(R.drawable.ic_star_border);
            }
        }
    }

    private void submitRating(int rating) {
        if (meetingCode == null || meetingCode.trim().isEmpty() || isRatingSubmitted) {
            return;
        }

        selectedRating = rating;
        updateStarsUi(rating);

        apiService.submitFeedback(meetingCode, new FeedbackRequest(rating)).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    isRatingSubmitted = true;
                    tvFeedbackSuccess.setVisibility(View.VISIBLE);
                    for (ImageView star : ivStars) {
                        star.setEnabled(false);
                        star.setAlpha(0.8f);
                    }
                } else {
                    Toast.makeText(SummaryActivity.this, "Unable to submit feedback", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(SummaryActivity.this, "Connection error, feedback not submitted", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchSummaryData() {
        if (meetingCode == null || meetingCode.trim().isEmpty()) {
            tvSubtitle.setText("Meeting ID: Unknown • -- duration");
            return;
        }

        apiService.getMeetingSummary(meetingCode, actionTaken).enqueue(new Callback<ApiResponse<MeetingSummaryResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<MeetingSummaryResponse>> call, Response<ApiResponse<MeetingSummaryResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    MeetingSummaryResponse summary = response.body().getData();
                    tvSubtitle.setText("Meeting ID: " + meetingCode + " • " + summary.getDuration() + " duration");
                    tvParticipantsVal.setText(summary.getParticipants() + " Joined");
                    tvMessagesVal.setText(summary.getMessages() + " Sent");
                } else {
                    tvSubtitle.setText("Meeting ID: " + meetingCode + " • -- duration");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MeetingSummaryResponse>> call, Throwable t) {
                tvSubtitle.setText("Meeting ID: " + meetingCode + " • -- duration");
            }
        });
    }
}
