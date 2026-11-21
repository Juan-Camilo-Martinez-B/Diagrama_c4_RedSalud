package com.redsaludandina.prescription.application.port;

import com.redsaludandina.prescription.domain.model.Prescription;

/** Farmacia del centro (puerto de salida). La entrega es eventual: una cola en AWS, un registro en local. */
public interface PharmacyGateway {

    void send(Prescription prescription);
}
