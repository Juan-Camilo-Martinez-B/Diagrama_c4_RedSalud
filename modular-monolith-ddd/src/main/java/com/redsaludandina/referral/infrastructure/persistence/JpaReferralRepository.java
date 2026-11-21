package com.redsaludandina.referral.infrastructure.persistence;

import com.redsaludandina.referral.application.port.ReferralRepository;
import com.redsaludandina.referral.domain.model.Referral;
import com.redsaludandina.referral.domain.model.ReferralStatus;
import com.redsaludandina.shared.domain.ClinicalId;
import java.util.Optional;

/** Adaptador del lado de escritura de Interconsulta sobre Spring Data JPA. */
public final class JpaReferralRepository implements ReferralRepository {

    private final ReferralJpaRepository jpa;

    public JpaReferralRepository(ReferralJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean exists(ClinicalId referralId) {
        return jpa.existsById(referralId.value());
    }

    @Override
    public Optional<Referral> findById(ClinicalId referralId) {
        return jpa.findById(referralId.value()).map(ReferralEntity::toDomain);
    }

    @Override
    public Optional<Referral> findBySlot(ClinicalId slotId) {
        return jpa.findFirstBySlotId(slotId.value()).map(ReferralEntity::toDomain);
    }

    @Override
    public Optional<Referral> oldestRequestedFor(String patientId) {
        return jpa.findFirstByPatientIdAndStatusOrderByRequestedAtAsc(patientId, ReferralStatus.REQUESTED.name())
                .map(ReferralEntity::toDomain);
    }

    @Override
    public void save(Referral referral) {
        String id = referral.id().value();
        ReferralEntity entity = jpa.findById(id).orElseGet(() -> new ReferralEntity(id));
        entity.copyFrom(referral);
        jpa.save(entity);
    }
}
