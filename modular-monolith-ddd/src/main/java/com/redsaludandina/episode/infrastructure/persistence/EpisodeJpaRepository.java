package com.redsaludandina.episode.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.repository.Repository;

/** Repositorio Spring Data del lado de escritura del episodio. */
public interface EpisodeJpaRepository extends Repository<EpisodeEntity, String> {

    boolean existsById(String id);

    Optional<EpisodeEntity> findById(String id);

    EpisodeEntity save(EpisodeEntity episode);
}
