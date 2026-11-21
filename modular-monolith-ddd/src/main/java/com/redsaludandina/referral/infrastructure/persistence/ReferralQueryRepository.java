package com.redsaludandina.referral.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Repositorio Spring Data del lado de LECTURA de Interconsulta: solo consultas. */
public interface ReferralQueryRepository extends Repository<ReferralEntity, String> {

    @Query("select distinct r from ReferralEntity r join r.alerts a"
            + " where r.centerId = :centerId and a.resolvedAt is null")
    List<ReferralEntity> findWithPendingAlerts(@Param("centerId") String centerId);
}
