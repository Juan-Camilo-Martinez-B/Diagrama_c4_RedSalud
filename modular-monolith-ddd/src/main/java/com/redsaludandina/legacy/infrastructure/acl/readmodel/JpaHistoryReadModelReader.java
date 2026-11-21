package com.redsaludandina.legacy.infrastructure.acl.readmodel;

import com.redsaludandina.legacy.application.port.HistoryReadModelReader;
import com.redsaludandina.legacy.application.query.ClinicalHistoryView;
import com.redsaludandina.legacy.application.query.ClinicalHistoryView.DocumentLine;
import com.redsaludandina.legacy.application.query.ClinicalHistoryView.EpisodeLine;
import com.redsaludandina.legacy.infrastructure.acl.readmodel.HistoryReadModelQueryRepository.HistoryRow;
import java.util.List;
import java.util.Optional;
import tools.jackson.databind.json.JsonMapper;

/** Lee el modelo de lectura y lo proyecta directo al DTO de la consulta (sin pasar por el dominio). */
public final class JpaHistoryReadModelReader implements HistoryReadModelReader {

    record ViewContent(List<EpisodeLine> episodes, List<DocumentLine> documents) {
    }

    private final HistoryReadModelQueryRepository queries;
    private final JsonMapper json;

    public JpaHistoryReadModelReader(HistoryReadModelQueryRepository queries, JsonMapper json) {
        this.queries = queries;
        this.json = json;
    }

    @Override
    public Optional<ClinicalHistoryView> find(String patientId) {
        return queries.findByPatientId(patientId).map(this::toView);
    }

    private ClinicalHistoryView toView(HistoryRow row) {
        ViewContent content = json.readValue(row.getContent(), ViewContent.class);
        return new ClinicalHistoryView(row.getPatientId(), content.episodes(), content.documents(), row.getObtainedAt(),
                row.getExpiresAt(), row.getSource(), false);
    }
}
