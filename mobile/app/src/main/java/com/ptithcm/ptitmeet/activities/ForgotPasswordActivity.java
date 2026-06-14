package com.ptithcm.ptitmeet.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.api.dto.auth.ForgotPasswordRequest;
import com.ptithcm.ptitmeet.api.dto.common.ApiResponse;
import com.ptithcm.ptitmeet.api.services.ApiService;
import com.ptithcm.ptitmeet.api.services.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private AppCompatButton btnSendReset;
    private TextView tvState;
    private ApiService apiService;
    private String lastEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        apiService = RetrofitClient.getApiService(this);
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
    }

    private void sendResetEmail() {
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showState("Enter a valid email address.");
            return;
        }

        lastEmail = email;
        setLoading(true);
        apiService.forgotPasswordMobile(new ForgotPasswordRequest(email))
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        setLoading(false);
                        if (response.isSuccessful()) {
                            showState("OTP sent to " + email + ".");
                            openOtpScreen();
                            return;
                        }
                        String message = response.body() != null
                                ? response.body().getMessage()
                                : "Unable to send reset email.";
                        showState(message);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        setLoading(false);
                        showState("Cannot connect to server. Check your network and try again.");
                    }
                });
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

    private void showState(String message) {
        if (tvState != null) {
            tvState.setVisibility(android.view.View.VISIBLE);
            tvState.setText(message);
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
