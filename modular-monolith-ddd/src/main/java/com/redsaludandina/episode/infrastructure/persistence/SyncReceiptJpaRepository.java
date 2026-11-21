package com.redsaludandina.episode.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.repository.Repository;

/** Repositorio Spring Data del lado de escritura de los recibos de sincronización. */
public interface SyncReceiptJpaRepository extends Repository<SyncReceiptEntity, String> {

    Optional<SyncReceiptEntity> findById(String idempotencyKey);

    SyncReceiptEntity save(SyncReceiptEntity receipt);
}
