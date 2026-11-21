package com.redsaludandina.shared.infrastructure.persistence.inbox;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

/** Repositorio Spring Data común de los inbox; cada contexto consumidor declara el suyo concreto. */
@NoRepositoryBean
public interface InboxRecordRepository<T extends InboxRecordEntity> extends Repository<T, String> {

    boolean existsById(String id);

    T save(T inboxRecord);
}
