package com.redsaludandina.legacy.infrastructure.acl.job;

import com.redsaludandina.legacy.application.port.HistoryReadModelWriter;
import com.redsaludandina.legacy.domain.model.FreshnessMark;
import com.redsaludandina.legacy.domain.model.HistorySource;
import com.redsaludandina.legacy.infrastructure.acl.client.soap.LegacySoapClient;
import com.redsaludandina.legacy.infrastructure.acl.model.LegacyHistory;
import com.redsaludandina.legacy.infrastructure.acl.translator.CanonicalTranslator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Job nocturno de la ACL (Caso §3): pide al SOAP del legado los historiales cambiados desde la última
 * corrida, los traduce y los deja en el modelo de lectura con un TTL nuevo.
 */
public final class NightlyMaterializationJob {

    static final String NAME = "nightly-materialization";

    private final LegacySoapClient soap;
    private final CanonicalTranslator translator;
    private final HistoryReadModelWriter readModel;
    private final JobStateRepository state;
    private final Clock clock;
    private final Duration ttl;

    public NightlyMaterializationJob(LegacySoapClient soap, CanonicalTranslator translator,
                                     HistoryReadModelWriter readModel, JobStateRepository state, Clock clock,
                                     Duration ttl) {
        this.soap = soap;
        this.translator = translator;
        this.readModel = readModel;
        this.state = state;
        this.clock = clock;
        this.ttl = ttl;
    }

    /** Devuelve cuántos historiales rematerializó. */
    public int run() {
        Instant now = clock.instant();
        Instant since = state.findById(NAME).map(JobStateEntity::lastRun).orElse(Instant.EPOCH);
        FreshnessMark freshness = FreshnessMark.withTtl(now, ttl, HistorySource.READ_MODEL);
        List<LegacyHistory> updated = soap.historiesUpdatedSince(since);
        updated.forEach(legacy -> readModel.save(translator.translate(legacy, freshness)));
        state.save(new JobStateEntity(NAME, now));
        return updated.size();
    }
}
