package com.ptithcm.ptitmeet.api.dto.auth;

public class ResetPasswordRequest {

    private final String token;
    private final String newPassword;

    public ResetPasswordRequest(String token, String newPassword) {
        this.token = token;
        this.newPassword = newPassword;
    }

    public String getToken() {
        return token;
    }

    public String getNewPassword() {
        return newPassword;
    }
}
