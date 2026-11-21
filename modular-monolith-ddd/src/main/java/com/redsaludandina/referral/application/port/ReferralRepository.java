package com.redsaludandina.referral.application.port;

import com.redsaludandina.referral.domain.model.Referral;
import com.redsaludandina.shared.domain.ClinicalId;
import java.util.Optional;

/** Lado de escritura: carga y guarda el agregado {@link Referral} (puerto de salida). */
public interface ReferralRepository {

    boolean exists(ClinicalId referralId);

    Optional<Referral> findById(ClinicalId referralId);

    Optional<Referral> findBySlot(ClinicalId slotId);

    /** La interconsulta SOLICITADA más antigua del paciente: la que un cupo nuevo debe agendar. */
    Optional<Referral> oldestRequestedFor(String patientId);

    void save(Referral referral);
}
