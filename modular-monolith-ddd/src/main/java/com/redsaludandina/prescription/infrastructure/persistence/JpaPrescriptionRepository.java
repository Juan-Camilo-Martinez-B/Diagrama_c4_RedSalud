package com.redsaludandina.prescription.infrastructure.persistence;

import com.redsaludandina.prescription.application.port.PrescriptionRepository;
import com.redsaludandina.prescription.domain.model.Prescription;
import com.redsaludandina.shared.domain.ClinicalId;
import java.util.Optional;

/** Adaptador del lado de escritura de Receta sobre Spring Data JPA. */
public final class JpaPrescriptionRepository implements PrescriptionRepository {

    private final PrescriptionJpaRepository jpa;

    public JpaPrescriptionRepository(PrescriptionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean exists(ClinicalId prescriptionId) {
        return jpa.existsById(prescriptionId.value());
    }

    @Override
    public Optional<Prescription> findById(ClinicalId prescriptionId) {
        return jpa.findById(prescriptionId.value()).map(PrescriptionEntity::toDomain);
    }

    @Override
    public void save(Prescription prescription) {
        String id = prescription.id().value();
        PrescriptionEntity entity = jpa.findById(id).orElseGet(() -> new PrescriptionEntity(id));
        entity.copyFrom(prescription);
        jpa.save(entity);
    }
}
