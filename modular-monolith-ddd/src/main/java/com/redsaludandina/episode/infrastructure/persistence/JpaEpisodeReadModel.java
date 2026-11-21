package com.redsaludandina.episode.infrastructure.persistence;

import com.redsaludandina.episode.application.port.EpisodeReadModel;
import com.redsaludandina.episode.application.query.EpisodeView;
import java.util.Optional;

/** Adaptador del lado de lectura: proyecta las filas al DTO {@link EpisodeView}, sin pasar por el dominio. */
public final class JpaEpisodeReadModel implements EpisodeReadModel {

    private final EpisodeQueryRepository queries;

    public JpaEpisodeReadModel(EpisodeQueryRepository queries) {
        this.queries = queries;
    }

    @Override
    public Optional<EpisodeView> findById(String episodeId) {
        return queries.findById(episodeId).map(EpisodeEntity::toView);
    }

    @Override
    public Optional<String> attachmentStorageKey(String episodeId, String attachmentId) {
        return queries.findAttachmentStorageKey(episodeId, attachmentId);
    }
}
