package com.redsaludandina.legacy.application.query;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Modelo de lectura del historial clínico, con su frescura visible para quien lo lee. */
public record ClinicalHistoryView(String patientId, List<EpisodeLine> episodes, List<DocumentLine> documents,
                                  Instant obtainedAt, Instant expiresAt, String source, boolean fresh) {

    public record EpisodeLine(String reference, LocalDate date, String description) {
    }

    public record DocumentLine(String reference, String type, LocalDate date) {
    }

    /** La misma vista con {@code fresh} calculado para el instante de la consulta. */
    public ClinicalHistoryView evaluatedAt(Instant now) {
        return new ClinicalHistoryView(patientId, episodes, documents, obtainedAt, expiresAt, source,
                now.isBefore(expiresAt));
    }
}
