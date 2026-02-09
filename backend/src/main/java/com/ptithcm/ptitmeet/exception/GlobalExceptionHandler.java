package com.ptithcm.ptitmeet.exception;

import com.ptithcm.ptitmeet.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = RuntimeException.class)
    ResponseEntity<ApiResponse> handlingRuntimeException(RuntimeException exception) {
        ApiResponse apiResponse = new ApiResponse();
        
        apiResponse.setCode(9999);
        apiResponse.setMessage(exception.getMessage());
        
        return ResponseEntity.badRequest().body(apiResponse);
    }
}