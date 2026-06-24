package com.ptithcm.ptitmeet.viewmodel;

public class ForgotPasswordUiEvent {
    public static final String SHOW_TOAST = "SHOW_TOAST";
    public static final String OPEN_OTP = "OPEN_OTP";

    private final String type;
    private final String message;
    private final String email;

    private ForgotPasswordUiEvent(String type, String message, String email) {
        this.type = type;
        this.message = message;
        this.email = email;
    }

    public static ForgotPasswordUiEvent toast(String message) {
        return new ForgotPasswordUiEvent(SHOW_TOAST, message, null);
    }

    public static ForgotPasswordUiEvent openOtp(String email) {
        return new ForgotPasswordUiEvent(OPEN_OTP, null, email);
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getEmail() {
        return email;
    }
}
