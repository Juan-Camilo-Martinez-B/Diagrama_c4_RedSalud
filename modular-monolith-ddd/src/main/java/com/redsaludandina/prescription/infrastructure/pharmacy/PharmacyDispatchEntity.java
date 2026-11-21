package com.redsaludandina.prescription.infrastructure.pharmacy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Fila {@code prescription.pharmacy_dispatch}: la farmacia simulada del perfil local. */
@Entity
@Table(schema = "prescription", name = "pharmacy_dispatch")
public class PharmacyDispatchEntity {

    @Id
    @Column(name = "prescription_id")
    private String prescriptionId;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    protected PharmacyDispatchEntity() {
    }

    PharmacyDispatchEntity(String prescriptionId, Instant sentAt) {
        this.prescriptionId = prescriptionId;
        this.sentAt = sentAt;
    }
}
