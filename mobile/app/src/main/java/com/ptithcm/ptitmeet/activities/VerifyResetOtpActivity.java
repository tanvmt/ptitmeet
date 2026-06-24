package com.ptithcm.ptitmeet.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.lifecycle.ViewModelProvider;

import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.viewmodel.VerifyResetOtpUiEvent;
import com.ptithcm.ptitmeet.viewmodel.VerifyResetOtpUiState;
import com.ptithcm.ptitmeet.viewmodel.VerifyResetOtpViewModel;

public class VerifyResetOtpActivity extends AppCompatActivity {

    private EditText etOtp;
    private TextView tvState;
    private AppCompatButton btnVerify;
    private String email;
    private VerifyResetOtpViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_reset_otp);

        viewModel = new ViewModelProvider(this).get(VerifyResetOtpViewModel.class);
        email = getIntent().getStringExtra("EMAIL");
        if (email == null) {
            email = "";
        }

        TextView tvOtpEmail = findViewById(R.id.tvOtpEmail);
        etOtp = findViewById(R.id.etResetOtp);
        tvState = findViewById(R.id.tvOtpState);
        btnVerify = findViewById(R.id.btnVerifyOtp);
        TextView tvBack = findViewById(R.id.tvBackToForgot);

        tvOtpEmail.setText("We sent a 6-digit code to " + email + ".");
        btnVerify.setOnClickListener(v -> verifyOtp());
        tvBack.setOnClickListener(v -> finish());

        viewModel.getUiState().observe(this, this::applyState);
        viewModel.getUiEvent().observe(this, event -> {
            if (event == null) {
                return;
            }
            VerifyResetOtpUiEvent uiEvent = event.getContentIfNotHandled();
            if (uiEvent == null) {
                return;
            }
            handleEvent(uiEvent);
        });
    }

    private void verifyOtp() {
        String otp = etOtp.getText().toString().trim();
        viewModel.verifyOtp(email, otp);
    }

    private void openResetPassword(String resetToken) {
        Toast.makeText(this, "Code verified", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, ResetPasswordActivity.class);
        intent.putExtra("EMAIL", email);
        intent.putExtra("RESET_TOKEN", resetToken);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        btnVerify.setEnabled(!loading);
        btnVerify.setText(loading ? "Verifying..." : "Verify code");
    }

    private void showState(String message) {
        tvState.setText(message);
    }

    private void applyState(VerifyResetOtpUiState state) {
        if (state == null) {
            return;
        }
        setLoading(state.isLoading());
        if (state.getMessage() != null && !state.getMessage().isEmpty()) {
            showState(state.getMessage());
        }
    }

    private void handleEvent(VerifyResetOtpUiEvent event) {
        switch (event.getType()) {
            case VerifyResetOtpUiEvent.SHOW_TOAST:
                Toast.makeText(this, event.getMessage(), Toast.LENGTH_SHORT).show();
                break;
            case VerifyResetOtpUiEvent.OPEN_RESET:
                openResetPassword(event.getResetToken());
                break;
            default:
                break;
        }
    }
}
