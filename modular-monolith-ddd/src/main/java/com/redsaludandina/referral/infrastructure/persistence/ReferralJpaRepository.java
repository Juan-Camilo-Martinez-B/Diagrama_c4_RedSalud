package com.redsaludandina.referral.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.repository.Repository;

/** Repositorio Spring Data del lado de escritura de Interconsulta. */
public interface ReferralJpaRepository extends Repository<ReferralEntity, String> {

    boolean existsById(String id);

    Optional<ReferralEntity> findById(String id);

    Optional<ReferralEntity> findFirstBySlotId(String slotId);

    Optional<ReferralEntity> findFirstByPatientIdAndStatusOrderByRequestedAtAsc(String patientId, String status);

    ReferralEntity save(ReferralEntity referral);
}
