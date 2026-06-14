package com.ptithcm.ptitmeet.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.api.dto.auth.VerifyResetOtpRequest;
import com.ptithcm.ptitmeet.api.dto.auth.VerifyResetOtpResponse;
import com.ptithcm.ptitmeet.api.dto.common.ApiResponse;
import com.ptithcm.ptitmeet.api.services.ApiService;
import com.ptithcm.ptitmeet.api.services.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerifyResetOtpActivity extends AppCompatActivity {

    private EditText etOtp;
    private TextView tvState;
    private AppCompatButton btnVerify;
    private ApiService apiService;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_reset_otp);

        apiService = RetrofitClient.getApiService(this);
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
    }

    private void verifyOtp() {
        String otp = etOtp.getText().toString().trim();
        if (email.trim().isEmpty()) {
            showState("Email is missing. Please request a new code.");
            return;
        }
        if (!otp.matches("\\d{6}")) {
            showState("Enter the 6-digit code from your email.");
            return;
        }

        setLoading(true);
        apiService.verifyResetOtp(new VerifyResetOtpRequest(email, otp))
                .enqueue(new Callback<ApiResponse<VerifyResetOtpResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<VerifyResetOtpResponse>> call,
                                           Response<ApiResponse<VerifyResetOtpResponse>> response) {
                        setLoading(false);
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().getData() != null
                                && response.body().getData().getResetToken() != null) {
                            openResetPassword(response.body().getData().getResetToken());
                            return;
                        }
                        String message = response.body() != null
                                ? response.body().getMessage()
                                : "Invalid or expired code.";
                        showState(message);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<VerifyResetOtpResponse>> call, Throwable t) {
                        setLoading(false);
                        showState("Cannot connect to server. Check your network and try again.");
                    }
                });
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
}
