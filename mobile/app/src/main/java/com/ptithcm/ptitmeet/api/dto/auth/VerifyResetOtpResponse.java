package com.ptithcm.ptitmeet.api.dto.auth;

import com.google.gson.annotations.SerializedName;

public class VerifyResetOtpResponse {

    @SerializedName("resetToken")
    private String resetToken;

    public String getResetToken() {
        return resetToken;
    }
}
