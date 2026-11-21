package com.redsaludandina.insights.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.repository.Repository;

/** Repositorio Spring Data del lado de LECTURA de los indicadores: proyección por interfaz. */
public interface UsageIndicatorQueryRepository extends Repository<UsageIndicatorEntity, String> {

    interface UsageIndicatorRow {
        String getCenterId();

        String getPeriod();

        long getTeleconsultations();

        long getBookings();

        long getTotalWaitSeconds();

        long getReferralsRequested();

        long getReferralsClosed();
    }

    Optional<UsageIndicatorRow> findByCenterIdAndPeriod(String centerId, String period);
}
