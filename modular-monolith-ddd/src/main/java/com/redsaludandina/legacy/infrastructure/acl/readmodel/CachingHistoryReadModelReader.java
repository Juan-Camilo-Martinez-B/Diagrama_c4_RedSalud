package com.redsaludandina.legacy.infrastructure.acl.readmodel;

import com.redsaludandina.legacy.application.port.HistoryReadModelReader;
import com.redsaludandina.legacy.application.query.ClinicalHistoryView;
import java.util.Optional;

/** Lee el historial por la caché de dos niveles y solo baja a JPA en un fallo de caché. */
public final class CachingHistoryReadModelReader implements HistoryReadModelReader {

    private final HistoryReadModelReader delegate;
    private final TwoLevelHistoryCache cache;

    public CachingHistoryReadModelReader(HistoryReadModelReader delegate, TwoLevelHistoryCache cache) {
        this.delegate = delegate;
        this.cache = cache;
    }

    @Override
    public Optional<ClinicalHistoryView> find(String patientId) {
        return cache.find(patientId, delegate::find);
    }
}
