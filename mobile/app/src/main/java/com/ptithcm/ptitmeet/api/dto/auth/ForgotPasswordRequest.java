package com.ptithcm.ptitmeet.api.dto.auth;

public class ForgotPasswordRequest {

    private final String email;

    public ForgotPasswordRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
