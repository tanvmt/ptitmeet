package com.ptithcm.ptitmeet.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // --- System / General (1xxx) ---
    UNCATEGORIZED_EXCEPTION(9999, "An unexpected system error occurred", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Invalid request data", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1002, "Authentication required", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1003, "You do not have permission to access this resource", HttpStatus.FORBIDDEN),

    // --- User / Auth (2xxx) ---
    USER_NOT_FOUND(2001, "User not found", HttpStatus.NOT_FOUND),
    USER_EXISTED(2002, "User already exists", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_USED(2003, "This email is already in use", HttpStatus.BAD_REQUEST),
    WRONG_PASSWORD(2004, "Incorrect password", HttpStatus.BAD_REQUEST),
    INVALID_LOGIN(2005, "Invalid email or password", HttpStatus.BAD_REQUEST),
    INVALID_GOOGLE_TOKEN_ID(2006, "Invalid Google ID Token", HttpStatus.BAD_REQUEST),
    INVALID_REFRESH_TOKEN(2007, "Refresh token is invalid or expired", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(2008, "Token is invalid or expired", HttpStatus.UNAUTHORIZED),
    INVALID_RESET_OTP(2009, "Invalid OTP", HttpStatus.BAD_REQUEST),
    EXPIRED_RESET_OTP(2010, "OTP has expired. Please request a new one.", HttpStatus.BAD_REQUEST),

    // --- Meeting (3xxx) ---
    MEETING_NOT_FOUND(3001, "Meeting not found", HttpStatus.NOT_FOUND),
    MEETING_ALREADY_FINISHED(3002, "Meeting has already ended or been cancelled", HttpStatus.BAD_REQUEST),
    MEETING_NOT_STARTED(3003, "Meeting has not started yet", HttpStatus.BAD_REQUEST),
    INVALID_MEETING_PASSWORD(3004, "Incorrect meeting password", HttpStatus.FORBIDDEN),
    HOST_ONLY_ACTION(3005, "Only the host can perform this action", HttpStatus.FORBIDDEN),
    PARTICIPANT_KICKED(3006, "You have been removed from the meeting", HttpStatus.FORBIDDEN),
    INVALID_TIME_RANGE(3007, "End time must be after start time", HttpStatus.BAD_REQUEST),
    CANNOT_GENERATE_CODE(3008, "System is busy, unable to generate meeting code. Please try again.",
            HttpStatus.SERVICE_UNAVAILABLE),
    MEETING_REJECTED(3009, "Your request to join the meeting has been rejected", HttpStatus.FORBIDDEN),
    MEETING_CANCELED(3010, "This meeting has been cancelled by the host", HttpStatus.BAD_REQUEST),
    UN_START_RECORD_MEETING_ROOM(3011, "Unable to start meeting recording", HttpStatus.BAD_REQUEST),
    UN_END_RECORD_MEETING_ROOM(3012, "Unable to save meeting recording", HttpStatus.BAD_REQUEST),
    USER_NOT_PARTICIPANT(3013, "You are not a participant of this meeting", HttpStatus.FORBIDDEN);

    private final int code;
    private final String message;
    private final HttpStatus statusCode;

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
