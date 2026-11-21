package com.redsaludandina.episode.application.port;

import com.redsaludandina.episode.application.query.EpisodeView;
import java.util.Optional;

/** Lado de lectura del episodio: DTO de lectura, nunca el agregado (puerto de salida). */
public interface EpisodeReadModel {

    Optional<EpisodeView> findById(String episodeId);

    Optional<String> attachmentStorageKey(String episodeId, String attachmentId);
}
