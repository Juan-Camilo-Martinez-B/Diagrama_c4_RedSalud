package com.redsaludandina.legacy.infrastructure.acl.readmodel;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/** Repositorio Spring Data del lado de LECTURA del historial: proyección por interfaz, sin {@code save}. */
public interface HistoryReadModelQueryRepository extends Repository<HistoryReadModelEntity, String> {

    interface HistoryRow {
        String getPatientId();

        String getContent();

        Instant getObtainedAt();

        Instant getExpiresAt();

        String getSource();
    }

    Optional<HistoryRow> findByPatientId(String patientId);
}
