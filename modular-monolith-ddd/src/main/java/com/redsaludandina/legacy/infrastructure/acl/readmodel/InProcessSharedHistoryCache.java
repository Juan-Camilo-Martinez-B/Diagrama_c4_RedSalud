package com.redsaludandina.legacy.infrastructure.acl.readmodel;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.redsaludandina.legacy.application.query.ClinicalHistoryView;
import java.time.Duration;
import java.util.Optional;

/** Nivel 2 en proceso. El perfil aws lo sustituye por ElastiCache sin cambiar a quien lee. */
public final class InProcessSharedHistoryCache implements SharedHistoryCache {

    private final Cache<String, ClinicalHistoryView> entries = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(2_000)
            .build();

    @Override
    public Optional<ClinicalHistoryView> find(String patientId) {
        return Optional.ofNullable(entries.getIfPresent(patientId));
    }

    @Override
    public void put(String patientId, ClinicalHistoryView view) {
        entries.put(patientId, view);
    }

    @Override
    public void invalidate(String patientId) {
        entries.invalidate(patientId);
    }
}
