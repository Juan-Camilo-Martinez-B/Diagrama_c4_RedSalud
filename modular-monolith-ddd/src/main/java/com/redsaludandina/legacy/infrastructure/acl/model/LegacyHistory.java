package com.redsaludandina.legacy.infrastructure.acl.model;

import java.util.List;

/**
 * El historial TAL COMO LO ENTREGA el legado: códigos, fechas en texto yyyyMMdd y glosas que pueden
 * venir vacías. Este modelo nunca sale de la capa anticorrupción.
 */
public record LegacyHistory(String codPaciente, List<LegacyAttention> attentions, List<LegacyDocument> documents) {

    public record LegacyAttention(String nroAtencion, String fecAtencion, String glosa) {
    }

    public record LegacyDocument(String codDocumento, String tipDocumento, String fecDocumento) {
    }
}
