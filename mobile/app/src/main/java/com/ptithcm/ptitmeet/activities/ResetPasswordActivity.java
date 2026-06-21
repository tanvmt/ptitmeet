package com.ptithcm.ptitmeet.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.lifecycle.ViewModelProvider;

import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.viewmodel.ResetPasswordUiEvent;
import com.ptithcm.ptitmeet.viewmodel.ResetPasswordUiState;
import com.ptithcm.ptitmeet.viewmodel.ResetPasswordViewModel;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etToken;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private TextView tvState;
    private AppCompatButton btnResetPassword;
    private String verifiedResetToken;
    private ResetPasswordViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        viewModel = new ViewModelProvider(this).get(ResetPasswordViewModel.class);
        etToken = findViewById(R.id.etResetToken);
        etPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmNewPassword);
        tvState = findViewById(R.id.tvResetState);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        TextView tvBackToLogin = findViewById(R.id.tvResetBackToLogin);

        String token = getIntent().getStringExtra("RESET_TOKEN");
        Uri data = getIntent().getData();
        if ((token == null || token.trim().isEmpty()) && data != null) {
            token = data.getQueryParameter("token");
        }
        if (token != null && !token.trim().isEmpty()) {
            verifiedResetToken = token;
            etToken.setText(token);
            etToken.setVisibility(android.view.View.GONE);
            tvState.setText("OTP verified. Choose your new password.");
        }

        btnResetPassword.setOnClickListener(v -> resetPassword());
        tvBackToLogin.setOnClickListener(v -> openLogin());

        viewModel.getUiState().observe(this, this::applyState);
        viewModel.getUiEvent().observe(this, event -> {
            if (event == null) {
                return;
            }
            ResetPasswordUiEvent uiEvent = event.getContentIfNotHandled();
            if (uiEvent == null) {
                return;
            }
            handleEvent(uiEvent);
        });
    }

    private void resetPassword() {
        String token = verifiedResetToken != null && !verifiedResetToken.trim().isEmpty()
                ? verifiedResetToken
                : etToken.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();
        viewModel.resetPassword(token, password, confirmPassword);
    }

    private void setLoading(boolean loading) {
        btnResetPassword.setEnabled(!loading);
        btnResetPassword.setText(loading ? "Updating..." : "Update password");
    }

    private void showState(String message) {
        tvState.setText(message);
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void applyState(ResetPasswordUiState state) {
        if (state == null) {
            return;
        }
        setLoading(state.isLoading());
        if (state.getMessage() != null && !state.getMessage().isEmpty()) {
            showState(state.getMessage());
        }
    }

    private void handleEvent(ResetPasswordUiEvent event) {
        switch (event.getType()) {
            case ResetPasswordUiEvent.SHOW_TOAST:
                Toast.makeText(this, event.getMessage(), Toast.LENGTH_SHORT).show();
                break;
            case ResetPasswordUiEvent.OPEN_LOGIN:
                openLogin();
                break;
            default:
                break;
        }
    }
}
