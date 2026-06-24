package com.ptithcm.ptitmeet.viewmodel;

public class LoginUiEvent {
    public static final String SHOW_TOAST = "SHOW_TOAST";
    public static final String NAVIGATE_MAIN = "NAVIGATE_MAIN";

    private final String type;
    private final String message;

    private LoginUiEvent(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public static LoginUiEvent toast(String message) {
        return new LoginUiEvent(SHOW_TOAST, message);
    }

    public static LoginUiEvent navigateMain() {
        return new LoginUiEvent(NAVIGATE_MAIN, null);
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}
