package com.redsaludandina.auditoria.infraestructura.adaptador.entrada.web;

import com.redsaludandina.auditoria.aplicacion.puerto.entrada.ConsultarQuienVio;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reporte «quién vio este documento y con qué motivo» (Caso §5).
 *
 * <p>PENDIENTE: restringirlo a un rol de auditoría cuando esté el adaptador de Cognito; el caso no
 * define ese rol.
 */
@RestController
public class AuditoriaControlador {

    private final ConsultarQuienVio consultarQuienVio;

    public AuditoriaControlador(ConsultarQuienVio consultarQuienVio) {
        this.consultarQuienVio = consultarQuienVio;
    }

    record LineaReporte(String usuarioId, String motivo, Instant ocurridoEn) {
    }

    @GetMapping("/auditoria/accesos")
    List<LineaReporte> quienVio(@RequestParam String recurso) {
        return consultarQuienVio.quienVio(recurso).stream()
                .map(acceso -> new LineaReporte(acceso.usuarioId(), acceso.motivo(), acceso.ocurridoEn()))
                .toList();
    }
}
