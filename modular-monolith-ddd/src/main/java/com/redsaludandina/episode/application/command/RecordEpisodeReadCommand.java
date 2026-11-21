package com.redsaludandina.episode.application.command;

import com.redsaludandina.shared.application.cqrs.Command;
import com.redsaludandina.shared.domain.ClinicalId;

/**
 * «Este usuario va a leer el episodio (o uno de sus adjuntos) por este motivo». Autoriza y deja la
 * lectura auditada ANTES de que la consulta, que no tiene efectos, devuelva los datos (Caso §5).
 * {@code attachmentId} es nulo si se lee el episodio completo.
 */
public record RecordEpisodeReadCommand(ClinicalId episodeId, ClinicalId attachmentId, String userId, String reason)
        implements Command {
}
