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
import com.ptithcm.ptitmeet.api.dto.AuthResponse;
import com.ptithcm.ptitmeet.api.dto.LoginRequest;
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
    }

    private void performLogin(String email, String password) {
        // Khởi tạo Request dựa trên cấu trúc LoginRequest của backend
        LoginRequest loginRequest = new LoginRequest(email, password);

        // Gọi API qua RetrofitClient
        ApiService apiService = RetrofitClient.getApiService();
        apiService.login(loginRequest).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authData = response.body();

                    // Lưu thông tin đăng nhập vào SessionManager
                    sessionManager.saveAuthData(authData.getAccessToken(), authData.getUser().getId(), authData.getUser().getFullName());

                    Log.d("LOGIN_SUCCESS", "Token: " + authData.getAccessToken());
                    Toast.makeText(LoginActivity.this, "Welcome " + authData.getUser().getFullName(), Toast.LENGTH_SHORT).show();

                    // Chuyển vào trang chính
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Sai tài khoản hoặc mật khẩu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Log.e("API_ERROR", t.getMessage() != null ? t.getMessage() : "Unknown error");
                Toast.makeText(LoginActivity.this, "Fail to connect to server", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
