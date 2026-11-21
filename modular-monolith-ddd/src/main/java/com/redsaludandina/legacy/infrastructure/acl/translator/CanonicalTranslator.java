package com.redsaludandina.legacy.infrastructure.acl.translator;

import com.redsaludandina.legacy.domain.model.AvailableDocument;
import com.redsaludandina.legacy.domain.model.ClinicalHistory;
import com.redsaludandina.legacy.domain.model.FreshnessMark;
import com.redsaludandina.legacy.domain.model.HistoricalEpisode;
import com.redsaludandina.legacy.infrastructure.acl.model.LegacyHistory;
import com.redsaludandina.legacy.infrastructure.acl.model.LegacyHistory.LegacyAttention;
import com.redsaludandina.legacy.infrastructure.acl.model.LegacyHistory.LegacyDocument;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Traductor de la capa anticorrupción (Caso §7): convierte el modelo del legado al modelo canónico.
 * Limpia espacios, interpreta fechas yyyyMMdd, nombra los tipos de documento y completa glosas vacías.
 */
public final class CanonicalTranslator {

    private static final String NO_GLOSS = "(sin glosa)";

    private static final Map<String, String> DOCUMENT_TYPES = Map.of(
            "LAB", "Laboratorio",
            "RX", "Radiografía",
            "ECO", "Ecografía",
            "INF", "Informe");

    /** Hoy el legado identifica al paciente con el mismo código; si cambia, solo cambia aquí. */
    public String legacyCode(String patientId) {
        return patientId;
    }

    public ClinicalHistory translate(LegacyHistory legacy, FreshnessMark freshness) {
        return new ClinicalHistory(legacy.codPaciente().strip(),
                legacy.attentions().stream().map(CanonicalTranslator::episode).toList(),
                legacy.documents().stream().map(CanonicalTranslator::document).toList(),
                freshness);
    }

    private static HistoricalEpisode episode(LegacyAttention attention) {
        String description = Optional.ofNullable(attention.glosa()).map(String::strip).filter(text -> !text.isEmpty())
                .orElse(NO_GLOSS);
        return new HistoricalEpisode(attention.nroAtencion().strip(), date(attention.fecAtencion()), description);
    }

    private static AvailableDocument document(LegacyDocument document) {
        return new AvailableDocument(document.codDocumento().strip(), documentType(document.tipDocumento()),
                date(document.fecDocumento()));
    }

    private static String documentType(String code) {
        String clean = Optional.ofNullable(code).orElse("").strip().toUpperCase(Locale.ROOT);
        return DOCUMENT_TYPES.getOrDefault(clean, "Otro (" + clean + ")");
    }

    private static LocalDate date(String yyyymmdd) {
        try {
            return LocalDate.parse(yyyymmdd.strip(), DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException | NullPointerException invalid) {
            throw new IllegalStateException("Fecha inválida en el legado: " + yyyymmdd, invalid);
        }
    }
}
