package com.redsaludandina.episode.infrastructure.config;

import com.redsaludandina.episode.application.command.AttachFileHandler;
import com.redsaludandina.episode.application.command.AuthorizeEpisodeAccessHandler;
import com.redsaludandina.episode.application.command.CloseTeleconsultationHandler;
import com.redsaludandina.episode.application.command.EpisodeUpdater;
import com.redsaludandina.episode.application.command.OpenEpisodeHandler;
import com.redsaludandina.episode.application.command.RecordCareHandler;
import com.redsaludandina.episode.application.command.RecordEpisodeReadHandler;
import com.redsaludandina.episode.application.command.sync.OfflineChangeTarget;
import com.redsaludandina.episode.application.command.sync.SyncOfflineBatchHandler;
import com.redsaludandina.episode.application.port.AttachmentStorage;
import com.redsaludandina.episode.application.query.GetAttachmentDownloadLinkHandler;
import com.redsaludandina.episode.application.query.GetAttachmentUploadLinkHandler;
import com.redsaludandina.episode.application.query.GetEpisodeHandler;
import com.redsaludandina.episode.application.query.GetSyncResultHandler;
import com.redsaludandina.episode.infrastructure.persistence.EpisodeJpaRepository;
import com.redsaludandina.episode.infrastructure.persistence.EpisodeOutboxRecord;
import com.redsaludandina.episode.infrastructure.persistence.EpisodeOutboxRepository;
import com.redsaludandina.episode.infrastructure.persistence.EpisodeQueryRepository;
import com.redsaludandina.episode.infrastructure.persistence.JpaEpisodeReadModel;
import com.redsaludandina.episode.infrastructure.persistence.JpaEpisodeRepository;
import com.redsaludandina.episode.infrastructure.persistence.JpaSyncReceiptReadModel;
import com.redsaludandina.episode.infrastructure.persistence.JpaSyncReceiptRepository;
import com.redsaludandina.episode.infrastructure.persistence.SyncReceiptJpaRepository;
import com.redsaludandina.episode.infrastructure.persistence.SyncReceiptQueryRepository;
import com.redsaludandina.episode.infrastructure.storage.S3AttachmentStorage;
import com.redsaludandina.identity.application.command.AuthorizeAccessCommand;
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
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import tools.jackson.databind.json.JsonMapper;

/** Cableado del contexto Episodio clínico, incluida su sincronización offline. */
@Configuration
public class EpisodeConfiguration {

    static final String CONTEXT = "episode";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Bean
    AttachmentStorage attachmentStorage(S3Presigner presigner,
                                        @Value("${redsalud.s3.attachments-bucket}") String bucket,
                                        @Value("${redsalud.s3.attachments-kms-key}") String kmsKey,
                                        @Value("${redsalud.s3.url-validity:PT10M}") Duration validity) {
        return new S3AttachmentStorage(presigner, bucket, kmsKey, validity);
    }

    @Bean
    EpisodeUpdater episodeUpdater(EpisodeJpaRepository episodes, EpisodeOutboxRepository outbox,
                                  EventSerializer serializer, UnitOfWork unitOfWork, Clock clock) {
        return new EpisodeUpdater(new JpaEpisodeRepository(episodes), unitOfWork,
                new OutboxRecorder(CONTEXT, outbox(outbox, serializer), clock));
    }

    // ------------------------------------------------------------------ comandos

    @Bean
    OpenEpisodeHandler openEpisodeHandler(EpisodeJpaRepository episodes, UnitOfWork unitOfWork) {
        return new OpenEpisodeHandler(new JpaEpisodeRepository(episodes), unitOfWork);
    }

    @Bean
    RecordCareHandler recordCareHandler(EpisodeUpdater updater, Clock clock) {
        return new RecordCareHandler(updater, clock);
    }

    @Bean
    AttachFileHandler attachFileHandler(EpisodeUpdater updater, AttachmentStorage storage) {
        return new AttachFileHandler(updater, storage);
    }

    @Bean
    CloseTeleconsultationHandler closeTeleconsultationHandler(EpisodeUpdater updater, Clock clock) {
        return new CloseTeleconsultationHandler(updater, clock);
    }

    @Bean
    AuthorizeEpisodeAccessHandler authorizeEpisodeAccessHandler(EpisodeUpdater updater,
                                                                CommandHandler<AuthorizeAccessCommand> authorizeAccess) {
        return new AuthorizeEpisodeAccessHandler(updater, authorizeAccess);
    }

    @Bean
    RecordEpisodeReadHandler recordEpisodeReadHandler(EpisodeUpdater updater,
                                                      AuthorizeEpisodeAccessHandler authorize, Clock clock) {
        return new RecordEpisodeReadHandler(updater, authorize, clock);
    }

    @Bean
    SyncOfflineBatchHandler syncOfflineBatchHandler(SyncReceiptJpaRepository receipts, OpenEpisodeHandler open,
                                                    RecordCareHandler recordCare, AttachFileHandler attachFile,
                                                    CloseTeleconsultationHandler close, UnitOfWork unitOfWork,
                                                    Clock clock) {
        return new SyncOfflineBatchHandler(new JpaSyncReceiptRepository(receipts, JSON),
                new OfflineChangeTarget(open, recordCare, attachFile, close), unitOfWork, clock);
    }

    // ------------------------------------------------------------------ consultas

    @Bean
    GetEpisodeHandler getEpisodeHandler(EpisodeQueryRepository queries) {
        return new GetEpisodeHandler(new JpaEpisodeReadModel(queries));
    }

    @Bean
    GetAttachmentUploadLinkHandler getAttachmentUploadLinkHandler(AttachmentStorage storage) {
        return new GetAttachmentUploadLinkHandler(storage);
    }

    @Bean
    GetAttachmentDownloadLinkHandler getAttachmentDownloadLinkHandler(EpisodeQueryRepository queries,
                                                                      AttachmentStorage storage) {
        return new GetAttachmentDownloadLinkHandler(new JpaEpisodeReadModel(queries), storage);
    }

    @Bean
    GetSyncResultHandler getSyncResultHandler(SyncReceiptQueryRepository queries) {
        return new GetSyncResultHandler(new JpaSyncReceiptReadModel(queries, JSON));
    }

    // ------------------------------------------------------------------ outbox

    @Bean
    OutboxRelay episodeOutboxRelay(EpisodeOutboxRepository outbox, EventSerializer serializer, EventBusPort bus,
                                   Clock clock) {
        return new OutboxRelay(outbox(outbox, serializer), bus, clock);
    }

    private static OutboxPort outbox(EpisodeOutboxRepository repository, EventSerializer serializer) {
        return new JpaOutbox<>(repository, EpisodeOutboxRecord::new, serializer);
    }
}
