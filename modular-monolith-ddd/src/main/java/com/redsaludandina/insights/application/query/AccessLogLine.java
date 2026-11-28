package com.redsaludandina.insights.application.query;

import java.time.Instant;

/**
 * Línea del reporte de evidencia. {@code type} distingue la lectura clínica, la denegación y el
 * conflicto last-writer-wins. {@code detail} trae los dos valores cuando hubo conflicto.
 */
public record AccessLogLine(String userId, String reason, Instant occurredAt, String type, String detail) {
}
