package com.redsaludandina.episode.application.command;

import com.redsaludandina.shared.application.cqrs.Command;
import com.redsaludandina.shared.domain.ClinicalId;

/** Exige que el usuario pueda acceder al episodio (por ejemplo, antes de pedir una URL de subida). */
public record AuthorizeEpisodeAccessCommand(ClinicalId episodeId, String userId) implements Command {
}
