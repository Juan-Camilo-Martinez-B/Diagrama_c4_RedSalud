package com.redsaludandina.episode.application.command.sync;

import com.redsaludandina.episode.application.command.AttachFileCommand;
import com.redsaludandina.episode.application.command.CloseTeleconsultationCommand;
import com.redsaludandina.episode.application.command.OpenEpisodeCommand;
import com.redsaludandina.episode.application.command.RecordCareCommand;
import com.redsaludandina.episode.domain.sync.ItemResult;
import com.redsaludandina.shared.domain.BusinessRuleException;
import com.redsaludandina.shared.domain.ClinicalId;
import java.time.Instant;

/**
 * Un cambio hecho sin conexión en el centro rural. Cada tipo sabe qué comando del episodio lo aplica
 * (polimorfismo en vez de un {@code switch}); los cupos y las recetas firmadas NO se resuelven en el
 * borde y se rechazan para que el cliente los reintente contra el servidor (Caso §4).
 */
public sealed interface OfflineChange {

    void applyTo(OfflineChangeTarget target, String centerId);

    record OpenEpisode(String episodeId, String patientId, String slotId) implements OfflineChange {
        @Override
        public void applyTo(OfflineChangeTarget target, String centerId) {
            target.open().handle(new OpenEpisodeCommand(ClinicalId.of(episodeId), patientId,
                    ClinicalId.ofNullable(slotId), centerId));
        }
    }

    record RecordCare(String episodeId, String field, String text, Instant stampedAt) implements OfflineChange {
        @Override
        public void applyTo(OfflineChangeTarget target, String centerId) {
            target.recordCare().handle(new RecordCareCommand(ClinicalId.of(episodeId), field, text, centerId, stampedAt));
        }
    }

    record AttachFile(String episodeId, String attachmentId, String type, String storageKey, String sha256)
            implements OfflineChange {
        @Override
        public void applyTo(OfflineChangeTarget target, String centerId) {
            target.attachFile().handle(new AttachFileCommand(ClinicalId.of(episodeId), ClinicalId.of(attachmentId),
                    type, storageKey, sha256));
        }
    }

    record CloseTeleconsultation(String episodeId) implements OfflineChange {
        @Override
        public void applyTo(OfflineChangeTarget target, String centerId) {
            target.close().handle(new CloseTeleconsultationCommand(ClinicalId.of(episodeId)));
        }
    }

    record BookSlot(String slotId, String bookingId, String patientId) implements OfflineChange {
        @Override
        public void applyTo(OfflineChangeTarget target, String centerId) {
            throw requiresServer();
        }
    }

    record SignPrescription(String prescriptionId) implements OfflineChange {
        @Override
        public void applyTo(OfflineChangeTarget target, String centerId) {
            throw requiresServer();
        }
    }

    private static BusinessRuleException requiresServer() {
        return new BusinessRuleException(ItemResult.REQUIRES_SERVER,
                "Cupos y recetas firmadas no se resuelven en el borde: reintente contra el servidor");
    }
}
