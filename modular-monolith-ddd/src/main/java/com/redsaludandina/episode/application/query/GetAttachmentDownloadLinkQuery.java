package com.redsaludandina.episode.application.query;

import com.redsaludandina.shared.application.cqrs.Query;

/** URL prefirmada para descargar un adjunto (la API audita antes con {@code RecordEpisodeReadCommand}). */
public record GetAttachmentDownloadLinkQuery(String episodeId, String attachmentId) implements Query<DownloadLink> {
}
