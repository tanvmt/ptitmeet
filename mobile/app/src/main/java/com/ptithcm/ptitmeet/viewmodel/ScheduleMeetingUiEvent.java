package com.ptithcm.ptitmeet.viewmodel;

public class ScheduleMeetingUiEvent {
    public static final String SHOW_TOAST = "SHOW_TOAST";
    public static final String FINISH = "FINISH";

    private final String type;
    private final String message;

    private ScheduleMeetingUiEvent(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public static ScheduleMeetingUiEvent toast(String message) {
        return new ScheduleMeetingUiEvent(SHOW_TOAST, message);
    }

    public static ScheduleMeetingUiEvent finishScreen() {
        return new ScheduleMeetingUiEvent(FINISH, null);
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}
