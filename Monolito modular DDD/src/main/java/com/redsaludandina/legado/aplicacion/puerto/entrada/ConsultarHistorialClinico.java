package com.redsaludandina.legado.aplicacion.puerto.entrada;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Servicio de Host Abierto (OHS) del historial clínico. Es lo ÚNICO de Integración legado que
 * otros contextos pueden usar (lo vigila la prueba de arquitectura de la ACL), por eso devuelve
 * sus propios tipos de vista y no el modelo de dominio.
 */
public interface ConsultarHistorialClinico {

    VistaHistorial consultar(Consulta consulta);

    /** {@code centroId} es el centro del paciente; {@code motivo} queda en la auditoría. */
    record Consulta(String usuarioId, String pacienteId, String centroId, String motivo) {
    }

    /** Historial canónico con su frescura visible: de dónde salió y hasta cuándo vale. */
    record VistaHistorial(String pacienteId, List<EpisodioVista> episodios, List<DocumentoVista> documentos,
                          Instant obtenidoEn, Instant caducaEn, String origen, boolean vigente) {
    }

    record EpisodioVista(String referencia, LocalDate fecha, String descripcion) {
    }

    record DocumentoVista(String referencia, String tipo, LocalDate fecha) {
    }
}
