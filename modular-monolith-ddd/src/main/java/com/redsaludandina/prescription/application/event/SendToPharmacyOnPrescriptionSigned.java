package com.redsaludandina.prescription.application.event;

import com.redsaludandina.prescription.application.command.PrescriptionUpdater;
import com.redsaludandina.prescription.application.port.PharmacyGateway;
import com.redsaludandina.prescription.domain.model.Prescription;
import com.redsaludandina.shared.application.UnitOfWork;
import com.redsaludandina.shared.application.messaging.EventConsumer;
import com.redsaludandina.shared.application.messaging.InboxPort;
import com.redsaludandina.shared.domain.Require;
import com.redsaludandina.shared.event.PrescriptionSigned;
import java.time.Clock;
import java.util.UUID;

/**
 * Al firmarse una receta, la envía a la farmacia del centro UNA vez: el inbox descarta el evento
 * repetido y la marca de envío evita reenviar aunque llegue con otro id.
 */
public final class SendToPharmacyOnPrescriptionSigned implements EventConsumer<PrescriptionSigned> {

    public static final String NAME = "prescription.send-to-pharmacy";

    private final PrescriptionUpdater updater;
    private final PharmacyGateway pharmacy;
    private final InboxPort inbox;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public SendToPharmacyOnPrescriptionSigned(PrescriptionUpdater updater, PharmacyGateway pharmacy, InboxPort inbox,
                                              UnitOfWork unitOfWork, Clock clock) {
        this.updater = Require.notNull(updater, "updater");
        this.pharmacy = Require.notNull(pharmacy, "pharmacy");
        this.inbox = Require.notNull(inbox, "inbox");
        this.unitOfWork = Require.notNull(unitOfWork, "unitOfWork");
        this.clock = Require.notNull(clock, "clock");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Class<PrescriptionSigned> eventType() {
        return PrescriptionSigned.class;
    }

    @Override
    public void consume(UUID eventId, PrescriptionSigned event) {
        unitOfWork.run(() -> {
            if (inbox.registerIfNew(NAME, eventId)) {
                updater.updateInCurrentTransaction(event.prescriptionId(), this::sendOnce);
            }
        });
    }

    private void sendOnce(Prescription prescription) {
        if (!prescription.wasSentToPharmacy()) {
            pharmacy.send(prescription);
            prescription.markSentToPharmacy(clock.instant());
        }
    }
}
