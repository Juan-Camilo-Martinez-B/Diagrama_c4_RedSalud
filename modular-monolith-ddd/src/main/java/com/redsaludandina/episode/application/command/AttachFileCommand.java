package com.redsaludandina.episode.application.command;

import com.redsaludandina.shared.application.cqrs.Command;
import com.redsaludandina.shared.domain.ClinicalId;

/** Asocia al episodio un adjunto que el dispositivo ya subió a S3 con la URL prefirmada. */
public record AttachFileCommand(ClinicalId episodeId, ClinicalId attachmentId, String type, String storageKey,
                                String sha256) implements Command {
}
