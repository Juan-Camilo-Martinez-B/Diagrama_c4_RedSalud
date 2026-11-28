package com.redsaludandina.legacy.infrastructure.acl.readmodel;

import com.redsaludandina.legacy.application.port.HistoryReadModelWriter;
import com.redsaludandina.legacy.domain.model.ClinicalHistory;
import com.redsaludandina.legacy.domain.model.FreshnessMark;
import java.util.Optional;

/** Persiste la copia y tira la caché de ese paciente para que la siguiente lectura vea la nueva. */
public final class CachingHistoryReadModelWriter implements HistoryReadModelWriter {

    private final HistoryReadModelWriter delegate;
    private final TwoLevelHistoryCache cache;

    public CachingHistoryReadModelWriter(HistoryReadModelWriter delegate, TwoLevelHistoryCache cache) {
        this.delegate = delegate;
        this.cache = cache;
    }

    @Override
    public Optional<FreshnessMark> freshnessOf(String patientId) {
        return delegate.freshnessOf(patientId);
    }

    @Override
    public void save(ClinicalHistory history) {
        delegate.save(history);
        cache.invalidate(history.patientId());
    }
}
