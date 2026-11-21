package com.redsaludandina.insights.infrastructure.persistence;

import com.redsaludandina.insights.application.query.AccessLogLine;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Repositorio Spring Data del lado de LECTURA de la evidencia: proyecta al DTO. */
public interface AccessRecordQueryRepository extends Repository<AccessRecordEntity, UUID> {

    @Query("select new com.redsaludandina.insights.application.query.AccessLogLine(a.userId, a.reason, a.occurredAt)"
            + " from AccessRecordEntity a where a.resource = :resource and a.type = 'CLINICAL_READ'"
            + " order by a.occurredAt, a.id")
    List<AccessLogLine> findClinicalReads(@Param("resource") String resource);
}
