package com.redsaludandina.insights.infrastructure.config;

import com.redsaludandina.insights.application.event.AuditConsumer;
import com.redsaludandina.insights.application.event.IndicatorProjection;
import com.redsaludandina.insights.application.event.IndicatorProjection.Rule;
import com.redsaludandina.insights.application.event.InsightsRules;
import com.redsaludandina.insights.application.port.AccessLogReader;
import com.redsaludandina.insights.application.port.AccessLogWriter;
import com.redsaludandina.insights.application.query.GetUsageIndicatorHandler;
import com.redsaludandina.insights.application.query.WhoAccessedHandler;
import com.redsaludandina.insights.domain.audit.AccessRecord;
import com.redsaludandina.insights.infrastructure.persistence.AccessRecordJpaRepository;
import com.redsaludandina.insights.infrastructure.persistence.AccessRecordQueryRepository;
import com.redsaludandina.insights.infrastructure.persistence.InsightsInboxRecord;
import com.redsaludandina.insights.infrastructure.persistence.InsightsInboxRepository;
import com.redsaludandina.insights.infrastructure.persistence.JpaAccessLogReader;
import com.redsaludandina.insights.infrastructure.persistence.JpaAccessLogWriter;
import com.redsaludandina.insights.infrastructure.persistence.JpaUsageIndicatorReadModel;
import com.redsaludandina.insights.infrastructure.persistence.JpaUsageIndicatorRepository;
import com.redsaludandina.insights.infrastructure.persistence.UsageIndicatorJpaRepository;
import com.redsaludandina.insights.infrastructure.persistence.UsageIndicatorQueryRepository;
import com.redsaludandina.insights.infrastructure.s3.S3ObjectLockAccessLog;
import com.redsaludandina.shared.application.UnitOfWork;
import com.redsaludandina.shared.domain.DomainEvent;
import com.redsaludandina.shared.event.AccessDenied;
import com.redsaludandina.shared.event.ClinicalRead;
import com.redsaludandina.shared.event.ClinicalValueDiscarded;
import com.redsaludandina.shared.event.PatientAttended;
import com.redsaludandina.shared.event.ReferralClosed;
import com.redsaludandina.shared.event.ReferralRequested;
import com.redsaludandina.shared.event.SlotBooked;
import com.redsaludandina.shared.infrastructure.persistence.inbox.JpaInbox;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import tools.jackson.databind.json.JsonMapper;

/** Cableado de Analítica y evidencia (auditoría de accesos + analítica de uso). */
@Configuration
public class InsightsConfiguration {

    // ------------------------------------------------------------------ evidencia de accesos

    @Bean
    @ConditionalOnProperty(name = "redsalud.audit.type", havingValue = "local", matchIfMissing = true)
    AccessLogWriter localAccessLogWriter(AccessRecordJpaRepository jpa) {
        return new JpaAccessLogWriter(jpa);
    }

    @Bean
    @ConditionalOnProperty(name = "redsalud.audit.type", havingValue = "local", matchIfMissing = true)
    AccessLogReader localAccessLogReader(AccessRecordQueryRepository queries) {
        return new JpaAccessLogReader(queries);
    }

    @Bean
    @ConditionalOnProperty(name = "redsalud.audit.type", havingValue = "s3")
    S3ObjectLockAccessLog s3ObjectLockAccessLog(S3Client s3, @Value("${redsalud.s3.audit-bucket}") String bucket,
                                                @Value("${redsalud.audit.retention}") Duration retention) {
        return new S3ObjectLockAccessLog(s3, bucket, retention, JsonMapper.builder().build());
    }

    @Bean
    AuditConsumer<ClinicalRead> auditClinicalRead(AccessLogWriter accessLog) {
        return new AuditConsumer<>("insights.audit.clinical-read", ClinicalRead.class, AccessRecord::of, accessLog);
    }

    @Bean
    AuditConsumer<ClinicalValueDiscarded> auditDiscardedValue(AccessLogWriter accessLog) {
        return new AuditConsumer<>("insights.audit.discarded-value", ClinicalValueDiscarded.class, AccessRecord::of,
                accessLog);
    }

    @Bean
    AuditConsumer<AccessDenied> auditAccessDenied(AccessLogWriter accessLog) {
        return new AuditConsumer<>("insights.audit.access-denied", AccessDenied.class, AccessRecord::of, accessLog);
    }

    @Bean
    WhoAccessedHandler whoAccessedHandler(AccessLogReader accessLog) {
        return new WhoAccessedHandler(accessLog);
    }

    // ------------------------------------------------------------------ analítica de uso

    @Bean
    IndicatorProjection<SlotBooked> bookingsProjection(UsageIndicatorJpaRepository indicators,
                                                       InsightsInboxRepository inbox, UnitOfWork unitOfWork,
                                                       Clock clock) {
        return projection(InsightsRules.BOOKINGS, indicators, inbox, unitOfWork, clock);
    }

    @Bean
    IndicatorProjection<PatientAttended> teleconsultationsProjection(UsageIndicatorJpaRepository indicators,
                                                                     InsightsInboxRepository inbox,
                                                                     UnitOfWork unitOfWork, Clock clock) {
        return projection(InsightsRules.TELECONSULTATIONS, indicators, inbox, unitOfWork, clock);
    }

    @Bean
    IndicatorProjection<ReferralRequested> referralsRequestedProjection(UsageIndicatorJpaRepository indicators,
                                                                        InsightsInboxRepository inbox,
                                                                        UnitOfWork unitOfWork, Clock clock) {
        return projection(InsightsRules.REFERRALS_REQUESTED, indicators, inbox, unitOfWork, clock);
    }

    @Bean
    IndicatorProjection<ReferralClosed> referralsClosedProjection(UsageIndicatorJpaRepository indicators,
                                                                  InsightsInboxRepository inbox, UnitOfWork unitOfWork,
                                                                  Clock clock) {
        return projection(InsightsRules.REFERRALS_CLOSED, indicators, inbox, unitOfWork, clock);
    }

    @Bean
    GetUsageIndicatorHandler getUsageIndicatorHandler(UsageIndicatorQueryRepository queries) {
        return new GetUsageIndicatorHandler(new JpaUsageIndicatorReadModel(queries));
    }

    private static <E extends DomainEvent> IndicatorProjection<E> projection(Rule<E> rule,
                                                                             UsageIndicatorJpaRepository indicators,
                                                                             InsightsInboxRepository inbox,
                                                                             UnitOfWork unitOfWork, Clock clock) {
        return new IndicatorProjection<>(rule, new JpaUsageIndicatorRepository(indicators),
                new JpaInbox<>(inbox, InsightsInboxRecord::new, clock), unitOfWork, clock);
    }
}
