package com.ptithcm.ptitmeet.viewmodel;

public class VerifyResetOtpUiEvent {
    public static final String SHOW_TOAST = "SHOW_TOAST";
    public static final String OPEN_RESET = "OPEN_RESET";

    private final String type;
    private final String message;
    private final String resetToken;

    private VerifyResetOtpUiEvent(String type, String message, String resetToken) {
        this.type = type;
        this.message = message;
        this.resetToken = resetToken;
    }

    public static VerifyResetOtpUiEvent toast(String message) {
        return new VerifyResetOtpUiEvent(SHOW_TOAST, message, null);
    }

    public static VerifyResetOtpUiEvent openReset(String resetToken) {
        return new VerifyResetOtpUiEvent(OPEN_RESET, null, resetToken);
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getResetToken() {
        return resetToken;
    }
}
