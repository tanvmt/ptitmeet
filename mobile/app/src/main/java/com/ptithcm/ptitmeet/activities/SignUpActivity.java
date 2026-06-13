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
import com.ptithcm.ptitmeet.api.dto.auth.RegisterRequest;
import com.ptithcm.ptitmeet.api.dto.common.ApiResponse;
import com.ptithcm.ptitmeet.api.dto.user.UserResponse;
import com.ptithcm.ptitmeet.api.services.ApiService;
import com.ptithcm.ptitmeet.api.services.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignUpActivity extends AppCompatActivity {
    private EditText etFullName, etEmail, etPassword, etConfirmPassword;
    private Button btnSignUp;
    private TextView tvLoginLink;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Ánh xạ
        btnSignUp = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);
        etEmail = findViewById(R.id.etEmail);
        etFullName = findViewById(R.id.etFullName);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPass);

        // Xử lý Đăng ký
        btnSignUp.setOnClickListener(v -> handleSignUp());

        // Quay lại Đăng nhập
        tvLoginLink.setOnClickListener(v -> {
            finish(); // Đóng Activity này để quay lại trang Login trước đó
        });
    }
    private void handleSignUp() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // 1. Kiểm tra tính hợp lệ của dữ liệu đầu vào (Validation)
        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Disable nút bấm để tránh spam click trong lúc chờ API
        btnSignUp.setEnabled(false);
        btnSignUp.setText("Đang xử lý...");

        RegisterRequest registerRequest = new RegisterRequest(fullName, email, password);
        ApiService apiService = RetrofitClient.getApiService(this);

        apiService.register(registerRequest).enqueue(new Callback<ApiResponse<UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserResponse>> call, Response<ApiResponse<UserResponse>> response) {
                btnSignUp.setEnabled(true);
                btnSignUp.setText("Đăng ký");

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    Toast.makeText(SignUpActivity.this, "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                    intent.putExtra("REGISTERED_EMAIL", email);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMessage = response.body() != null ? response.body().getMessage() : "Đăng ký thất bại hoặc email đã tồn tại";
                    Toast.makeText(SignUpActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {
                btnSignUp.setEnabled(true);
                btnSignUp.setText("Đăng ký");

                Log.e("API_ERROR", "Lỗi đăng ký: " + t.getMessage());
                Toast.makeText(SignUpActivity.this, "Không thể kết nối tới máy chủ", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
