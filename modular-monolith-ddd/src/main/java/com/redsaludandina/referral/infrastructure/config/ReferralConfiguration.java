package com.redsaludandina.referral.infrastructure.config;

import com.redsaludandina.referral.application.command.CloseReferralHandler;
import com.redsaludandina.referral.application.command.ReferralWriter;
import com.redsaludandina.referral.application.command.RequestReferralHandler;
import com.redsaludandina.referral.application.event.RecordAttentionOnPatientAttended;
import com.redsaludandina.referral.application.event.ScheduleReferralOnSlotBooked;
import com.redsaludandina.referral.application.query.ListPendingAlertsHandler;
import com.redsaludandina.referral.infrastructure.persistence.JpaReferralReadModel;
import com.redsaludandina.referral.infrastructure.persistence.JpaReferralRepository;
import com.redsaludandina.referral.infrastructure.persistence.ReferralInboxRecord;
import com.redsaludandina.referral.infrastructure.persistence.ReferralInboxRepository;
import com.redsaludandina.referral.infrastructure.persistence.ReferralJpaRepository;
import com.redsaludandina.referral.infrastructure.persistence.ReferralOutboxRecord;
import com.redsaludandina.referral.infrastructure.persistence.ReferralOutboxRepository;
import com.redsaludandina.referral.infrastructure.persistence.ReferralQueryRepository;
import com.redsaludandina.shared.application.UnitOfWork;
import com.redsaludandina.shared.application.messaging.EventBusPort;
import com.redsaludandina.shared.application.messaging.InboxPort;
import com.redsaludandina.shared.application.messaging.OutboxPort;
import com.redsaludandina.shared.application.messaging.OutboxRecorder;
import com.redsaludandina.shared.application.messaging.OutboxRelay;
import com.redsaludandina.shared.infrastructure.messaging.EventSerializer;
import com.redsaludandina.shared.infrastructure.persistence.inbox.JpaInbox;
import com.redsaludandina.shared.infrastructure.persistence.outbox.JpaOutbox;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cableado del contexto Interconsulta. */
@Configuration
public class ReferralConfiguration {

    static final String CONTEXT = "referral";

    @Bean
    ReferralWriter referralWriter(ReferralJpaRepository referrals, ReferralOutboxRepository outbox,
                                  EventSerializer serializer, Clock clock) {
        return new ReferralWriter(new JpaReferralRepository(referrals),
                new OutboxRecorder(CONTEXT, outbox(outbox, serializer), clock));
    }

    @Bean
    RequestReferralHandler requestReferralHandler(ReferralJpaRepository referrals, ReferralWriter writer,
                                                  UnitOfWork unitOfWork, Clock clock) {
        return new RequestReferralHandler(new JpaReferralRepository(referrals), writer, unitOfWork, clock);
    }

    @Bean
    CloseReferralHandler closeReferralHandler(ReferralJpaRepository referrals, ReferralWriter writer,
                                              UnitOfWork unitOfWork, Clock clock) {
        return new CloseReferralHandler(new JpaReferralRepository(referrals), writer, unitOfWork, clock);
    }

    @Bean
    ScheduleReferralOnSlotBooked scheduleReferralOnSlotBooked(ReferralJpaRepository referrals, ReferralWriter writer,
                                                              ReferralInboxRepository inbox, UnitOfWork unitOfWork,
                                                              Clock clock) {
        return new ScheduleReferralOnSlotBooked(new JpaReferralRepository(referrals), writer, inbox(inbox, clock),
                unitOfWork, clock);
    }

    @Bean
    RecordAttentionOnPatientAttended recordAttentionOnPatientAttended(ReferralJpaRepository referrals,
                                                                      ReferralWriter writer,
                                                                      ReferralInboxRepository inbox,
                                                                      UnitOfWork unitOfWork, Clock clock) {
        return new RecordAttentionOnPatientAttended(new JpaReferralRepository(referrals), writer, inbox(inbox, clock),
                unitOfWork, clock);
    }

    @Bean
    ListPendingAlertsHandler listPendingAlertsHandler(ReferralQueryRepository queries) {
        return new ListPendingAlertsHandler(new JpaReferralReadModel(queries));
    }

    @Bean
    OutboxRelay referralOutboxRelay(ReferralOutboxRepository outbox, EventSerializer serializer, EventBusPort bus,
                                    Clock clock) {
        return new OutboxRelay(outbox(outbox, serializer), bus, clock);
    }

    private static OutboxPort outbox(ReferralOutboxRepository repository, EventSerializer serializer) {
        return new JpaOutbox<>(repository, ReferralOutboxRecord::new, serializer);
    }

    private static InboxPort inbox(ReferralInboxRepository repository, Clock clock) {
        return new JpaInbox<>(repository, ReferralInboxRecord::new, clock);
    }
}
