package com.redsaludandina.legacy.infrastructure.acl.readmodel;

import com.redsaludandina.legacy.application.port.HistoryReadModelWriter;
import com.redsaludandina.legacy.domain.model.AvailableDocument;
import com.redsaludandina.legacy.domain.model.ClinicalHistory;
import com.redsaludandina.legacy.domain.model.FreshnessMark;
import com.redsaludandina.legacy.domain.model.HistoricalEpisode;
import com.redsaludandina.legacy.domain.model.HistorySource;
import java.util.List;
import java.util.Optional;
import tools.jackson.databind.json.JsonMapper;

/** Escribe la copia canónica del historial en el modelo de lectura. */
public final class JpaHistoryReadModelWriter implements HistoryReadModelWriter {

    record Content(List<HistoricalEpisode> episodes, List<AvailableDocument> documents) {
    }

    private final HistoryReadModelJpaRepository jpa;
    private final JsonMapper json;

    public JpaHistoryReadModelWriter(HistoryReadModelJpaRepository jpa, JsonMapper json) {
        this.jpa = jpa;
        this.json = json;
    }

    @Override
    public Optional<FreshnessMark> freshnessOf(String patientId) {
        return jpa.findById(patientId).map(entity -> new FreshnessMark(entity.obtainedAt(), entity.expiresAt(),
                HistorySource.valueOf(entity.source())));
    }

    @Override
    public void save(ClinicalHistory history) {
        FreshnessMark freshness = history.freshness();
        String content = json.writeValueAsString(new Content(history.episodes(), history.documents()));
        jpa.save(new HistoryReadModelEntity(history.patientId(), content, freshness.obtainedAt(), freshness.expiresAt(),
                freshness.source().name()));
    }
}
