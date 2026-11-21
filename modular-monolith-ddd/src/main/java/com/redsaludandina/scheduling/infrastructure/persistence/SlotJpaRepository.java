package com.redsaludandina.scheduling.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.Repository;

/** Repositorio Spring Data del lado de escritura de Agenda. */
public interface SlotJpaRepository extends Repository<SlotEntity, String> {

    boolean existsById(String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SlotEntity> findWithLockById(String id);

    SlotEntity save(SlotEntity slot);
}
