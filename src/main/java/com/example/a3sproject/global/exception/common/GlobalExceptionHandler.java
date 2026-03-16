package com.example.a3sproject.global.exception.common;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.example.a3sproject.global.dto.ApiResponseDto;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 도메인 커스텀 예외 (UserException, PaymentException 등 ServiceException 하위 전부 처리)
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleServiceException(ServiceException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponseDto.error(
                        ex.getStatus().toString(),
                        ex.getCode() + ": " + ex.getMessage())
                );
    }

    // Spring Security - 인가 실패 (403)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponseDto.error(
                        HttpStatus.FORBIDDEN.toString(),
                        ErrorCode.USER_FORBIDDEN.name() + ": " + ErrorCode.USER_FORBIDDEN.getMessage())
                );
    }

    // Spring Security - 인증 실패 (401)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponseDto.error(
                        HttpStatus.UNAUTHORIZED.toString(),
                        ErrorCode.USER_UNAUTHORIZED.name() + ": " + ErrorCode.USER_UNAUTHORIZED.getMessage())
                );
    }

    // JWT - 토큰 만료 (401)
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleExpiredJwtException(ExpiredJwtException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponseDto.error(
                        HttpStatus.UNAUTHORIZED.toString(),
                        "TOKEN_EXPIRED: 토큰이 만료되었습니다.")
                );
    }

    // JWT - 잘못된 형식 (401)
    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleMalformedJwtException(MalformedJwtException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponseDto.error(
                        HttpStatus.UNAUTHORIZED.toString(),
                        "TOKEN_MALFORMED: 유효하지 않은 토큰입니다.")
                );
    }

    // JWT - 서명 오류 (401)
    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleSignatureException(SignatureException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponseDto.error(
                        HttpStatus.UNAUTHORIZED.toString(),
                        "TOKEN_SIGNATURE_INVALID: 토큰 서명이 올바르지 않습니다.")
                );
    }

     // @Valid 유효성 검증 실패 (400) 필드별 에러 메시지를 Map으로 반환
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDto<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDto.errorWithMap(
                        errors,
                        HttpStatus.BAD_REQUEST.toString(),
                        ErrorCode.INVALID_INPUT.name() + ": " + ErrorCode.INVALID_INPUT.getMessage())
                );
    }

    // 필수 요청 파라미터 누락 (400)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDto.error(
                        HttpStatus.BAD_REQUEST.toString(),
                        ErrorCode.INVALID_INPUT.name() + ": " + ex.getMessage())
                );
    }

    // 그 외 처리되지 않은 모든 예외 (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto<Void>> handleException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.error(
                        HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                        ErrorCode.INTERNAL_SERVER_ERROR.name() + ": " + ErrorCode.INTERNAL_SERVER_ERROR.getMessage())
                );
    }
}
