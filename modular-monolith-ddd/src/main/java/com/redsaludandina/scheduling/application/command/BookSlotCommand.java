package com.redsaludandina.scheduling.application.command;

import com.redsaludandina.shared.application.cqrs.Command;
import com.redsaludandina.shared.domain.ClinicalId;

/** Un paciente reserva un cupo desde su centro. La reserva se resuelve en el servidor. */
public record BookSlotCommand(ClinicalId slotId, ClinicalId bookingId, String patientId, String centerId)
        implements Command {
}
