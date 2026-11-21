package com.redsaludandina.episode.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.repository.Repository;

/**
 * Repositorio Spring Data del lado de LECTURA de los recibos: proyección por interfaz con solo lo que
 * la consulta necesita (la respuesta R y si fue repetición).
 */
public interface SyncReceiptQueryRepository extends Repository<SyncReceiptEntity, String> {

    interface SyncReceiptRow {
        String getResult();

        boolean isReplayed();
    }

    Optional<SyncReceiptRow> findByIdempotencyKey(String idempotencyKey);
}
