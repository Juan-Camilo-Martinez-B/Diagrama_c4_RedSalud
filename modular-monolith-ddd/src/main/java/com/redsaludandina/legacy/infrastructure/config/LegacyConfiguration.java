package com.redsaludandina.legacy.infrastructure.config;

import com.redsaludandina.identity.application.command.AuthorizeAccessCommand;
import com.redsaludandina.legacy.application.command.RecordHistoryReadHandler;
import com.redsaludandina.legacy.application.command.RefreshClinicalHistoryHandler;
import com.redsaludandina.legacy.application.port.HistoryReadModelReader;
import com.redsaludandina.legacy.application.port.HistoryReadModelWriter;
import com.redsaludandina.legacy.application.port.LegacyHistorySource;
import com.redsaludandina.legacy.application.query.GetClinicalHistoryHandler;
import com.redsaludandina.legacy.infrastructure.persistence.LegacyOutboxRecord;
import com.redsaludandina.legacy.infrastructure.persistence.LegacyOutboxRepository;
import com.redsaludandina.shared.application.UnitOfWork;
import com.redsaludandina.shared.application.cqrs.CommandHandler;
import com.redsaludandina.shared.application.messaging.EventBusPort;
import com.redsaludandina.shared.application.messaging.OutboxPort;
import com.redsaludandina.shared.application.messaging.OutboxRecorder;
import com.redsaludandina.shared.application.messaging.OutboxRelay;
import com.redsaludandina.shared.infrastructure.messaging.EventSerializer;
import com.redsaludandina.shared.infrastructure.persistence.outbox.JpaOutbox;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cableado de los casos de uso de Integración con el legado; recibe solo PUERTOS de la ACL. */
@Configuration
public class LegacyConfiguration {

    static final String CONTEXT = "legacy";

    @Bean
    RecordHistoryReadHandler recordHistoryReadHandler(CommandHandler<AuthorizeAccessCommand> authorizeAccess,
                                                      LegacyOutboxRepository outbox, EventSerializer serializer,
                                                      UnitOfWork unitOfWork, Clock clock) {
        return new RecordHistoryReadHandler(authorizeAccess, unitOfWork,
                new OutboxRecorder(CONTEXT, outbox(outbox, serializer), clock), clock);
    }

    @Bean
    RefreshClinicalHistoryHandler refreshClinicalHistoryHandler(HistoryReadModelWriter readModel,
                                                                LegacyHistorySource legacy, Clock clock,
                                                                @Value("${redsalud.legacy.ttl:PT24H}") Duration ttl) {
        return new RefreshClinicalHistoryHandler(readModel, legacy, ttl, clock);
    }

    @Bean
    GetClinicalHistoryHandler getClinicalHistoryHandler(HistoryReadModelReader readModel, Clock clock) {
        return new GetClinicalHistoryHandler(readModel, clock);
    }

    @Bean
    OutboxRelay legacyOutboxRelay(LegacyOutboxRepository outbox, EventSerializer serializer, EventBusPort bus,
                                  Clock clock) {
        return new OutboxRelay(outbox(outbox, serializer), bus, clock);
    }

    private static OutboxPort outbox(LegacyOutboxRepository repository, EventSerializer serializer) {
        return new JpaOutbox<>(repository, LegacyOutboxRecord::new, serializer);
    }
}
