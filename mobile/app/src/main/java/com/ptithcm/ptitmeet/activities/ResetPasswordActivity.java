package com.ptithcm.ptitmeet.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.api.dto.auth.ResetPasswordRequest;
import com.ptithcm.ptitmeet.api.dto.common.ApiResponse;
import com.ptithcm.ptitmeet.api.services.ApiService;
import com.ptithcm.ptitmeet.api.services.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etToken;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private TextView tvState;
    private AppCompatButton btnResetPassword;
    private ApiService apiService;
    private String verifiedResetToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        apiService = RetrofitClient.getApiService(this);
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
    }

    private void resetPassword() {
        String token = verifiedResetToken != null && !verifiedResetToken.trim().isEmpty()
                ? verifiedResetToken
                : etToken.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (token.isEmpty()) {
            showState("Reset token is required.");
            return;
        }
        if (password.length() < 8) {
            showState("Password must be at least 8 characters.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showState("Passwords do not match.");
            return;
        }

        setLoading(true);
        apiService.resetPassword(new ResetPasswordRequest(token, password))
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        setLoading(false);
                        if (response.isSuccessful()) {
                            showState("Password updated. You can sign in now.");
                            Toast.makeText(ResetPasswordActivity.this, "Password updated", Toast.LENGTH_SHORT).show();
                            openLogin();
                            return;
                        }
                        String message = response.body() != null
                                ? response.body().getMessage()
                                : "Unable to update password.";
                        showState(message);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        setLoading(false);
                        showState("Cannot connect to server. Check your network and try again.");
                    }
                });
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
}
