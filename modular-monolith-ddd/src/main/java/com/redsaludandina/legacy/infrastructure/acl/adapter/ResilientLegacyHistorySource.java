package com.redsaludandina.legacy.infrastructure.acl.adapter;

import com.redsaludandina.legacy.application.port.LegacyHistorySource;
import com.redsaludandina.legacy.application.port.LegacyUnavailableException;
import com.redsaludandina.legacy.domain.model.ClinicalHistory;
import com.redsaludandina.legacy.domain.model.FreshnessMark;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.time.Duration;
import java.util.Optional;

/**
 * Circuit breaker del legado (Resilience4j). Si el circuito abre, la consulta falla como legado no
 * disponible y el llamador conserva la copia caducada en vez de devolver un 500.
 */
public final class ResilientLegacyHistorySource implements LegacyHistorySource {

    private final LegacyHistorySource delegate;
    private final CircuitBreaker breaker;

    private ResilientLegacyHistorySource(LegacyHistorySource delegate, CircuitBreaker breaker) {
        this.delegate = delegate;
        this.breaker = breaker;
    }

    public static LegacyHistorySource around(LegacyHistorySource delegate) {
        CircuitBreaker breaker = CircuitBreaker.of("legacy-history", CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .build());
        return new ResilientLegacyHistorySource(delegate, breaker);
    }

    @Override
    public Optional<ClinicalHistory> fetch(String patientId, FreshnessMark freshness) {
        try {
            return breaker.executeSupplier(() -> delegate.fetch(patientId, freshness));
        } catch (CallNotPermittedException open) {
            throw new LegacyUnavailableException("El circuito del legado está abierto", open);
        }
    }
}
