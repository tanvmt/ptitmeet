package com.ptithcm.ptitmeet.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // --- Lỗi Hệ Thống/Chung (1xxx) ---
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi hệ thống chưa được định nghĩa", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Sai định dạng dữ liệu", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1002, "Vui lòng đăng nhập", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1003, "Bạn không có quyền truy cập", HttpStatus.FORBIDDEN),
    
    // --- Lỗi User/Auth (2xxx) ---
    USER_NOT_FOUND(2001, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
    USER_EXISTED(2002, "Người dùng đã tồn tại", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_USED(2003, "Email này đã được sử dụng", HttpStatus.BAD_REQUEST),
    WRONG_PASSWORD(2004, "Mật khẩu không chính xác", HttpStatus.BAD_REQUEST),

    // --- Lỗi Meeting (3xxx) ---
    MEETING_NOT_FOUND(3001, "Không tìm thấy phòng họp", HttpStatus.NOT_FOUND),
    MEETING_ALREADY_FINISHED(3002, "Cuộc họp đã kết thúc hoặc bị hủy", HttpStatus.BAD_REQUEST),
    MEETING_NOT_STARTED(3003, "Cuộc họp chưa bắt đầu", HttpStatus.BAD_REQUEST),
    INVALID_MEETING_PASSWORD(3004, "Mật khẩu tham gia không đúng", HttpStatus.FORBIDDEN),
    HOST_ONLY_ACTION(3005, "Chỉ chủ phòng mới có quyền thực hiện hành động này", HttpStatus.FORBIDDEN),
    PARTICIPANT_KICKED(3006, "Bạn đã bị mời ra khỏi phòng", HttpStatus.FORBIDDEN),
    INVALID_TIME_RANGE(3007, "Thời gian kết thúc phải sau thời gian bắt đầu", HttpStatus.BAD_REQUEST),
    CANNOT_GENERATE_CODE(3008, "Hệ thống đang bận, không thể tạo mã phòng. Vui lòng thử lại.", HttpStatus.SERVICE_UNAVAILABLE);

    private final int code; 
    private final String message;
    private final HttpStatus statusCode;

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}