package com.redsaludandina.episode.application.port;

import com.redsaludandina.episode.domain.model.Episode;
import com.redsaludandina.shared.domain.ClinicalId;
import java.util.Optional;

/** Lado de escritura: carga y guarda el agregado {@link Episode} (puerto de salida). */
public interface EpisodeRepository {

    boolean exists(ClinicalId episodeId);

    Optional<Episode> findById(ClinicalId episodeId);

    void save(Episode episode);
}
