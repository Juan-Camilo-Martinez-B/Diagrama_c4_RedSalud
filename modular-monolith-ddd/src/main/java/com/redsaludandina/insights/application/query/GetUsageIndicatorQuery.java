package com.redsaludandina.insights.application.query;

import com.redsaludandina.shared.application.cqrs.Query;

/** Indicadores de un centro en un mes ({@code period} con formato yyyy-MM). */
public record GetUsageIndicatorQuery(String centerId, String period) implements Query<UsageIndicatorView> {
}
