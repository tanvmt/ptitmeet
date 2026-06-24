package com.ptithcm.ptitmeet.viewmodel;

public class SummaryUiEvent {
    public static final String SHOW_TOAST = "SHOW_TOAST";

    private final String type;
    private final String message;

    private SummaryUiEvent(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public static SummaryUiEvent toast(String message) {
        return new SummaryUiEvent(SHOW_TOAST, message);
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}
