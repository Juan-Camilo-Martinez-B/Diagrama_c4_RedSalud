package com.redsaludandina.episode.domain.model;

import com.redsaludandina.shared.domain.AggregateRoot;
import com.redsaludandina.shared.domain.BusinessRuleException;
import com.redsaludandina.shared.domain.CenterStamp;
import com.redsaludandina.shared.domain.ClinicalId;
import com.redsaludandina.shared.domain.Require;
import com.redsaludandina.shared.event.ClinicalRead;
import com.redsaludandina.shared.event.ClinicalValueDiscarded;
import com.redsaludandina.shared.event.PatientAttended;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Episodio clínico de una teleconsulta (raíz de agregado). Se puede crear y modificar sin conexión:
 * su id es un ULID del cliente y cada campo clínico se resuelve con last-writer-wins por sello de
 * centro, auditando el valor descartado (Caso §4).
 */
public final class Episode extends AggregateRoot<ClinicalId> {

    public static final String CARE_WITHOUT_REASON = "CARE_WITHOUT_REASON";
    public static final String ATTACHMENT_CONFLICT = "ATTACHMENT_CONFLICT";
    public static final String ATTACHMENT_NOT_FOUND = "ATTACHMENT_NOT_FOUND";

    private final ClinicalId id;
    private final String patientId;
    private final ClinicalId slotId;
    private final String centerId;
    private EpisodeStatus status;
    private CareRecord care;
    private final Map<ClinicalId, Attachment> attachments;
    private Instant attendedAt;

    private Episode(ClinicalId id, String patientId, ClinicalId slotId, String centerId, EpisodeStatus status,
                    CareRecord care, Collection<Attachment> attachments, Instant attendedAt) {
        this.id = Require.notNull(id, "episodeId");
        this.patientId = Require.text(patientId, "patientId");
        this.slotId = slotId;
        this.centerId = Require.text(centerId, "centerId");
        this.status = Require.notNull(status, "status");
        this.care = Require.notNull(care, "care");
        this.attachments = new LinkedHashMap<>();
        attachments.forEach(attachment -> this.attachments.put(attachment.id(), attachment));
        this.attendedAt = attendedAt;
    }

    public static Episode open(ClinicalId id, String patientId, ClinicalId slotId, String centerId) {
        return new Episode(id, patientId, slotId, centerId, EpisodeStatus.OPEN, CareRecord.empty(), List.of(), null);
    }

    public static Episode restore(ClinicalId id, String patientId, ClinicalId slotId, String centerId,
                                  EpisodeStatus status, CareRecord care, Collection<Attachment> attachments,
                                  Instant attendedAt) {
        return new Episode(id, patientId, slotId, centerId, status, care, attachments, attendedAt);
    }

    /** Registra un campo de la nota de atención; si ya tenía valor, decide last-writer-wins. */
    public void recordCare(CareField field, String text, CenterStamp stamp, Instant now) {
        Require.notNull(field, "field");
        Require.notNull(now, "now");
        ClinicalValue incoming = new ClinicalValue(text, stamp);
        care.value(field).ifPresentOrElse(
                current -> resolve(field, current, incoming, now),
                () -> care = care.with(field, incoming));
    }

    private void resolve(CareField field, ClinicalValue current, ClinicalValue incoming, Instant now) {
        if (current.equals(incoming)) {
            return;
        }
        LastWriterWins outcome = LastWriterWins.between(current, incoming);
        care = care.with(field, outcome.kept());
        if (outcome.isConflict()) {
            record(new ClinicalValueDiscarded(id, field.name(), outcome.kept().text(), outcome.kept().stamp(),
                    outcome.discarded().text(), outcome.discarded().stamp(), now));
        }
    }

    /** Reenviar el mismo adjunto es idempotente; otro contenido con el mismo id es un conflicto. */
    public void attach(Attachment attachment) {
        Require.notNull(attachment, "attachment");
        Attachment existing = attachments.putIfAbsent(attachment.id(), attachment);
        if (existing != null && !existing.sameContentAs(attachment)) {
            throw new BusinessRuleException(ATTACHMENT_CONFLICT, "Ya existe otro adjunto con el id " + attachment.id());
        }
    }

    /** Cierra la teleconsulta. Exige motivo de consulta; cerrar dos veces no emite otro evento. */
    public void markAttended(Instant now) {
        Require.notNull(now, "now");
        if (status == EpisodeStatus.ATTENDED) {
            return;
        }
        if (!care.has(CareField.REASON)) {
            throw new BusinessRuleException(CARE_WITHOUT_REASON,
                    "No se puede cerrar la teleconsulta " + id + " sin motivo de consulta");
        }
        status = EpisodeStatus.ATTENDED;
        attendedAt = now;
        record(new PatientAttended(id, patientId, slotId, centerId, now));
    }

    /** Toda lectura del episodio queda auditada con usuario, fecha, recurso y motivo (Caso §5). */
    public void recordRead(String userId, String reason, Instant now) {
        record(new ClinicalRead(userId, resource(), reason, now));
    }

    public void recordAttachmentRead(ClinicalId attachmentId, String userId, String reason, Instant now) {
        if (!attachments.containsKey(Require.notNull(attachmentId, "attachmentId"))) {
            throw new BusinessRuleException(ATTACHMENT_NOT_FOUND, "El episodio " + id + " no tiene el adjunto " + attachmentId);
        }
        record(new ClinicalRead(userId, resource() + "/attachment/" + attachmentId, reason, now));
    }

    public String resource() {
        return "episode/" + id;
    }

    @Override
    public ClinicalId id() {
        return id;
    }

    public String patientId() {
        return patientId;
    }

    public Optional<ClinicalId> slotId() {
        return Optional.ofNullable(slotId);
    }

    public String centerId() {
        return centerId;
    }

    public EpisodeStatus status() {
        return status;
    }

    public CareRecord care() {
        return care;
    }

    public Collection<Attachment> attachments() {
        return Collections.unmodifiableCollection(attachments.values());
    }

    public Optional<Instant> attendedAt() {
        return Optional.ofNullable(attendedAt);
    }
}
