package com.ptithcm.ptitmeet.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class MeetingUiFormatter {

    private static final DateTimeFormatter INPUT_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter OUTPUT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault());
    private static final DateTimeFormatter OUTPUT_TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault());

    private MeetingUiFormatter() {
    }

    public static String formatTimeRange(String startTime, String endTime) {
        String start = formatDateTime(startTime);
        String end = formatClock(endTime);

        if (start.isEmpty() && end.isEmpty()) {
            return "Chưa có lịch";
        }
        if (end.isEmpty()) {
            return start;
        }
        return start + " - " + end;
    }

    public static String formatDateTime(String value) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(value, INPUT_FORMAT);
            return OUTPUT_DATE.format(dateTime) + " " + OUTPUT_TIME.format(dateTime);
        } catch (Exception ignored) {
            return value == null ? "" : value;
        }
    }

    public static String formatClock(String value) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(value, INPUT_FORMAT);
            return OUTPUT_TIME.format(dateTime);
        } catch (Exception ignored) {
            return value == null ? "" : value;
        }
    }
}
