package com.ptithcm.ptitmeet.viewmodel;

public class ProfileUiEvent {
    public static final String SHOW_TOAST = "SHOW_TOAST";
    public static final String OPEN_LOGIN = "OPEN_LOGIN";

    private final String type;
    private final String message;

    private ProfileUiEvent(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public static ProfileUiEvent toast(String message) {
        return new ProfileUiEvent(SHOW_TOAST, message);
    }

    public static ProfileUiEvent openLogin() {
        return new ProfileUiEvent(OPEN_LOGIN, null);
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}
