package com.ptithcm.ptitmeet.viewmodel;

public class ResetPasswordUiEvent {
    public static final String SHOW_TOAST = "SHOW_TOAST";
    public static final String OPEN_LOGIN = "OPEN_LOGIN";

    private final String type;
    private final String message;

    private ResetPasswordUiEvent(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public static ResetPasswordUiEvent toast(String message) {
        return new ResetPasswordUiEvent(SHOW_TOAST, message);
    }

    public static ResetPasswordUiEvent openLogin() {
        return new ResetPasswordUiEvent(OPEN_LOGIN, null);
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}
