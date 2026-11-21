package com.redsaludandina.prescription.domain.model;

import com.redsaludandina.shared.domain.AggregateRoot;
import com.redsaludandina.shared.domain.BusinessRuleException;
import com.redsaludandina.shared.domain.ClinicalId;
import com.redsaludandina.shared.domain.Require;
import com.redsaludandina.shared.event.PrescriptionSigned;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Receta electrónica (raíz de agregado). Reglas del caso: una vez firmada es inmutable, se firma
 * SOLO en el servidor (nunca offline) y se envía a la farmacia del centro una sola vez.
 */
public final class Prescription extends AggregateRoot<ClinicalId> {

    public static final String SIGNED_IS_IMMUTABLE = "PRESCRIPTION_SIGNED_IMMUTABLE";
    public static final String WITHOUT_MEDICATIONS = "PRESCRIPTION_WITHOUT_MEDICATIONS";
    public static final String SIGNER_NOT_PRESCRIBER = "SIGNER_NOT_PRESCRIBER";
    public static final String NOT_SIGNED = "PRESCRIPTION_NOT_SIGNED";

    private final ClinicalId id;
    private final ClinicalId episodeId;
    private final String prescriberId;
    private final List<Medication> medications;
    private DigitalSignature signature;
    private Instant sentToPharmacyAt;

    private Prescription(ClinicalId id, ClinicalId episodeId, String prescriberId, Collection<Medication> medications,
                         DigitalSignature signature, Instant sentToPharmacyAt) {
        this.id = Require.notNull(id, "prescriptionId");
        this.episodeId = Require.notNull(episodeId, "episodeId");
        this.prescriberId = Require.text(prescriberId, "prescriberId");
        this.medications = new ArrayList<>(Require.notNull(medications, "medications"));
        this.signature = signature;
        this.sentToPharmacyAt = sentToPharmacyAt;
    }

    public static Prescription draft(ClinicalId id, ClinicalId episodeId, String prescriberId) {
        return new Prescription(id, episodeId, prescriberId, List.of(), null, null);
    }

    public static Prescription restore(ClinicalId id, ClinicalId episodeId, String prescriberId,
                                       Collection<Medication> medications, DigitalSignature signature,
                                       Instant sentToPharmacyAt) {
        return new Prescription(id, episodeId, prescriberId, medications, signature, sentToPharmacyAt);
    }

    public void addMedication(Medication medication) {
        Require.notNull(medication, "medication");
        if (signature != null) {
            throw new BusinessRuleException(SIGNED_IS_IMMUTABLE, "La receta " + id + " ya está firmada y no se puede modificar");
        }
        medications.add(medication);
    }

    /** Firmar otra vez con la MISMA firma es idempotente; con otra, se rechaza. */
    public void sign(DigitalSignature newSignature) {
        Require.notNull(newSignature, "signature");
        if (signature != null) {
            rejectUnlessSameSignature(newSignature);
            return;
        }
        requireSignable(newSignature);
        signature = newSignature;
        record(new PrescriptionSigned(id, episodeId, newSignature.signerId(), newSignature.signedAt()));
    }

    private void rejectUnlessSameSignature(DigitalSignature newSignature) {
        if (!signature.sameAs(newSignature)) {
            throw new BusinessRuleException(SIGNED_IS_IMMUTABLE, "La receta " + id + " ya está firmada y no admite otra firma");
        }
    }

    private void requireSignable(DigitalSignature newSignature) {
        if (medications.isEmpty()) {
            throw new BusinessRuleException(WITHOUT_MEDICATIONS, "La receta " + id + " no tiene medicamentos y no se puede firmar");
        }
        if (!newSignature.signerId().equals(prescriberId)) {
            throw new BusinessRuleException(SIGNER_NOT_PRESCRIBER, "Solo el prescriptor puede firmar la receta " + id);
        }
    }

    /** Marca el envío a la farmacia; solo la primera vez cuenta. */
    public void markSentToPharmacy(Instant now) {
        Require.notNull(now, "now");
        if (signature == null) {
            throw new BusinessRuleException(NOT_SIGNED, "La receta " + id + " no se puede enviar a la farmacia sin firma");
        }
        if (sentToPharmacyAt == null) {
            sentToPharmacyAt = now;
        }
    }

    public boolean wasSentToPharmacy() {
        return sentToPharmacyAt != null;
    }

    public PrescriptionStatus status() {
        return signature == null ? PrescriptionStatus.DRAFT : PrescriptionStatus.SIGNED;
    }

    @Override
    public ClinicalId id() {
        return id;
    }

    public ClinicalId episodeId() {
        return episodeId;
    }

    public String prescriberId() {
        return prescriberId;
    }

    public List<Medication> medications() {
        return List.copyOf(medications);
    }

    public Optional<DigitalSignature> signature() {
        return Optional.ofNullable(signature);
    }

    public Optional<Instant> sentToPharmacyAt() {
        return Optional.ofNullable(sentToPharmacyAt);
    }
}
