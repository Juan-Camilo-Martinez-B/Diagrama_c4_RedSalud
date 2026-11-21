package com.redsaludandina.scheduling.application.command;

import com.redsaludandina.shared.application.cqrs.Command;
import com.redsaludandina.shared.domain.ClinicalId;
import java.time.Instant;

/** Un especialista ofrece un cupo de teleconsulta. */
public record PublishSlotCommand(ClinicalId slotId, String specialistId, Instant start, Instant end)
        implements Command {
}
