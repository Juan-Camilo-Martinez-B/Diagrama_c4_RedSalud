package com.redsaludandina.insights.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.repository.Repository;

/** Repositorio Spring Data del lado de escritura de los indicadores. */
public interface UsageIndicatorJpaRepository extends Repository<UsageIndicatorEntity, String> {

    Optional<UsageIndicatorEntity> findById(String id);

    UsageIndicatorEntity save(UsageIndicatorEntity indicator);
}
