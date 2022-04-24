package com.moyeorun.auth.global.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyeorun.auth.global.common.response.ErrorResponseBody;
import com.moyeorun.auth.global.error.exception.BusinessException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final ObjectMapper objectMapper;

  @ExceptionHandler(BusinessException.class)
  protected ResponseEntity<?> handleBusinessException(BusinessException e) throws IOException {
    log.info(e.getMessage());

    ErrorResponseBody errorResponseBody = new ErrorResponseBody(e.getErrorCode());

    return ResponseEntity.status(e.getErrorCode().getStatusCode())
        .contentType(MediaType.valueOf(MediaType.APPLICATION_JSON_VALUE))
        .body(objectMapper.writeValueAsString(errorResponseBody));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  private ResponseEntity<?> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException e) throws IOException {
    log.error(e.getMessage());

    ErrorCode code = ErrorCode.INVALID_INPUT_VALUE;
    ErrorResponseBody errorResponseBody = new ErrorResponseBody(code);

    return ResponseEntity.status(code.getStatusCode())
        .contentType(MediaType.valueOf(MediaType.APPLICATION_JSON_VALUE))
        .body(objectMapper.writeValueAsString(errorResponseBody));
  }

  @ExceptionHandler(Exception.class)
  protected ResponseEntity<?> handleException(Exception e) throws IOException {
    log.error("Not handle Exception", e);
    ErrorCode internalError = ErrorCode.INTERNAL_SERVER_ERROR;
    ErrorResponseBody errorResponseBody = new ErrorResponseBody(internalError);

    return ResponseEntity.status(internalError.getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
        .body(objectMapper.writeValueAsString(errorResponseBody));
  }
}
