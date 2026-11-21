package com.redsaludandina.episode.application.command;

import com.redsaludandina.shared.application.cqrs.Command;
import com.redsaludandina.shared.domain.ClinicalId;

/** Cierra la teleconsulta: el episodio pasa a atendido y se publica {@code PatientAttended}. */
public record CloseTeleconsultationCommand(ClinicalId episodeId) implements Command {
}
