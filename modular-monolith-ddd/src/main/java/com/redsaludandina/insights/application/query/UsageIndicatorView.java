package com.redsaludandina.insights.application.query;

/**
 * Modelo de lectura de los indicadores de uso. {@code averageWaitMinutes} y {@code closureRate} son
 * nulos mientras no haya reservas o interconsultas que los permitan calcular.
 */
public record UsageIndicatorView(String centerId, String period, long teleconsultations, long bookings,
                                 Long averageWaitMinutes, long referralsRequested, long referralsClosed,
                                 Double closureRate) {
}
