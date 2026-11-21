package com.redsaludandina.insights.infrastructure.persistence;

import com.redsaludandina.insights.application.port.UsageIndicatorReadModel;
import com.redsaludandina.insights.application.query.UsageIndicatorView;
import com.redsaludandina.insights.infrastructure.persistence.UsageIndicatorQueryRepository.UsageIndicatorRow;
import java.util.Optional;

/** Proyecta la fila al DTO y calcula los promedios de lectura (sin pasar por el dominio). */
public final class JpaUsageIndicatorReadModel implements UsageIndicatorReadModel {

    private static final long SECONDS_PER_MINUTE = 60;

    private final UsageIndicatorQueryRepository queries;

    public JpaUsageIndicatorReadModel(UsageIndicatorQueryRepository queries) {
        this.queries = queries;
    }

    @Override
    public Optional<UsageIndicatorView> find(String centerId, String period) {
        return queries.findByCenterIdAndPeriod(centerId, period).map(JpaUsageIndicatorReadModel::toView);
    }

    private static UsageIndicatorView toView(UsageIndicatorRow row) {
        Long averageWait = row.getBookings() == 0 ? null : row.getTotalWaitSeconds() / row.getBookings() / SECONDS_PER_MINUTE;
        Double closureRate = row.getReferralsRequested() == 0 ? null
                : (double) row.getReferralsClosed() / row.getReferralsRequested();
        return new UsageIndicatorView(row.getCenterId(), row.getPeriod(), row.getTeleconsultations(), row.getBookings(),
                averageWait, row.getReferralsRequested(), row.getReferralsClosed(), closureRate);
    }
}
