package com.redsaludandina.prescription.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.repository.Repository;

/** Repositorio Spring Data del lado de escritura de Receta. */
public interface PrescriptionJpaRepository extends Repository<PrescriptionEntity, String> {

    boolean existsById(String id);

    Optional<PrescriptionEntity> findById(String id);

    PrescriptionEntity save(PrescriptionEntity prescription);
}
