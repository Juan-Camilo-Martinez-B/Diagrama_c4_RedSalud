package com.redsaludandina.identidad.dominio.modelo;

import com.redsaludandina.compartido.dominio.Requisitos;
import java.util.Optional;

/**
 * Dato clínico al que se quiere acceder, descrito por lo que importa para autorizar: de qué
 * paciente es, de qué centro y, si aplica, de qué especialidad.
 */
public record RecursoClinico(String referencia, String pacienteId, String centroId, String especialidad) {

    public RecursoClinico {
        referencia = Requisitos.texto(referencia, "referencia");
        pacienteId = Requisitos.texto(pacienteId, "pacienteId");
        centroId = Requisitos.texto(centroId, "centroId");
        especialidad = especialidad == null || especialidad.isBlank() ? null : especialidad.strip();
    }

    public Optional<String> especialidadRequerida() {
        return Optional.ofNullable(especialidad);
    }
}
