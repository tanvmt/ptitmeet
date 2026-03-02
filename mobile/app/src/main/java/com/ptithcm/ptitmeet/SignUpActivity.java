package com.ptithcm.ptitmeet;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SignUpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Ánh xạ
        Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvLoginLink = findViewById(R.id.tvLoginLink);
        EditText etFullName = findViewById(R.id.etFullName);
        EditText etPassword = findViewById(R.id.etPassword);
        EditText etConfirmPass = findViewById(R.id.etConfirmPass);

        // Xử lý Đăng ký
        btnRegister.setOnClickListener(v -> {
            String pass = etPassword.getText().toString();
            String confirm = etConfirmPass.getText().toString();

            if (!pass.equals(confirm)) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
                return;
            }

            // TODO: Gọi API đăng ký
            // Thành công -> Chuyển đến màn hình Waiting Room (theo luồng của Web Client)
            Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
            // Hoặc tạo WaitingRoomActivity sau này
            startActivity(intent);
            finishAffinity(); // Xóa lịch sử back stack
        });

        // Quay lại Đăng nhập
        tvLoginLink.setOnClickListener(v -> {
            finish(); // Đóng Activity này để quay lại trang Login trước đó
        });
    }
}