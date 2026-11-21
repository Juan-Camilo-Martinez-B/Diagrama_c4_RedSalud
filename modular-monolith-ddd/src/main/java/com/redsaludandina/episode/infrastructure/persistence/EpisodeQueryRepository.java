package com.redsaludandina.episode.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Repositorio Spring Data del lado de LECTURA del episodio: solo consultas, sin {@code save}. */
public interface EpisodeQueryRepository extends Repository<EpisodeEntity, String> {

    Optional<EpisodeEntity> findById(String id);

    @Query("select a.storageKey from EpisodeEntity e join e.attachments a"
            + " where e.id = :episodeId and a.id = :attachmentId")
    Optional<String> findAttachmentStorageKey(@Param("episodeId") String episodeId,
                                              @Param("attachmentId") String attachmentId);
}
