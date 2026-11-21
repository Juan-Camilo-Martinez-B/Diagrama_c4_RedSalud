package com.redsaludandina.episode.infrastructure.web;

import com.redsaludandina.episode.application.command.sync.OfflineChange;
import com.redsaludandina.episode.application.command.sync.OfflineItem;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Un cambio del lote tal como llega en JSON. {@code type} elige el cambio offline; una tabla de
 * traductores reemplaza al {@code switch} (agregar un tipo es agregar una entrada).
 */
record SyncItemRequest(String changeId, String aggregateId, long sequence, String type, String episodeId,
                       String patientId, String slotId, String field, String text, Instant stampedAt,
                       String attachmentId, String attachmentType, String storageKey, String sha256,
                       String bookingId, String prescriptionId) {

    private static final Map<String, Function<SyncItemRequest, OfflineChange>> CHANGES = Map.of(
            "OPEN_EPISODE", item -> new OfflineChange.OpenEpisode(item.episodeId, item.patientId, item.slotId),
            "RECORD_CARE", item -> new OfflineChange.RecordCare(item.episodeId, item.field, item.text, item.stampedAt),
            "ATTACH_FILE", item -> new OfflineChange.AttachFile(item.episodeId, item.attachmentId, item.attachmentType,
                    item.storageKey, item.sha256),
            "CLOSE_TELECONSULTATION", item -> new OfflineChange.CloseTeleconsultation(item.episodeId),
            "BOOK_SLOT", item -> new OfflineChange.BookSlot(item.slotId, item.bookingId, item.patientId),
            "SIGN_PRESCRIPTION", item -> new OfflineChange.SignPrescription(item.prescriptionId));

    OfflineItem toItem() {
        String key = Optional.ofNullable(type).orElse("").toUpperCase(Locale.ROOT);
        Function<SyncItemRequest, OfflineChange> translator = Optional.ofNullable(CHANGES.get(key))
                .orElseThrow(() -> new IllegalArgumentException("Tipo de cambio offline desconocido: " + type));
        return new OfflineItem(changeId, aggregateId, sequence, translator.apply(this));
    }
}
