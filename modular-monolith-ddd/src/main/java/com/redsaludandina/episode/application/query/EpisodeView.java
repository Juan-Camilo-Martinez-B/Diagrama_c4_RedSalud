package com.redsaludandina.episode.application.query;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Modelo de lectura del episodio clínico. */
public record EpisodeView(String id, String patientId, String slotId, String centerId, String status,
                          Map<String, String> care, List<AttachmentView> attachments, Instant attendedAt) {

    public record AttachmentView(String id, String type, String storageKey) {
    }
}
