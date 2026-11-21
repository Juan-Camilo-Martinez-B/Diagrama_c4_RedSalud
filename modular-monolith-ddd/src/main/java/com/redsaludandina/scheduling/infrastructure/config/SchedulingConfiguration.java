package com.redsaludandina.scheduling.infrastructure.config;

import com.redsaludandina.scheduling.application.command.BookSlotHandler;
import com.redsaludandina.scheduling.application.command.PublishSlotHandler;
import com.redsaludandina.scheduling.application.query.ListAvailableSlotsHandler;
import com.redsaludandina.scheduling.infrastructure.persistence.JpaSlotReadModel;
import com.redsaludandina.scheduling.infrastructure.persistence.JpaSlotRepository;
import com.redsaludandina.scheduling.infrastructure.persistence.SchedulingOutboxRecord;
import com.redsaludandina.scheduling.infrastructure.persistence.SchedulingOutboxRepository;
import com.redsaludandina.scheduling.infrastructure.persistence.SlotJpaRepository;
import com.redsaludandina.scheduling.infrastructure.persistence.SlotQueryRepository;
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

/** Cableado del contexto Agenda. */
@Configuration
public class SchedulingConfiguration {

    static final String CONTEXT = "scheduling";

    @Bean
    PublishSlotHandler publishSlotHandler(SlotJpaRepository slots, UnitOfWork unitOfWork) {
        return new PublishSlotHandler(new JpaSlotRepository(slots), unitOfWork);
    }

    @Bean
    BookSlotHandler bookSlotHandler(SlotJpaRepository slots, SchedulingOutboxRepository outbox,
                                    EventSerializer serializer, UnitOfWork unitOfWork, Clock clock) {
        return new BookSlotHandler(new JpaSlotRepository(slots), unitOfWork,
                new OutboxRecorder(CONTEXT, outbox(outbox, serializer), clock), clock);
    }

    @Bean
    ListAvailableSlotsHandler listAvailableSlotsHandler(SlotQueryRepository queries, Clock clock) {
        return new ListAvailableSlotsHandler(new JpaSlotReadModel(queries), clock);
    }

    @Bean
    OutboxRelay schedulingOutboxRelay(SchedulingOutboxRepository outbox, EventSerializer serializer,
                                      EventBusPort bus, Clock clock) {
        return new OutboxRelay(outbox(outbox, serializer), bus, clock);
    }

    private static OutboxPort outbox(SchedulingOutboxRepository repository, EventSerializer serializer) {
        return new JpaOutbox<>(repository, SchedulingOutboxRecord::new, serializer);
    }
}
