package com.redsaludandina.insights.application.port;

import com.redsaludandina.insights.application.query.UsageIndicatorView;
import java.util.Optional;

/** Lado de lectura de los indicadores de uso (puerto de salida). */
public interface UsageIndicatorReadModel {

    Optional<UsageIndicatorView> find(String centerId, String period);
}
