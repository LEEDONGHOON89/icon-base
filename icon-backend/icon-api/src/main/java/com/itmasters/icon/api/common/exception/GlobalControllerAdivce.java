package com.itmasters.icon.api.common.exception;

import com.itmasters.icon.api.common.response.ErrorResponse;
import com.itmasters.icon.engine.exception.EngineException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalControllerAdivce {


    @ExceptionHandler(LoginFailedException.class)
    public ResponseEntity<ErrorResponse> handleLoginFailedException(LoginFailedException ex) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(errorResponse);
    }

    @ExceptionHandler(NoDataException.class)
    public ResponseEntity<ErrorResponse> noData(NoDataException ex) {
        log.warn("NoDataException: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }


    /**
     * 잘못된 파라미터 request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> MethodArgumentNotValidExceptionHandler(
            MethodArgumentNotValidException e) {
        log.error("MethodArgumentNotValidException: ", e);

        List<ObjectError> allErrors = e.getBindingResult().getAllErrors();
        List<String> messageList = new ArrayList<>();
        for (ObjectError allError : allErrors) {
            String typeName = null;
            if (allError instanceof FieldError) {
                FieldError allError1 = (FieldError) allError;
                typeName = allError1.getField();
            } else {
                typeName = allError.getObjectName();
            }

            String message = typeName + ": " + allError.getDefaultMessage();

            messageList.add(message);
        }

        String message = messageList.stream().collect(Collectors.joining(", "));
        ErrorResponse response = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message, messageList);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> MethodArgumentTypeMismatchExceptionHandler(MethodArgumentTypeMismatchException e) {
        String propertyName = e.getPropertyName();
        String value = e.getValue().toString();
        Class<?> requiredType = e.getRequiredType();

        String message = null;
        if (requiredType.isEnum()) {
            String enumList = Arrays.stream(requiredType.getEnumConstants())
                    .map(o -> o.toString())
                    .collect(Collectors.joining(","));
            message = "잘못된 파라미터입니다.\n파라미터명=%s, 전송한값=%s, 입력가능한 값=%s"
                    .formatted(propertyName, value, enumList);

        } else {
            message = "잘못된 파라미터입니다.\n파라미터명=%s, 전송한값=%s"
                    .formatted(propertyName, value);
        }

        ErrorResponse response = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }


    /**
     * 엔진 실행 중 발생한 치명적 오류 처리
     * (DB 에러, 스키마 불일치, 데이터 처리 실패 등)
     */
    @ExceptionHandler(EngineException.class)
    public ResponseEntity<ErrorResponse> handleEngineException(EngineException ex) {
        log.error("🚨 EngineException [{}]: execDsMpId={}, message={}",
                ex.getErrorCode(), ex.getExecDsMpId(), ex.getMessage(), ex);

        String detailedMessage = String.format("[%s] %s", ex.getErrorCode(), ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                detailedMessage
        );
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    /**
     * whereJson 검증 실패 등 비즈니스 검증 오류 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("IllegalArgumentException: ", ex);
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    @ExceptionHandler({Exception.class, RuntimeException.class})
    public ResponseEntity<?> handleNotFound(Exception ex) {
        log.error("Unhandled exception: ", ex);
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
}
