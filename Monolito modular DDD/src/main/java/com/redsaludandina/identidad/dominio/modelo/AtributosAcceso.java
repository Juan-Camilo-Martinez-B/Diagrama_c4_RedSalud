package com.redsaludandina.identidad.dominio.modelo;

import com.redsaludandina.compartido.dominio.Requisitos;
import java.util.Optional;
import java.util.Set;

/**
 * Atributos con los que se decide el acceso además del rol (Caso §5): centro, especialidad y
 * relación con el paciente (los pacientes con los que el profesional tiene una atención vigente).
 */
public record AtributosAcceso(String centroId, String especialidad, Set<String> pacientesRelacionados) {

    public AtributosAcceso {
        centroId = centroId == null || centroId.isBlank() ? null : centroId.strip();
        especialidad = especialidad == null || especialidad.isBlank() ? null : especialidad.strip();
        pacientesRelacionados = Set.copyOf(Requisitos.noNulo(pacientesRelacionados, "pacientesRelacionados"));
    }

    public Optional<String> centro() {
        return Optional.ofNullable(centroId);
    }

    public Optional<String> especialidadDeclarada() {
        return Optional.ofNullable(especialidad);
    }

    public boolean tieneRelacionCon(String pacienteId) {
        return pacientesRelacionados.contains(pacienteId);
    }
}
