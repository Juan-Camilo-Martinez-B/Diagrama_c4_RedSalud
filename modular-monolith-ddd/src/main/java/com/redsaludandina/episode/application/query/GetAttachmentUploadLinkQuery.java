package com.redsaludandina.episode.application.query;

import com.redsaludandina.shared.application.cqrs.Query;
import com.redsaludandina.shared.domain.ClinicalId;

/** URL prefirmada para subir un adjunto (la API autoriza antes con {@code AuthorizeEpisodeAccessCommand}). */
public record GetAttachmentUploadLinkQuery(ClinicalId episodeId, ClinicalId attachmentId, String sha256)
        implements Query<UploadLink> {
}
