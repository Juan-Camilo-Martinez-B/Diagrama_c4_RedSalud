package com.redsaludandina.identity.infrastructure.config;

import com.redsaludandina.identity.application.command.AuthorizeAccessHandler;
import com.redsaludandina.identity.domain.service.AccessPolicy;
import com.redsaludandina.identity.infrastructure.persistence.IdentityOutboxRecord;
import com.redsaludandina.identity.infrastructure.persistence.IdentityOutboxRepository;
import com.redsaludandina.identity.infrastructure.persistence.JpaUserRepository;
import com.redsaludandina.identity.infrastructure.persistence.UserJpaRepository;
import com.redsaludandina.shared.application.UnitOfWork;
import com.redsaludandina.shared.application.messaging.EventBusPort;
import com.redsaludandina.shared.application.messaging.OutboxPort;
import com.redsaludandina.shared.application.messaging.OutboxRecorder;
import com.redsaludandina.shared.application.messaging.OutboxRelay;
import com.redsaludandina.shared.infrastructure.messaging.EventSerializer;
import com.redsaludandina.shared.infrastructure.persistence.outbox.JpaOutbox;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cableado del contexto Identidad y acceso. */
@Configuration
public class IdentityConfiguration {

    static final String CONTEXT = "identity";

    @Bean
    AuthorizeAccessHandler authorizeAccessHandler(UserJpaRepository users, IdentityOutboxRepository outbox,
                                                  EventSerializer serializer, UnitOfWork unitOfWork, Clock clock) {
        return new AuthorizeAccessHandler(new JpaUserRepository(users), new AccessPolicy(), unitOfWork,
                new OutboxRecorder(CONTEXT, outbox(outbox, serializer), clock), clock);
    }

    @Bean
    OutboxRelay identityOutboxRelay(IdentityOutboxRepository outbox, EventSerializer serializer, EventBusPort bus,
                                    Clock clock) {
        return new OutboxRelay(outbox(outbox, serializer), bus, clock);
    }

    private static OutboxPort outbox(IdentityOutboxRepository repository, EventSerializer serializer) {
        return new JpaOutbox<>(repository, IdentityOutboxRecord::new, serializer);
    }
}
