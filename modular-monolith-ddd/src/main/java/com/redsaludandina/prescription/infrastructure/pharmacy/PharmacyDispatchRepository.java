package com.redsaludandina.prescription.infrastructure.pharmacy;

import org.springframework.data.repository.Repository;

public interface PharmacyDispatchRepository extends Repository<PharmacyDispatchEntity, String> {

    boolean existsById(String prescriptionId);

    PharmacyDispatchEntity save(PharmacyDispatchEntity dispatch);
}
