package com.redsaludandina.insights.domain.analytics;

import com.redsaludandina.shared.domain.Require;
import com.redsaludandina.shared.event.PatientAttended;
import com.redsaludandina.shared.event.ReferralClosed;
import com.redsaludandina.shared.event.ReferralRequested;
import com.redsaludandina.shared.event.SlotBooked;
import java.time.Duration;
import java.time.YearMonth;
import java.util.Optional;

/**
 * Indicadores de uso de un centro en un mes (analítica fuera del modelo transaccional y sin datos
 * del paciente: solo conteos por centro). Se proyecta desde los eventos del lenguaje publicado.
 */
public final class UsageIndicator {

    private final String centerId;
    private final YearMonth period;
    private long teleconsultations;
    private long bookings;
    private Duration totalWait;
    private long referralsRequested;
    private long referralsClosed;

    private UsageIndicator(String centerId, YearMonth period, Counters counters) {
        this.centerId = Require.text(centerId, "centerId");
        this.period = Require.notNull(period, "period");
        this.teleconsultations = counters.teleconsultations();
        this.bookings = counters.bookings();
        this.totalWait = Require.notNull(counters.totalWait(), "totalWait");
        this.referralsRequested = counters.referralsRequested();
        this.referralsClosed = counters.referralsClosed();
    }

    /** Valores acumulados de un indicador. */
    public record Counters(long teleconsultations, long bookings, Duration totalWait, long referralsRequested,
                           long referralsClosed) {

        public static final Counters ZERO = new Counters(0, 0, Duration.ZERO, 0, 0);
    }

    public static UsageIndicator start(String centerId, YearMonth period) {
        return new UsageIndicator(centerId, period, Counters.ZERO);
    }

    public static UsageIndicator restore(String centerId, YearMonth period, Counters counters) {
        return new UsageIndicator(centerId, period, counters);
    }

    public void onSlotBooked(SlotBooked event) {
        requireCenter(event.centerId());
        bookings++;
        Duration wait = Duration.between(event.occurredAt(), event.slotStart());
        totalWait = totalWait.plus(wait.isNegative() ? Duration.ZERO : wait);
    }

    public void onPatientAttended(PatientAttended event) {
        requireCenter(event.centerId());
        teleconsultations++;
    }

    public void onReferralRequested(ReferralRequested event) {
        requireCenter(event.centerId());
        referralsRequested++;
    }

    public void onReferralClosed(ReferralClosed event) {
        requireCenter(event.centerId());
        referralsClosed++;
    }

    private void requireCenter(String eventCenterId) {
        if (!centerId.equals(eventCenterId)) {
            throw new IllegalArgumentException("El evento es del centro " + eventCenterId + " y el indicador del centro " + centerId);
        }
    }

    public Optional<Duration> averageWait() {
        return bookings == 0 ? Optional.empty() : Optional.of(totalWait.dividedBy(bookings));
    }

    public Optional<Double> closureRate() {
        return referralsRequested == 0 ? Optional.empty() : Optional.of((double) referralsClosed / referralsRequested);
    }

    public String centerId() {
        return centerId;
    }

    public YearMonth period() {
        return period;
    }

    public Counters counters() {
        return new Counters(teleconsultations, bookings, totalWait, referralsRequested, referralsClosed);
    }
}
