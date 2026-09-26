package com.tokenrealty.web.exception;

import com.tokenrealty.web.ProblemDetails;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class TokenRealtyExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TokenRealtyExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, "/errors/not-found", ex.getMessage());
    }

    @ExceptionHandler({ConflictException.class, IdempotencyConflictException.class})
    public ProblemDetail handleConflict(RuntimeException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, "/errors/conflict", ex.getMessage());
    }

    @ExceptionHandler({ValidationException.class, InsufficientFundsException.class, ComplianceBlockedException.class})
    public ProblemDetail handleValidation(RuntimeException ex) {
        return ProblemDetails.of(HttpStatus.UNPROCESSABLE_ENTITY, "/errors/business-rule", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleBeanValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                        (a, b) -> a
                ));
        ProblemDetail pd = ProblemDetails.of(HttpStatus.BAD_REQUEST, "/errors/validation", "Validation failed");
        pd.setProperty("fieldErrors", fieldErrors);
        return pd;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, "/errors/constraint", "Database constraint violation");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException ex) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, "/errors/bad-json", "Malformed JSON request");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, "/errors/type-mismatch",
                "Invalid parameter: " + ex.getName());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleEntityNotFound(EntityNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, "/errors/entity-not-found", "Entity not found");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ProblemDetails.of(HttpStatus.INTERNAL_SERVER_ERROR, "/errors/internal",
                "An unexpected error occurred");
    }
}
