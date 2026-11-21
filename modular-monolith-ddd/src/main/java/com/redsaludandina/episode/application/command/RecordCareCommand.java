package com.redsaludandina.episode.application.command;

import com.redsaludandina.shared.application.cqrs.Command;
import com.redsaludandina.shared.domain.ClinicalId;
import java.time.Instant;

/** Registra un campo de la nota de atención con el sello del centro que lo escribió (online u offline). */
public record RecordCareCommand(ClinicalId episodeId, String field, String text, String centerId,
                                Instant stampedAt) implements Command {
}
