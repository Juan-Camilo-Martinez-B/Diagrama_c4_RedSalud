package com.redsaludandina.insights.application.port;

import com.redsaludandina.insights.domain.analytics.UsageIndicator;
import java.time.YearMonth;
import java.util.Optional;

/** Lado de escritura de los indicadores de uso (puerto de salida). */
public interface UsageIndicatorRepository {

    Optional<UsageIndicator> find(String centerId, YearMonth period);

    void save(UsageIndicator indicator);
}
