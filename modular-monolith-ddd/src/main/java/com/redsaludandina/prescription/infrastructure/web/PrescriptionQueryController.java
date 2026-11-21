package com.redsaludandina.prescription.infrastructure.web;

import com.redsaludandina.prescription.application.query.GetPrescriptionStatusQuery;
import com.redsaludandina.prescription.application.query.PrescriptionView;
import com.redsaludandina.shared.application.cqrs.QueryHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Lado de lectura de Receta: solo el estado (sin detalle clínico), sin efectos. */
@RestController
public class PrescriptionQueryController {

    private final QueryHandler<GetPrescriptionStatusQuery, PrescriptionView> getStatus;

    public PrescriptionQueryController(QueryHandler<GetPrescriptionStatusQuery, PrescriptionView> getStatus) {
        this.getStatus = getStatus;
    }

    @GetMapping("/prescriptions/{prescriptionId}/status")
    PrescriptionView status(@PathVariable String prescriptionId) {
        return getStatus.handle(new GetPrescriptionStatusQuery(prescriptionId));
    }
}
