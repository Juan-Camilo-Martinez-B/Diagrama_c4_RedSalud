package com.redsaludandina.insights.infrastructure.persistence;

import com.redsaludandina.insights.application.port.UsageIndicatorRepository;
import com.redsaludandina.insights.domain.analytics.UsageIndicator;
import java.time.YearMonth;
import java.util.Optional;

public final class JpaUsageIndicatorRepository implements UsageIndicatorRepository {

    private final UsageIndicatorJpaRepository jpa;

    public JpaUsageIndicatorRepository(UsageIndicatorJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<UsageIndicator> find(String centerId, YearMonth period) {
        return jpa.findById(UsageIndicatorEntity.idOf(centerId, period.toString())).map(UsageIndicatorEntity::toDomain);
    }

    @Override
    public void save(UsageIndicator indicator) {
        jpa.save(UsageIndicatorEntity.fromDomain(indicator));
    }
}
