package com.ptithcm.ptitmeet.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.HandlerMethod;

import com.ptithcm.ptitmeet.controllers.AuthController;
import com.ptithcm.ptitmeet.dto.auth.LoginRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldMapAppExceptionToConfiguredStatusAndCode() {
        AppException exception = new AppException(ErrorCode.INVALID_LOGIN);

        var response = handler.handleAppException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ErrorCode.INVALID_LOGIN.getCode(), response.getBody().getCode());
        assertEquals(ErrorCode.INVALID_LOGIN.getMessage(), response.getBody().getMessage());
    }

    @Test
    void shouldReturnValidationErrorsAsFieldMap() throws NoSuchMethodException {
        LoginRequest target = LoginRequest.builder().email("").password("").build();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "loginRequest");
        bindingResult.addError(new FieldError("loginRequest", "email", "Email không hợp lệ"));
        bindingResult.addError(new FieldError("loginRequest", "password", "Mật khẩu không được để trống"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                new org.springframework.core.MethodParameter(
                        new HandlerMethod(new AuthController(null), AuthController.class.getMethod(
                                "login",
                                LoginRequest.class,
                                jakarta.servlet.http.HttpServletResponse.class)).getMethod(),
                        0),
                bindingResult);

        var response = handler.handleValidation(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Dữ liệu đầu vào không hợp lệ", response.getBody().getMessage());
        assertEquals("Email không hợp lệ", response.getBody().getData().get("email"));
        assertEquals("Mật khẩu không được để trống", response.getBody().getData().get("password"));
    }
}
