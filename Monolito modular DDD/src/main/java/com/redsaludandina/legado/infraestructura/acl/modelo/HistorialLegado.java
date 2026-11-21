package com.redsaludandina.legado.infraestructura.acl.modelo;

import java.util.List;

/**
 * Historial tal como lo entrega el LEGADO: sus nombres, sus códigos y sus fechas en texto
 * {@code yyyyMMdd}. No sale de la capa anticorrupción.
 *
 * <p>SUPUESTO: el caso no describe el esquema del legado; estas estructuras (HC_PACIENTE,
 * HC_ATENCION, HC_DOCUMENTO) son un modelo de referencia que se reemplaza por el real.
 */
public record HistorialLegado(String codPaciente, List<AtencionLegado> atenciones, List<DocumentoLegado> documentos) {

    /** Fila de HC_ATENCION. {@code fecAtencion} en formato yyyyMMdd. */
    public record AtencionLegado(String nroAtencion, String fecAtencion, String glosa) {
    }

    /** Fila de HC_DOCUMENTO. {@code tipDocumento}: LAB, RX, ECO, INF... */
    public record DocumentoLegado(String codDocumento, String tipDocumento, String fecDocumento) {
    }
}
