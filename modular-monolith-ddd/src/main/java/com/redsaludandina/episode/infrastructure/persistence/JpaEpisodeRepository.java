package com.redsaludandina.episode.infrastructure.persistence;

import com.redsaludandina.episode.application.port.EpisodeRepository;
import com.redsaludandina.episode.domain.model.Episode;
import com.redsaludandina.shared.domain.ClinicalId;
import java.util.Optional;

/**
 * Adaptador del lado de escritura. Guarda copiando el agregado sobre la entidad ya cargada en la
 * transacción, así JPA actualiza solo lo que cambió y verifica la versión.
 */
public final class JpaEpisodeRepository implements EpisodeRepository {

    private final EpisodeJpaRepository jpa;

    public JpaEpisodeRepository(EpisodeJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean exists(ClinicalId episodeId) {
        return jpa.existsById(episodeId.value());
    }

    @Override
    public Optional<Episode> findById(ClinicalId episodeId) {
        return jpa.findById(episodeId.value()).map(EpisodeEntity::toDomain);
    }

    @Override
    public void save(Episode episode) {
        String id = episode.id().value();
        EpisodeEntity entity = jpa.findById(id).orElseGet(() -> new EpisodeEntity(id));
        entity.copyFrom(episode);
        jpa.save(entity);
    }
}
