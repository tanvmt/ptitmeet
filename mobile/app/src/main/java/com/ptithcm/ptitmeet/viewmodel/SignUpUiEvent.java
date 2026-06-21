package com.ptithcm.ptitmeet.viewmodel;

public class SignUpUiEvent {
    public static final String SHOW_TOAST = "SHOW_TOAST";
    public static final String NAVIGATE_LOGIN = "NAVIGATE_LOGIN";

    private final String type;
    private final String message;
    private final String email;

    private SignUpUiEvent(String type, String message, String email) {
        this.type = type;
        this.message = message;
        this.email = email;
    }

    public static SignUpUiEvent toast(String message) {
        return new SignUpUiEvent(SHOW_TOAST, message, null);
    }

    public static SignUpUiEvent navigateLogin(String email) {
        return new SignUpUiEvent(NAVIGATE_LOGIN, null, email);
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
