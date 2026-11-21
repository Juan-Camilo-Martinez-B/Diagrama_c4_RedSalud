package com.redsaludandina.shared.infrastructure.persistence.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/** Repositorio Spring Data común de las tablas outbox; cada contexto declara el suyo concreto. */
@NoRepositoryBean
public interface OutboxRecordRepository<T extends OutboxRecordEntity> extends Repository<T, Long> {

    T save(T outboxRecord);

    List<T> findByPublishedAtIsNullOrderBySequenceAsc(Limit limit);

    @Transactional
    @Modifying
    @Query("update #{#entityName} o set o.publishedAt = :publishedAt where o.eventId = :eventId")
    int markPublished(@Param("eventId") UUID eventId, @Param("publishedAt") Instant publishedAt);

    long countByType(String type);
}
