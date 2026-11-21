package com.redsaludandina.legacy.application.command;

import com.redsaludandina.shared.application.cqrs.Command;

/**
 * Asegura que el modelo de lectura tenga una copia vigente del historial: si caducó (TTL) o no
 * existe, la trae del legado a través de la capa anticorrupción (lado de escritura de CQRS).
 */
public record RefreshClinicalHistoryCommand(String patientId) implements Command {
}
