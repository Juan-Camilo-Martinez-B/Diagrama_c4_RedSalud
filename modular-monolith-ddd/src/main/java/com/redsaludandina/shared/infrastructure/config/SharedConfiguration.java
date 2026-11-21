package com.redsaludandina.shared.infrastructure.config;

import com.redsaludandina.shared.application.UnitOfWork;
import com.redsaludandina.shared.application.messaging.EventBusPort;
import com.redsaludandina.shared.application.messaging.EventConsumer;
import com.redsaludandina.shared.application.messaging.OutboxRelay;
import com.redsaludandina.shared.event.PublishedLanguage;
import com.redsaludandina.shared.infrastructure.messaging.EventSerializer;
import com.redsaludandina.shared.infrastructure.messaging.InProcessEventBus;
import com.redsaludandina.shared.infrastructure.messaging.ScheduledOutboxRelay;
import com.redsaludandina.shared.infrastructure.persistence.SpringUnitOfWork;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/** Piezas comunes a todos los contextos: reloj, unidad de trabajo, serializador, bus y relevo. */
@Configuration
@EnableScheduling
public class SharedConfiguration {

    @Bean
    Clock clock(@Value("${redsalud.time-zone:UTC}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }

    @Bean
    UnitOfWork unitOfWork(PlatformTransactionManager transactions) {
        return new SpringUnitOfWork(new TransactionTemplate(transactions));
    }

    @Bean
    EventSerializer eventSerializer() {
        return new EventSerializer(JsonMapper.builder().build(), PublishedLanguage.EVENTS);
    }

    @Bean
    @ConditionalOnProperty(name = "redsalud.messaging.type", havingValue = "local", matchIfMissing = true)
    EventBusPort inProcessEventBus(List<EventConsumer<?>> consumers) {
        return new InProcessEventBus(consumers);
    }

    @Bean
    ScheduledOutboxRelay scheduledOutboxRelay(List<OutboxRelay> relays,
                                              @Value("${redsalud.outbox.auto-relay:true}") boolean automatic,
                                              @Value("${redsalud.outbox.batch-size:100}") int batchSize) {
        return new ScheduledOutboxRelay(relays, automatic, batchSize);
    }
}
