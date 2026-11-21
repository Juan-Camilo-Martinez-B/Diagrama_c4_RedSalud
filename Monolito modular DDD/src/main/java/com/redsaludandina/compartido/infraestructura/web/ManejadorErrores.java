package com.redsaludandina.compartido.infraestructura.web;

import com.redsaludandina.compartido.aplicacion.AccesoNoAutorizadoException;
import com.redsaludandina.compartido.aplicacion.RecursoNoEncontradoException;
import com.redsaludandina.compartido.dominio.ReglaDeNegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce los errores de aplicación y dominio a HTTP (RFC 9457). Una regla de negocio incumplida
 * es un 409 con su {@code codigo} estable, para que el cliente decida (por ejemplo, reintentar
 * contra el servidor).
 */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(ReglaDeNegocioException.class)
    ProblemDetail reglaDeNegocio(ReglaDeNegocioException error) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, error.getMessage());
        problema.setTitle("Regla de negocio");
        problema.setProperty("codigo", error.codigo());
        return problema;
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ProblemDetail noEncontrado(RecursoNoEncontradoException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, error.getMessage());
    }

    @ExceptionHandler(AccesoNoAutorizadoException.class)
    ProblemDetail accesoNoAutorizado(AccesoNoAutorizadoException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, error.getMessage());
    }

    @ExceptionHandler(NoAutenticadoException.class)
    ProblemDetail noAutenticado(NoAutenticadoException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, error.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail datosInvalidos(IllegalArgumentException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, error.getMessage());
    }
}
