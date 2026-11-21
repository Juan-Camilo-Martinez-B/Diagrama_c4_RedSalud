package com.redsaludandina.prescription.infrastructure.pharmacy;

import com.redsaludandina.prescription.application.port.PharmacyGateway;
import com.redsaludandina.prescription.domain.model.Prescription;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Farmacia simulada (perfil local): deja constancia del envío en una tabla. */
public final class LocalPharmacyGateway implements PharmacyGateway {

    private static final Logger LOG = LoggerFactory.getLogger(LocalPharmacyGateway.class);

    private final PharmacyDispatchRepository dispatches;
    private final Clock clock;

    public LocalPharmacyGateway(PharmacyDispatchRepository dispatches, Clock clock) {
        this.dispatches = dispatches;
        this.clock = clock;
    }

    @Override
    public void send(Prescription prescription) {
        String id = prescription.id().value();
        if (!dispatches.existsById(id)) {
            dispatches.save(new PharmacyDispatchEntity(id, clock.instant()));
            LOG.info("Receta {} enviada a la farmacia (registro local)", id);
        }
    }
}
