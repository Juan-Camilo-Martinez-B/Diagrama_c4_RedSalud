package com.redsaludandina.legacy.application.query;

import com.redsaludandina.shared.application.cqrs.Query;

/**
 * Servicio de host abierto (OHS) del historial: lee SOLO el modelo de lectura, sin tocar el legado ni
 * cambiar estado. La API ejecuta antes {@code RecordHistoryReadCommand} y
 * {@code RefreshClinicalHistoryCommand}.
 */
public record GetClinicalHistoryQuery(String patientId) implements Query<ClinicalHistoryView> {
}
