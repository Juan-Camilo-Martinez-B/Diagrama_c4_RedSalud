package com.redsaludandina.prescription.application.command;

import com.redsaludandina.prescription.application.port.PrescriptionRepository;
import com.redsaludandina.prescription.domain.model.Prescription;
import com.redsaludandina.shared.application.ResourceNotFoundException;
import com.redsaludandina.shared.application.UnitOfWork;
import com.redsaludandina.shared.application.messaging.OutboxRecorder;
import com.redsaludandina.shared.domain.ClinicalId;
import com.redsaludandina.shared.domain.Require;
import java.util.function.Consumer;

/** Plantilla de los comandos que modifican una receta: cargar, cambiar, guardar y outbox en UNA transacción. */
public final class PrescriptionUpdater {

    private final PrescriptionRepository prescriptions;
    private final UnitOfWork unitOfWork;
    private final OutboxRecorder outbox;

    public PrescriptionUpdater(PrescriptionRepository prescriptions, UnitOfWork unitOfWork, OutboxRecorder outbox) {
        this.prescriptions = Require.notNull(prescriptions, "prescriptions");
        this.unitOfWork = Require.notNull(unitOfWork, "unitOfWork");
        this.outbox = Require.notNull(outbox, "outbox");
    }

    public void update(ClinicalId prescriptionId, Consumer<Prescription> change) {
        unitOfWork.run(() -> updateInCurrentTransaction(prescriptionId, change));
    }

    /** Para quien ya abrió la transacción (por ejemplo, un consumidor que registra su inbox en ella). */
    public void updateInCurrentTransaction(ClinicalId prescriptionId, Consumer<Prescription> change) {
        Prescription prescription = prescriptions.findById(Require.notNull(prescriptionId, "prescriptionId"))
                .orElseThrow(() -> new ResourceNotFoundException("la receta " + prescriptionId));
        change.accept(prescription);
        prescriptions.save(prescription);
        outbox.record(prescription);
    }
}
