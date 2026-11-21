package com.redsaludandina.legacy.application.command;

import com.redsaludandina.shared.application.cqrs.Command;

/** «Este usuario va a leer el historial del paciente por este motivo»: autoriza y audita (Caso §5). */
public record RecordHistoryReadCommand(String userId, String patientId, String centerId, String reason)
        implements Command {
}
