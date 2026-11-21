package com.redsaludandina.prescription.domain.model;

import static com.redsaludandina.TestIds.id;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.redsaludandina.shared.domain.BusinessRuleException;
import com.redsaludandina.shared.event.PrescriptionSigned;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PrescriptionTest {

    private static final Instant NOW = Instant.parse("2026-03-01T12:00:00Z");
    private static final Medication PARACETAMOL = new Medication("Paracetamol", "500 mg", "Cada 8 horas");

    private static Prescription draftWithMedication() {
        Prescription prescription = Prescription.draft(id(1), id(2), "med-1");
        prescription.addMedication(PARACETAMOL);
        return prescription;
    }

    private static String codeOf(Runnable action) {
        try {
            action.run();
            return "";
        } catch (BusinessRuleException error) {
            return error.code();
        }
    }

    @Test
    void signingPublishesPrescriptionSignedAndFreezesThePrescription() {
        Prescription prescription = draftWithMedication();

        prescription.sign(new DigitalSignature("med-1", NOW, "firma"));

        assertThat(prescription.status()).isEqualTo(PrescriptionStatus.SIGNED);
        assertThat(prescription.pullEvents()).singleElement().isInstanceOf(PrescriptionSigned.class);
        assertThat(codeOf(() -> prescription.addMedication(PARACETAMOL))).isEqualTo(Prescription.SIGNED_IS_IMMUTABLE);
    }

    @Test
    void signingTwiceWithTheSameSignatureIsIdempotent() {
        Prescription prescription = draftWithMedication();
        prescription.sign(new DigitalSignature("med-1", NOW, "firma"));
        prescription.pullEvents();

        prescription.sign(new DigitalSignature("med-1", NOW.plusSeconds(5), "firma"));

        assertThat(prescription.pullEvents()).isEmpty();
    }

    @Test
    void signingRules() {
        Prescription empty = Prescription.draft(id(1), id(2), "med-1");
        assertThat(codeOf(() -> empty.sign(new DigitalSignature("med-1", NOW, "f"))))
                .isEqualTo(Prescription.WITHOUT_MEDICATIONS);
        assertThat(codeOf(() -> draftWithMedication().sign(new DigitalSignature("esp-1", NOW, "f"))))
                .isEqualTo(Prescription.SIGNER_NOT_PRESCRIBER);
    }

    @Test
    void onlyASignedPrescriptionGoesToThePharmacyAndOnlyOnce() {
        Prescription prescription = draftWithMedication();
        assertThatThrownBy(() -> prescription.markSentToPharmacy(NOW)).isInstanceOf(BusinessRuleException.class);

        prescription.sign(new DigitalSignature("med-1", NOW, "firma"));
        prescription.markSentToPharmacy(NOW);
        prescription.markSentToPharmacy(NOW.plusSeconds(60));

        assertThat(prescription.sentToPharmacyAt()).contains(NOW);
    }
}
