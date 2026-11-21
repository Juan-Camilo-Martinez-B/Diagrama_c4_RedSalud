package com.redsaludandina.prescription.application.command;

import com.redsaludandina.prescription.domain.model.DigitalSignature;
import com.redsaludandina.shared.application.cqrs.CommandHandler;
import com.redsaludandina.shared.domain.Require;
import java.time.Clock;

/** Firma y deja {@code PrescriptionSigned} en el outbox, en la misma transacción. */
public final class SignPrescriptionHandler implements CommandHandler<SignPrescriptionCommand> {

    private final PrescriptionUpdater updater;
    private final Clock clock;

    public SignPrescriptionHandler(PrescriptionUpdater updater, Clock clock) {
        this.updater = Require.notNull(updater, "updater");
        this.clock = Require.notNull(clock, "clock");
    }

    @Override
    public void handle(SignPrescriptionCommand command) {
        DigitalSignature signature = new DigitalSignature(command.signerId(), clock.instant(), command.signatureValue());
        updater.update(command.prescriptionId(), prescription -> prescription.sign(signature));
    }
}
