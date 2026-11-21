package com.redsaludandina.legacy.domain.model;

import com.redsaludandina.shared.domain.Require;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Historia clínica del paciente en el modelo CANÓNICO (nunca en el del legado), con su marca de
 * frescura. Episodios y documentos quedan del más reciente al más antiguo.
 */
public final class ClinicalHistory {

    private final String patientId;
    private final List<HistoricalEpisode> episodes;
    private final List<AvailableDocument> documents;
    private final FreshnessMark freshness;

    public ClinicalHistory(String patientId, List<HistoricalEpisode> episodes, List<AvailableDocument> documents,
                           FreshnessMark freshness) {
        this.patientId = Require.text(patientId, "patientId");
        this.episodes = Require.notNull(episodes, "episodes").stream()
                .sorted(Comparator.comparing(HistoricalEpisode::date).reversed())
                .toList();
        this.documents = Require.notNull(documents, "documents").stream()
                .sorted(Comparator.comparing(AvailableDocument::date).reversed())
                .toList();
        this.freshness = Require.notNull(freshness, "freshness");
    }

    public boolean isFresh(Instant now) {
        return freshness.isFresh(now);
    }

    public String patientId() {
        return patientId;
    }

    public List<HistoricalEpisode> episodes() {
        return episodes;
    }

    public List<AvailableDocument> documents() {
        return documents;
    }

    public FreshnessMark freshness() {
        return freshness;
    }
}
