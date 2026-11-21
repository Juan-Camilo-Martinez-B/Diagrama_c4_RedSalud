package com.redsaludandina.prescription.infrastructure.pharmacy;

import com.redsaludandina.prescription.application.port.PharmacyGateway;
import com.redsaludandina.prescription.domain.model.DigitalSignature;
import com.redsaludandina.prescription.domain.model.Prescription;
import java.time.Instant;
import java.util.List;
import software.amazon.awssdk.services.sqs.SqsClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Envía la receta firmada a la cola SQS FIFO de la farmacia del centro. La deduplicación por id de
 * receta hace que un reenvío no genere un segundo despacho.
 */
public final class SqsPharmacyGateway implements PharmacyGateway {

    record PharmacyMessage(String prescriptionId, String episodeId, String prescriberId, String signerId,
                           Instant signedAt, List<Line> medications) {
    }

    record Line(String name, String dose, String instructions) {
    }

    private final SqsClient sqs;
    private final String queueUrl;
    private final JsonMapper json;

    public SqsPharmacyGateway(SqsClient sqs, String queueUrl, JsonMapper json) {
        if (queueUrl == null || !queueUrl.endsWith(".fifo")) {
            throw new IllegalArgumentException("La cola de farmacia debe ser SQS FIFO (.fifo): " + queueUrl);
        }
        this.sqs = sqs;
        this.queueUrl = queueUrl;
        this.json = json;
    }

    @Override
    public void send(Prescription prescription) {
        DigitalSignature signature = prescription.signature()
                .orElseThrow(() -> new IllegalStateException("Solo se envían recetas firmadas: " + prescription.id()));
        PharmacyMessage message = new PharmacyMessage(prescription.id().value(), prescription.episodeId().value(),
                prescription.prescriberId(), signature.signerId(), signature.signedAt(),
                prescription.medications().stream()
                        .map(medication -> new Line(medication.name(), medication.dose(), medication.instructions()))
                        .toList());
        sqs.sendMessage(send -> send
                .queueUrl(queueUrl)
                .messageGroupId(prescription.id().value())
                .messageDeduplicationId(prescription.id().value())
                .messageBody(json.writeValueAsString(message)));
    }
}
