package com.redsaludandina.insights.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Repositorio Spring Data del lado de escritura de la evidencia: solo agregar, nunca modificar ni borrar. */
public interface AccessRecordJpaRepository extends Repository<AccessRecordEntity, UUID> {

    boolean existsById(UUID id);

    AccessRecordEntity save(AccessRecordEntity accessRecord);
}
