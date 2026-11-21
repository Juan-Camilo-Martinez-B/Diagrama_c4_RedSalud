package com.redsaludandina.prescription.infrastructure.config;

import com.redsaludandina.prescription.application.command.AddMedicationHandler;
import com.redsaludandina.prescription.application.command.DraftPrescriptionHandler;
import com.redsaludandina.prescription.application.command.PrescriptionUpdater;
import com.redsaludandina.prescription.application.command.SignPrescriptionHandler;
import com.redsaludandina.prescription.application.event.SendToPharmacyOnPrescriptionSigned;
import com.redsaludandina.prescription.application.port.PharmacyGateway;
import com.redsaludandina.prescription.application.query.GetPrescriptionStatusHandler;
import com.redsaludandina.prescription.infrastructure.persistence.JpaPrescriptionReadModel;
import com.redsaludandina.prescription.infrastructure.persistence.JpaPrescriptionRepository;
import com.redsaludandina.prescription.infrastructure.persistence.PrescriptionInboxRecord;
import com.redsaludandina.prescription.infrastructure.persistence.PrescriptionInboxRepository;
import com.redsaludandina.prescription.infrastructure.persistence.PrescriptionJpaRepository;
import com.redsaludandina.prescription.infrastructure.persistence.PrescriptionOutboxRecord;
import com.redsaludandina.prescription.infrastructure.persistence.PrescriptionOutboxRepository;
import com.redsaludandina.prescription.infrastructure.persistence.PrescriptionQueryRepository;
import com.redsaludandina.prescription.infrastructure.pharmacy.LocalPharmacyGateway;
import com.redsaludandina.prescription.infrastructure.pharmacy.PharmacyDispatchRepository;
import com.redsaludandina.prescription.infrastructure.pharmacy.SqsPharmacyGateway;
import com.redsaludandina.shared.application.UnitOfWork;
import com.redsaludandina.shared.application.messaging.EventBusPort;
import com.redsaludandina.shared.application.messaging.OutboxPort;
import com.redsaludandina.shared.application.messaging.OutboxRecorder;
import com.redsaludandina.shared.application.messaging.OutboxRelay;
import com.redsaludandina.shared.infrastructure.messaging.EventSerializer;
import com.redsaludandina.shared.infrastructure.persistence.inbox.JpaInbox;
import com.redsaludandina.shared.infrastructure.persistence.outbox.JpaOutbox;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsClient;
import tools.jackson.databind.json.JsonMapper;

/** Cableado del contexto Receta. */
@Configuration
public class PrescriptionConfiguration {

    static final String CONTEXT = "prescription";

    @Bean
    @ConditionalOnProperty(name = "redsalud.pharmacy.type", havingValue = "local", matchIfMissing = true)
    PharmacyGateway localPharmacyGateway(PharmacyDispatchRepository dispatches, Clock clock) {
        return new LocalPharmacyGateway(dispatches, clock);
    }

    @Bean
    @ConditionalOnProperty(name = "redsalud.pharmacy.type", havingValue = "sqs")
    PharmacyGateway sqsPharmacyGateway(SqsClient sqs, @Value("${redsalud.sqs.pharmacy-queue-url}") String queueUrl) {
        return new SqsPharmacyGateway(sqs, queueUrl, JsonMapper.builder().build());
    }

    @Bean
    PrescriptionUpdater prescriptionUpdater(PrescriptionJpaRepository prescriptions, PrescriptionOutboxRepository outbox,
                                            EventSerializer serializer, UnitOfWork unitOfWork, Clock clock) {
        return new PrescriptionUpdater(new JpaPrescriptionRepository(prescriptions), unitOfWork,
                new OutboxRecorder(CONTEXT, outbox(outbox, serializer), clock));
    }

    @Bean
    DraftPrescriptionHandler draftPrescriptionHandler(PrescriptionJpaRepository prescriptions, UnitOfWork unitOfWork) {
        return new DraftPrescriptionHandler(new JpaPrescriptionRepository(prescriptions), unitOfWork);
    }

    @Bean
    AddMedicationHandler addMedicationHandler(PrescriptionUpdater updater) {
        return new AddMedicationHandler(updater);
    }

    @Bean
    SignPrescriptionHandler signPrescriptionHandler(PrescriptionUpdater updater, Clock clock) {
        return new SignPrescriptionHandler(updater, clock);
    }

    @Bean
    SendToPharmacyOnPrescriptionSigned sendToPharmacyOnPrescriptionSigned(PrescriptionUpdater updater,
                                                                          PharmacyGateway pharmacy,
                                                                          PrescriptionInboxRepository inbox,
                                                                          UnitOfWork unitOfWork, Clock clock) {
        return new SendToPharmacyOnPrescriptionSigned(updater, pharmacy,
                new JpaInbox<>(inbox, PrescriptionInboxRecord::new, clock), unitOfWork, clock);
    }

    @Bean
    GetPrescriptionStatusHandler getPrescriptionStatusHandler(PrescriptionQueryRepository queries) {
        return new GetPrescriptionStatusHandler(new JpaPrescriptionReadModel(queries));
    }

    @Bean
    OutboxRelay prescriptionOutboxRelay(PrescriptionOutboxRepository outbox, EventSerializer serializer,
                                        EventBusPort bus, Clock clock) {
        return new OutboxRelay(outbox(outbox, serializer), bus, clock);
    }

    private static OutboxPort outbox(PrescriptionOutboxRepository repository, EventSerializer serializer) {
        return new JpaOutbox<>(repository, PrescriptionOutboxRecord::new, serializer);
    }
}
