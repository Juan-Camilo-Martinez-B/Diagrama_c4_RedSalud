package com.redsaludandina.referral.domain.model;

import com.redsaludandina.shared.domain.AggregateRoot;
import com.redsaludandina.shared.domain.BusinessRuleException;
import com.redsaludandina.shared.domain.ClinicalId;
import com.redsaludandina.shared.domain.Require;
import com.redsaludandina.shared.event.ReferralClosed;
import com.redsaludandina.shared.event.ReferralRequested;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Interconsulta de un médico rural a una especialidad (raíz de agregado). Avanza con los hechos de
 * otros contextos: un cupo reservado la agenda y una atención terminada la marca atendida. Repetir
 * el mismo hecho no la hace avanzar dos veces.
 */
public final class Referral extends AggregateRoot<ClinicalId> {

    public static final String INVALID_TRANSITION = "REFERRAL_INVALID_TRANSITION";

    private final ClinicalId id;
    private final ClinicalId originEpisodeId;
    private final String patientId;
    private final String centerId;
    private final String specialty;
    private final String reason;
    private final Instant requestedAt;
    private ReferralStatus status;
    private ClinicalId slotId;
    private ClinicalId attentionEpisodeId;
    private final List<Alert> alerts;

    private Referral(ReferralData data, ReferralStatus status, ClinicalId slotId, ClinicalId attentionEpisodeId,
                     Collection<Alert> alerts) {
        this.id = Require.notNull(data.id(), "referralId");
        this.originEpisodeId = Require.notNull(data.originEpisodeId(), "originEpisodeId");
        this.patientId = Require.text(data.patientId(), "patientId");
        this.centerId = Require.text(data.centerId(), "centerId");
        this.specialty = Require.text(data.specialty(), "specialty");
        this.reason = Require.text(data.reason(), "reason");
        this.requestedAt = Require.notNull(data.requestedAt(), "requestedAt");
        this.status = Require.notNull(status, "status");
        this.slotId = slotId;
        this.attentionEpisodeId = attentionEpisodeId;
        this.alerts = new ArrayList<>(Require.notNull(alerts, "alerts"));
    }

    /** Datos con los que se solicita una interconsulta; no cambian durante su vida. */
    public record ReferralData(ClinicalId id, ClinicalId originEpisodeId, String patientId, String centerId,
                               String specialty, String reason, Instant requestedAt) {
    }

    public static Referral request(ReferralData data) {
        Referral referral = new Referral(data, ReferralStatus.REQUESTED, null, null, List.of());
        referral.openPendingAlert(data.requestedAt());
        referral.record(new ReferralRequested(referral.id, referral.centerId, referral.specialty, data.requestedAt()));
        return referral;
    }

    public static Referral restore(ReferralData data, ReferralStatus status, ClinicalId slotId,
                                   ClinicalId attentionEpisodeId, Collection<Alert> alerts) {
        return new Referral(data, status, slotId, attentionEpisodeId, alerts);
    }

    public void schedule(ClinicalId bookedSlotId, Instant now) {
        Require.notNull(bookedSlotId, "slotId");
        if (status == ReferralStatus.SCHEDULED && bookedSlotId.equals(slotId)) {
            return;
        }
        advanceTo(ReferralStatus.SCHEDULED, now);
        slotId = bookedSlotId;
    }

    public void recordAttention(ClinicalId attendedEpisodeId, Instant now) {
        Require.notNull(attendedEpisodeId, "episodeId");
        if (status == ReferralStatus.ATTENDED && attendedEpisodeId.equals(attentionEpisodeId)) {
            return;
        }
        advanceTo(ReferralStatus.ATTENDED, now);
        attentionEpisodeId = attendedEpisodeId;
    }

    public void close(Instant now) {
        if (status == ReferralStatus.CLOSED) {
            return;
        }
        advanceTo(ReferralStatus.CLOSED, now);
        record(new ReferralClosed(id, centerId, now));
    }

    private void advanceTo(ReferralStatus target, Instant now) {
        Require.notNull(now, "now");
        if (!status.canAdvanceTo(target)) {
            throw new BusinessRuleException(INVALID_TRANSITION,
                    "La interconsulta " + id + " no puede pasar de " + status + " a " + target);
        }
        alerts.stream().filter(Alert::isPending).forEach(alert -> alert.resolve(now));
        status = target;
        openPendingAlert(now);
    }

    private void openPendingAlert(Instant now) {
        status.pendingAlert().ifPresent(type -> alerts.add(Alert.open(type, now)));
    }

    public List<Alert> pendingAlerts() {
        return alerts.stream().filter(Alert::isPending).toList();
    }

    @Override
    public ClinicalId id() {
        return id;
    }

    public ReferralData data() {
        return new ReferralData(id, originEpisodeId, patientId, centerId, specialty, reason, requestedAt);
    }

    public ReferralStatus status() {
        return status;
    }

    public Optional<ClinicalId> slotId() {
        return Optional.ofNullable(slotId);
    }

    public Optional<ClinicalId> attentionEpisodeId() {
        return Optional.ofNullable(attentionEpisodeId);
    }

    public List<Alert> alerts() {
        return List.copyOf(alerts);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Referral referral && id.equals(referral.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
