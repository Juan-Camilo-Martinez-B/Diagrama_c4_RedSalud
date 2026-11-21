package com.redsaludandina.prescription.infrastructure.web;

import com.redsaludandina.prescription.application.command.AddMedicationCommand;
import com.redsaludandina.prescription.application.command.DraftPrescriptionCommand;
import com.redsaludandina.prescription.application.command.SignPrescriptionCommand;
import com.redsaludandina.shared.application.cqrs.CommandHandler;
import com.redsaludandina.shared.domain.ClinicalId;
import com.redsaludandina.shared.infrastructure.web.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Lado de escritura de Receta: el prescriptor y el firmante son el usuario autenticado. */
@RestController
@RequestMapping("/prescriptions")
public class PrescriptionCommandController {

    private final CommandHandler<DraftPrescriptionCommand> draftPrescription;
    private final CommandHandler<AddMedicationCommand> addMedication;
    private final CommandHandler<SignPrescriptionCommand> signPrescription;

    public PrescriptionCommandController(CommandHandler<DraftPrescriptionCommand> draftPrescription,
                                         CommandHandler<AddMedicationCommand> addMedication,
                                         CommandHandler<SignPrescriptionCommand> signPrescription) {
        this.draftPrescription = draftPrescription;
        this.addMedication = addMedication;
        this.signPrescription = signPrescription;
    }

    record DraftRequest(String prescriptionId, String episodeId) {
    }

    record MedicationRequest(String name, String dose, String instructions) {
    }

    record SignatureRequest(String signatureValue) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void draft(@RequestBody DraftRequest request, @AuthenticatedUser String prescriberId) {
        draftPrescription.handle(new DraftPrescriptionCommand(ClinicalId.of(request.prescriptionId()),
                ClinicalId.of(request.episodeId()), prescriberId));
    }

    @PostMapping("/{prescriptionId}/medications")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void addMedication(@PathVariable String prescriptionId, @RequestBody MedicationRequest request) {
        addMedication.handle(new AddMedicationCommand(ClinicalId.of(prescriptionId), request.name(), request.dose(),
                request.instructions()));
    }

    @PostMapping("/{prescriptionId}/signature")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void sign(@PathVariable String prescriptionId, @RequestBody SignatureRequest request,
              @AuthenticatedUser String signerId) {
        signPrescription.handle(new SignPrescriptionCommand(ClinicalId.of(prescriptionId), signerId,
                request.signatureValue()));
    }
}
