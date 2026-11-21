package com.redsaludandina.prescription.infrastructure.persistence;

import com.redsaludandina.prescription.application.query.PrescriptionView;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Repositorio Spring Data del lado de LECTURA de Receta: proyecta directo al DTO. */
public interface PrescriptionQueryRepository extends Repository<PrescriptionEntity, String> {

    @Query("select new com.redsaludandina.prescription.application.query.PrescriptionView("
            + "p.id, p.episodeId, case when p.signedAt is null then 'DRAFT' else 'SIGNED' end,"
            + " size(p.medications), p.signedAt, p.sentToPharmacyAt)"
            + " from PrescriptionEntity p where p.id = :id")
    Optional<PrescriptionView> findView(@Param("id") String id);
}
