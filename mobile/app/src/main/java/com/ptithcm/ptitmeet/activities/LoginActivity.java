package com.ptithcm.ptitmeet.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.api.SessionManager;
import com.ptithcm.ptitmeet.api.dto.auth.AuthEnvelope;
import com.ptithcm.ptitmeet.api.dto.auth.AuthResponse;
import com.ptithcm.ptitmeet.api.dto.auth.LoginRequest;
import com.ptithcm.ptitmeet.api.services.ApiService;
import com.ptithcm.ptitmeet.api.services.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Khởi tạo SessionManager
        sessionManager = new SessionManager(this);

        // Ánh xạ View
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvSignUpLink = findViewById(R.id.tvSignUpLink);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);

        String registeredEmail = getIntent().getStringExtra("REGISTERED_EMAIL");
        if (registeredEmail != null && !registeredEmail.trim().isEmpty()) {
            etEmail.setText(registeredEmail);
        }

        // Xử lý nút Đăng nhập
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            performLogin(email, password);
        });

        tvSignUpLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            intent.putExtra("EMAIL", etEmail.getText().toString().trim());
            startActivity(intent);
        });
    }

    private void performLogin(String email, String password) {
        LoginRequest loginRequest = new LoginRequest(email, password);
        ApiService apiService = RetrofitClient.getApiService(this);
        apiService.login(loginRequest).enqueue(new Callback<AuthEnvelope>() {
            @Override
            public void onResponse(Call<AuthEnvelope> call, Response<AuthEnvelope> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    AuthResponse authData = response.body().getData();

                    sessionManager.saveAuthData(
                            authData.getAccessToken(),
                            authData.getUser().getUserId(),
                            authData.getUser().getFullName()
                    );

                    Log.d("LOGIN_SUCCESS", "Token: " + authData.getAccessToken());
                    Toast.makeText(LoginActivity.this, "Welcome " + authData.getUser().getFullName(), Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMessage = response.body() != null ? response.body().getMessage() : "Sai tài khoản hoặc mật khẩu";
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthEnvelope> call, Throwable t) {
                Log.e("API_ERROR", t.getMessage() != null ? t.getMessage() : "Unknown error");
                Toast.makeText(LoginActivity.this, "Fail to connect to server", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
