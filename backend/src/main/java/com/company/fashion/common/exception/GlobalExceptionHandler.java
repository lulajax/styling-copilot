package com.company.fashion.common.exception;

import com.company.fashion.common.api.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<Result<Void>> handleBusiness(BusinessException ex) {
    HttpStatus status = ex.getCode() >= 400 && ex.getCode() < 600
        ? HttpStatus.valueOf(ex.getCode())
        : HttpStatus.BAD_REQUEST;
    return ResponseEntity.status(status).body(Result.error(ex.getCode(), ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException ex) {
    FieldError first = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
    String msg = first == null ? "Invalid request" : first.getField() + " " + first.getDefaultMessage();
    return ResponseEntity.badRequest().body(Result.error(400, msg));
  }

  @ExceptionHandler({
      MissingServletRequestParameterException.class,
      MissingServletRequestPartException.class,
      MethodArgumentTypeMismatchException.class,
      MultipartException.class
  })
  public ResponseEntity<Result<Void>> handleBadRequest(Exception ex) {
    return ResponseEntity.badRequest().body(Result.error(400, ex.getMessage()));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<Result<Void>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
    return ResponseEntity.badRequest().body(Result.error(400, "file size exceeds 5MB"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Result<Void>> handleOthers(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Result.error(500, ex.getMessage()));
  }
}
