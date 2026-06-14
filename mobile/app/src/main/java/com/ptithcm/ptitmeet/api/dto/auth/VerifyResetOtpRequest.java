package com.ptithcm.ptitmeet.api.dto.auth;

public class VerifyResetOtpRequest {

    private final String email;
    private final String otp;

    public VerifyResetOtpRequest(String email, String otp) {
        this.email = email;
        this.otp = otp;
    }

    public String getEmail() {
        return email;
    }

    public String getOtp() {
        return otp;
    }
}
