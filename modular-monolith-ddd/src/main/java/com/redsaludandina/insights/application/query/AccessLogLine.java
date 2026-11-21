package com.redsaludandina.insights.application.query;

import java.time.Instant;

/** Modelo de lectura: quién leyó, por qué motivo y cuándo. */
public record AccessLogLine(String userId, String reason, Instant occurredAt) {
}
