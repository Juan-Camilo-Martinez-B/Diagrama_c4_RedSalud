package com.redsaludandina.legacy.infrastructure.acl.readmodel;

import java.util.Optional;
import org.springframework.data.repository.Repository;

/** Repositorio Spring Data del lado de escritura del modelo de lectura. */
public interface HistoryReadModelJpaRepository extends Repository<HistoryReadModelEntity, String> {

    Optional<HistoryReadModelEntity> findById(String patientId);

    HistoryReadModelEntity save(HistoryReadModelEntity history);
}
