package com.redsaludandina.shared.infrastructure.web;

import com.redsaludandina.shared.application.DuplicateRecordException;
import com.redsaludandina.shared.application.ForbiddenAccessException;
import com.redsaludandina.shared.application.ResourceNotFoundException;
import com.redsaludandina.shared.domain.BusinessRuleException;
import java.time.format.DateTimeParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce las excepciones de dominio y aplicación a respuestas HTTP (RFC 9457). */
@RestControllerAdvice
public class ErrorHandler {

    public static final String CODE = "code";

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail businessRule(BusinessRuleException error) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, error.getMessage());
        problem.setTitle("Regla de negocio");
        problem.setProperty(CODE, error.code());
        return problem;
    }

    @ExceptionHandler(DuplicateRecordException.class)
    ProblemDetail duplicate(DuplicateRecordException error) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, error.getMessage());
        problem.setProperty(CODE, "DUPLICATE_RECORD");
        return problem;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail notFound(ResourceNotFoundException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, error.getMessage());
    }

    @ExceptionHandler(ForbiddenAccessException.class)
    ProblemDetail forbidden(ForbiddenAccessException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, error.getMessage());
    }

    @ExceptionHandler(UnauthenticatedException.class)
    ProblemDetail unauthenticated(UnauthenticatedException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, error.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, DateTimeParseException.class})
    ProblemDetail invalidData(RuntimeException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, error.getMessage());
    }
}
