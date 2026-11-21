package com.redsaludandina.referral.infrastructure.persistence;

import com.redsaludandina.referral.application.query.PendingReferralView;
import com.redsaludandina.referral.domain.model.Referral;
import com.redsaludandina.referral.domain.model.Referral.ReferralData;
import com.redsaludandina.referral.domain.model.ReferralStatus;
import com.redsaludandina.shared.domain.ClinicalId;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Fila JPA de la interconsulta con sus alertas en orden. */
@Entity
@Table(schema = "referral", name = "referral")
public class ReferralEntity {

    @Id
    private String id;

    @Column(name = "origin_episode_id", nullable = false)
    private String originEpisodeId;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "center_id", nullable = false)
    private String centerId;

    @Column(nullable = false)
    private String specialty;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private String status;

    @Column(name = "slot_id")
    private String slotId;

    @Column(name = "attention_episode_id")
    private String attentionEpisodeId;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(schema = "referral", name = "alert", joinColumns = @JoinColumn(name = "referral_id"))
    @OrderColumn(name = "position")
    private List<AlertEmbeddable> alerts = new ArrayList<>();

    protected ReferralEntity() {
    }

    ReferralEntity(String id) {
        this.id = id;
    }

    void copyFrom(Referral referral) {
        ReferralData data = referral.data();
        originEpisodeId = data.originEpisodeId().value();
        patientId = data.patientId();
        centerId = data.centerId();
        specialty = data.specialty();
        reason = data.reason();
        requestedAt = data.requestedAt();
        status = referral.status().name();
        slotId = referral.slotId().map(ClinicalId::value).orElse(null);
        attentionEpisodeId = referral.attentionEpisodeId().map(ClinicalId::value).orElse(null);
        alerts.clear();
        referral.alerts().forEach(alert -> alerts.add(new AlertEmbeddable(alert)));
    }

    Referral toDomain() {
        ReferralData data = new ReferralData(ClinicalId.of(id), ClinicalId.of(originEpisodeId), patientId, centerId,
                specialty, reason, requestedAt);
        return Referral.restore(data, ReferralStatus.valueOf(status), ClinicalId.ofNullable(slotId),
                ClinicalId.ofNullable(attentionEpisodeId), alerts.stream().map(AlertEmbeddable::toDomain).toList());
    }

    PendingReferralView toPendingView() {
        return new PendingReferralView(id, specialty, status,
                alerts.stream().filter(AlertEmbeddable::isPending).map(AlertEmbeddable::type).toList());
    }
}
