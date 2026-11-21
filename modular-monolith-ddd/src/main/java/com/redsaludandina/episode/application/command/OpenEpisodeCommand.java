package com.redsaludandina.episode.application.command;

import com.redsaludandina.shared.application.cqrs.Command;
import com.redsaludandina.shared.domain.ClinicalId;

/** Abre un episodio con el ULID generado en el dispositivo; {@code slotId} es opcional. */
public record OpenEpisodeCommand(ClinicalId episodeId, String patientId, ClinicalId slotId, String centerId)
        implements Command {
}
