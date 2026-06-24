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
import com.ptithcm.ptitmeet.viewmodel.ForgotPasswordUiEvent;
import com.ptithcm.ptitmeet.viewmodel.ForgotPasswordUiState;
import com.ptithcm.ptitmeet.viewmodel.ForgotPasswordViewModel;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private AppCompatButton btnSendReset;
    private TextView tvState;
    private String lastEmail;
    private ForgotPasswordViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        viewModel = new ViewModelProvider(this).get(ForgotPasswordViewModel.class);
        etEmail = findViewById(R.id.etForgotEmail);
        btnSendReset = findViewById(R.id.btnSendReset);
        tvState = findViewById(R.id.tvForgotState);
        TextView tvBackToLogin = findViewById(R.id.tvBackToLogin);

        String email = getIntent().getStringExtra("EMAIL");
        if (email != null && !email.trim().isEmpty()) {
            etEmail.setText(email);
        }

        btnSendReset.setOnClickListener(v -> sendResetEmail());
        tvBackToLogin.setOnClickListener(v -> finish());

        viewModel.getUiState().observe(this, this::applyState);
        viewModel.getUiEvent().observe(this, event -> {
            if (event == null) {
                return;
            }
            ForgotPasswordUiEvent uiEvent = event.getContentIfNotHandled();
            if (uiEvent == null) {
                return;
            }
            handleEvent(uiEvent);
        });
    }

    private void sendResetEmail() {
        String email = etEmail.getText().toString().trim();
        lastEmail = email;
        viewModel.sendResetEmail(email);
    }

    private void openOtpScreen() {
        if (lastEmail == null || lastEmail.trim().isEmpty()) {
            lastEmail = etEmail.getText().toString().trim();
        }
        Intent intent = new Intent(this, VerifyResetOtpActivity.class);
        intent.putExtra("EMAIL", lastEmail);
        startActivity(intent);
    }

    private void setLoading(boolean loading) {
        btnSendReset.setEnabled(!loading);
        btnSendReset.setText(loading ? "Sending..." : "Send reset email");
    }

    private void applyState(ForgotPasswordUiState state) {
        if (state == null) {
            return;
        }
        setLoading(state.isLoading());
        if (state.getMessage() != null && !state.getMessage().isEmpty()) {
            if (tvState != null) {
                tvState.setVisibility(android.view.View.VISIBLE);
                tvState.setText(state.getMessage());
            }
        }
    }

    private void handleEvent(ForgotPasswordUiEvent event) {
        switch (event.getType()) {
            case ForgotPasswordUiEvent.SHOW_TOAST:
                Toast.makeText(this, event.getMessage(), Toast.LENGTH_SHORT).show();
                break;
            case ForgotPasswordUiEvent.OPEN_OTP:
                lastEmail = event.getEmail();
                openOtpScreen();
                break;
            default:
                break;
        }
    }
}
