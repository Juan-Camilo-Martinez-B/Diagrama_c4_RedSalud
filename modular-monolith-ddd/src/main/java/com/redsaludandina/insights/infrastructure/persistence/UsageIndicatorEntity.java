package com.redsaludandina.insights.infrastructure.persistence;

import com.redsaludandina.insights.domain.analytics.UsageIndicator;
import com.redsaludandina.insights.domain.analytics.UsageIndicator.Counters;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.YearMonth;

/** Fila {@code insights.usage_indicator}: un centro en un mes. */
@Entity
@Table(schema = "insights", name = "usage_indicator")
public class UsageIndicatorEntity {

    @Id
    private String id;

    @Column(name = "center_id", nullable = false)
    private String centerId;

    @Column(nullable = false)
    private String period;

    @Column(nullable = false)
    private long teleconsultations;

    @Column(nullable = false)
    private long bookings;

    @Column(name = "total_wait_seconds", nullable = false)
    private long totalWaitSeconds;

    @Column(name = "referrals_requested", nullable = false)
    private long referralsRequested;

    @Column(name = "referrals_closed", nullable = false)
    private long referralsClosed;

    protected UsageIndicatorEntity() {
    }

    static String idOf(String centerId, String period) {
        return centerId + "|" + period;
    }

    static UsageIndicatorEntity fromDomain(UsageIndicator indicator) {
        UsageIndicatorEntity entity = new UsageIndicatorEntity();
        Counters counters = indicator.counters();
        entity.centerId = indicator.centerId();
        entity.period = indicator.period().toString();
        entity.id = idOf(entity.centerId, entity.period);
        entity.teleconsultations = counters.teleconsultations();
        entity.bookings = counters.bookings();
        entity.totalWaitSeconds = counters.totalWait().toSeconds();
        entity.referralsRequested = counters.referralsRequested();
        entity.referralsClosed = counters.referralsClosed();
        return entity;
    }

    UsageIndicator toDomain() {
        return UsageIndicator.restore(centerId, YearMonth.parse(period), new Counters(teleconsultations, bookings,
                Duration.ofSeconds(totalWaitSeconds), referralsRequested, referralsClosed));
    }
}
