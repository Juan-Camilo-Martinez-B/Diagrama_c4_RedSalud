package com.redsaludandina.prescription.infrastructure.persistence;

import com.redsaludandina.prescription.domain.model.DigitalSignature;
import com.redsaludandina.prescription.domain.model.Prescription;
import com.redsaludandina.shared.domain.ClinicalId;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Fila JPA de la receta con sus medicamentos en orden. */
@Entity
@Table(schema = "prescription", name = "prescription")
public class PrescriptionEntity {

    @Id
    private String id;

    @Column(name = "episode_id", nullable = false)
    private String episodeId;

    @Column(name = "prescriber_id", nullable = false)
    private String prescriberId;

    @Column(name = "signer_id")
    private String signerId;

    @Column(name = "signed_at")
    private Instant signedAt;

    @Column(name = "signature_value")
    private String signatureValue;

    @Column(name = "sent_to_pharmacy_at")
    private Instant sentToPharmacyAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(schema = "prescription", name = "medication", joinColumns = @JoinColumn(name = "prescription_id"))
    @OrderColumn(name = "position")
    private List<MedicationEmbeddable> medications = new ArrayList<>();

    protected PrescriptionEntity() {
    }

    PrescriptionEntity(String id) {
        this.id = id;
    }

    void copyFrom(Prescription prescription) {
        episodeId = prescription.episodeId().value();
        prescriberId = prescription.prescriberId();
        prescription.signature().ifPresent(this::copySignature);
        sentToPharmacyAt = prescription.sentToPharmacyAt().orElse(null);
        medications.clear();
        prescription.medications().forEach(medication -> medications.add(new MedicationEmbeddable(medication)));
    }

    private void copySignature(DigitalSignature signature) {
        signerId = signature.signerId();
        signedAt = signature.signedAt();
        signatureValue = signature.value();
    }

    Prescription toDomain() {
        DigitalSignature signature = signerId == null ? null : new DigitalSignature(signerId, signedAt, signatureValue);
        return Prescription.restore(ClinicalId.of(id), ClinicalId.of(episodeId), prescriberId,
                medications.stream().map(MedicationEmbeddable::toDomain).toList(), signature, sentToPharmacyAt);
    }
}
