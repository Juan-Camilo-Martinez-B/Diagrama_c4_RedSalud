package com.redsaludandina.legacy.infrastructure.acl.readmodel;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.redsaludandina.legacy.application.query.ClinicalHistoryView;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

/**
 * Caché de dos niveles del historial: Caffeine en la instancia y la caché compartida detrás.
 * Un acierto evita la base. Una escritura invalida los dos niveles.
 */
public final class TwoLevelHistoryCache {

    private final Cache<String, ClinicalHistoryView> local = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(2))
            .maximumSize(500)
            .build();
    private final SharedHistoryCache shared;

    public TwoLevelHistoryCache(SharedHistoryCache shared) {
        this.shared = shared;
    }

    public Optional<ClinicalHistoryView> find(String patientId, Function<String, Optional<ClinicalHistoryView>> load) {
        ClinicalHistoryView hit = local.getIfPresent(patientId);
        if (hit != null) {
            return Optional.of(hit);
        }
        return fromSharedOrLoad(patientId, load);
    }

    public void invalidate(String patientId) {
        local.invalidate(patientId);
        shared.invalidate(patientId);
    }

    private Optional<ClinicalHistoryView> fromSharedOrLoad(String patientId,
                                                           Function<String, Optional<ClinicalHistoryView>> load) {
        Optional<ClinicalHistoryView> sharedHit = shared.find(patientId);
        if (sharedHit.isPresent()) {
            local.put(patientId, sharedHit.get());
            return sharedHit;
        }
        return loadAndStore(patientId, load);
    }

    private Optional<ClinicalHistoryView> loadAndStore(String patientId,
                                                       Function<String, Optional<ClinicalHistoryView>> load) {
        Optional<ClinicalHistoryView> loaded = load.apply(patientId);
        loaded.ifPresent(view -> store(patientId, view));
        return loaded;
    }

    private void store(String patientId, ClinicalHistoryView view) {
        shared.put(patientId, view);
        local.put(patientId, view);
    }
}
