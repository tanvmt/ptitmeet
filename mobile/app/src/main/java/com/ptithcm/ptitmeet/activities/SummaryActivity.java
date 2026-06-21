package com.ptithcm.ptitmeet.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.viewmodel.SummaryUiEvent;
import com.ptithcm.ptitmeet.viewmodel.SummaryUiState;
import com.ptithcm.ptitmeet.viewmodel.SummaryViewModel;

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

    private SummaryViewModel viewModel;
    private String meetingCode;
    private String actionTaken;
    private int selectedRating = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        meetingCode = getIntent().getStringExtra("MEETING_CODE");
        actionTaken = getIntent().getStringExtra("ACTION_TAKEN");
        if (actionTaken == null) {
            actionTaken = "LEAVE";
        }
        viewModel = new ViewModelProvider(
                this,
                new SummaryViewModel.Factory(getApplication(), meetingCode, actionTaken)
        ).get(SummaryViewModel.class);

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
        viewModel.getUiState().observe(this, this::applyState);
        viewModel.getUiEvent().observe(this, event -> {
            if (event == null) {
                return;
            }
            SummaryUiEvent uiEvent = event.getContentIfNotHandled();
            if (uiEvent == null) {
                return;
            }
            if (SummaryUiEvent.SHOW_TOAST.equals(uiEvent.getType())) {
                Toast.makeText(this, uiEvent.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.fetchSummaryData();
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
        intent.putExtra("DISPLAY_NAME", viewModel.getDisplayName());
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
                viewModel.submitRating(starIndex + 1);
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

    private void applyState(SummaryUiState state) {
        if (state == null) {
            return;
        }
        if (state.getSubtitle() != null) {
            tvSubtitle.setText(state.getSubtitle());
        }
        tvParticipantsVal.setText(state.getParticipantsText());
        tvMessagesVal.setText(state.getMessagesText());
        selectedRating = state.getSelectedRating();
        updateStarsUi(selectedRating);
        if (state.isRatingSubmitted()) {
            tvFeedbackSuccess.setVisibility(View.VISIBLE);
            for (ImageView star : ivStars) {
                star.setEnabled(false);
                star.setAlpha(0.8f);
            }
        }
    }
}
