package com.notify.shared.interfaces.rest;

import com.notify.shared.application.AccessDeniedException;
import com.notify.shared.application.AuthException;
import com.notify.shared.application.BusinessException;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String URI_PREFIX = "uri=";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        List<ErrorResponse.Violation> violations = ex
                .getBindingResult().getFieldErrors().stream().map(error -> ErrorResponse.Violation.builder()
                        .field(error.getField()).message(error.getDefaultMessage()).build())
                .collect(Collectors.toList());

        ErrorResponse body = ErrorResponse.builder().type("/errors/validation-error").title("Validation Error")
                .status(400).detail("The request contains invalid fields")
                .instance(request.getDescription(false).replace(URI_PREFIX, "")).violations(violations).build();

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
            WebRequest request) {
        List<ErrorResponse.Violation> violations = ex
                .getConstraintViolations().stream().map(v -> ErrorResponse.Violation.builder()
                        .field(v.getPropertyPath().toString()).message(v.getMessage()).build())
                .collect(Collectors.toList());

        ErrorResponse body = ErrorResponse.builder().type("/errors/validation-error").title("Validation Error")
                .status(400).detail("The request contains invalid fields")
                .instance(request.getDescription(false).replace(URI_PREFIX, "")).violations(violations).build();

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedPayload(HttpMessageNotReadableException ex,
            WebRequest request) {
        ErrorResponse body = ErrorResponse.builder().type("/errors/malformed-payload").title("Malformed request body")
                .status(400).detail(ex.getMessage()).instance(request.getDescription(false).replace(URI_PREFIX, ""))
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, WebRequest request) {
        ErrorResponse body = ErrorResponse.builder().type("/errors/business-error").title("Business Error")
                .status(ex.getStatus()).detail(ex.getMessage())
                .instance(request.getDescription(false).replace(URI_PREFIX, "")).build();

        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthException ex, WebRequest request) {
        ErrorResponse body = ErrorResponse.builder().type("/errors/auth-error").title("Authentication Error")
                .status(ex.getStatus()).detail(ex.getMessage())
                .instance(request.getDescription(false).replace(URI_PREFIX, "")).build();

        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        ErrorResponse body = ErrorResponse.builder().type("/errors/access-denied").title("Access Denied")
                .status(ex.getStatus()).detail(ex.getMessage())
                .instance(request.getDescription(false).replace(URI_PREFIX, "")).build();

        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleSpringAccessDenied(
            org.springframework.security.access.AccessDeniedException ex, WebRequest request) {
        ErrorResponse body = ErrorResponse.builder().type("/errors/access-denied").title("Access Denied").status(403)
                .detail("Insufficient permissions").instance(request.getDescription(false).replace(URI_PREFIX, ""))
                .build();

        return ResponseEntity.status(403).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, WebRequest request) {
        ErrorResponse body = ErrorResponse.builder().type("/errors/business-error").title("Business Error").status(409)
                .detail(ex.getMessage()).instance(request.getDescription(false).replace(URI_PREFIX, "")).build();
        return ResponseEntity.status(409).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, WebRequest request) {
        if (log.isErrorEnabled()) {
            log.error("Unhandled exception: {}", ex.getMessage(), ex);
        }
        ErrorResponse body = ErrorResponse.builder().type("/errors/internal-error").title("Internal Server Error")
                .status(500).detail("An unexpected error occurred")
                .instance(request.getDescription(false).replace("uri=", "")).build();

        return ResponseEntity.status(500).body(body);
    }
}
