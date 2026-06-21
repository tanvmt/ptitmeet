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
import com.ptithcm.ptitmeet.viewmodel.LoginUiEvent;
import com.ptithcm.ptitmeet.viewmodel.LoginUiState;
import com.ptithcm.ptitmeet.viewmodel.LoginViewModel;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private LoginViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        TextView tvSignUpLink = findViewById(R.id.tvSignUpLink);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);

        String registeredEmail = getIntent().getStringExtra("REGISTERED_EMAIL");
        if (registeredEmail != null && !registeredEmail.trim().isEmpty()) {
            etEmail.setText(registeredEmail);
        }

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            viewModel.login(email, password);
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

        viewModel.getUiState().observe(this, this::applyState);
        viewModel.getUiEvent().observe(this, event -> {
            if (event == null) {
                return;
            }
            LoginUiEvent uiEvent = event.getContentIfNotHandled();
            if (uiEvent == null) {
                return;
            }
            handleEvent(uiEvent);
        });
    }

    private void applyState(LoginUiState state) {
        if (state == null) {
            return;
        }
        btnLogin.setEnabled(!state.isLoading());
        btnLogin.setText(state.isLoading() ? "Loading..." : "Login");
    }

    private void handleEvent(LoginUiEvent event) {
        switch (event.getType()) {
            case LoginUiEvent.SHOW_TOAST:
                Toast.makeText(this, event.getMessage(), Toast.LENGTH_SHORT).show();
                break;
            case LoginUiEvent.NAVIGATE_MAIN:
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
                break;
            default:
                break;
        }
    }
}
