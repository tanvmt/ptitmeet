package com.ptithcm.ptitmeet.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.viewmodel.SignUpUiEvent;
import com.ptithcm.ptitmeet.viewmodel.SignUpUiState;
import com.ptithcm.ptitmeet.viewmodel.SignUpViewModel;

public class SignUpActivity extends AppCompatActivity {
    private EditText etFullName, etEmail, etPassword, etConfirmPassword;
    private Button btnSignUp;
    private TextView tvLoginLink;
    private SignUpViewModel viewModel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        viewModel = new ViewModelProvider(this).get(SignUpViewModel.class);

        btnSignUp = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);
        etEmail = findViewById(R.id.etEmail);
        etFullName = findViewById(R.id.etFullName);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPass);

        btnSignUp.setOnClickListener(v -> handleSignUp());

        tvLoginLink.setOnClickListener(v -> {
            finish();
        });

        viewModel.getUiState().observe(this, this::applyState);
        viewModel.getUiEvent().observe(this, event -> {
            if (event == null) {
                return;
            }
            SignUpUiEvent uiEvent = event.getContentIfNotHandled();
            if (uiEvent == null) {
                return;
            }
            handleEvent(uiEvent);
        });
    }

    private void handleSignUp() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        viewModel.register(fullName, email, password, confirmPassword);
    }

    private void applyState(SignUpUiState state) {
        if (state == null) {
            return;
        }
        btnSignUp.setEnabled(!state.isLoading());
        btnSignUp.setText(state.isLoading() ? "Đang xử lý..." : "Đăng ký");
    }

    private void handleEvent(SignUpUiEvent event) {
        switch (event.getType()) {
            case SignUpUiEvent.SHOW_TOAST:
                Toast.makeText(this, event.getMessage(), Toast.LENGTH_SHORT).show();
                break;
            case SignUpUiEvent.NAVIGATE_LOGIN:
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                intent.putExtra("REGISTERED_EMAIL", event.getEmail());
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                break;
            default:
                break;
        }
    }
}
