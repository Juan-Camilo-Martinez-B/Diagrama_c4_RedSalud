package com.redsaludandina.episode.infrastructure.persistence;

import com.redsaludandina.episode.application.query.EpisodeView;
import com.redsaludandina.episode.domain.model.CareField;
import com.redsaludandina.episode.domain.model.CareRecord;
import com.redsaludandina.episode.domain.model.ClinicalValue;
import com.redsaludandina.episode.domain.model.Episode;
import com.redsaludandina.episode.domain.model.EpisodeStatus;
import com.redsaludandina.shared.domain.ClinicalId;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Fila JPA del episodio. {@link Version} detecta dos sincronizaciones que modifican el mismo episodio
 * a la vez: la segunda falla y su cambio queda pendiente para reintentar.
 */
@Entity
@Table(schema = "episode", name = "episode")
public class EpisodeEntity {

    @Id
    private String id;

    @Version
    private Long version;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "slot_id")
    private String slotId;

    @Column(name = "center_id", nullable = false)
    private String centerId;

    @Column(nullable = false)
    private String status;

    @Column(name = "attended_at")
    private Instant attendedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(schema = "episode", name = "care_value", joinColumns = @JoinColumn(name = "episode_id"))
    @MapKeyColumn(name = "field")
    private Map<String, CareValueEmbeddable> care = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(schema = "episode", name = "attachment", joinColumns = @JoinColumn(name = "episode_id"))
    private Set<AttachmentEmbeddable> attachments = new HashSet<>();

    protected EpisodeEntity() {
    }

    EpisodeEntity(String id) {
        this.id = id;
    }

    void copyFrom(Episode episode) {
        patientId = episode.patientId();
        slotId = episode.slotId().map(ClinicalId::value).orElse(null);
        centerId = episode.centerId();
        status = episode.status().name();
        attendedAt = episode.attendedAt().orElse(null);
        care.clear();
        episode.care().values().forEach((field, value) -> care.put(field.name(), new CareValueEmbeddable(value)));
        attachments.clear();
        episode.attachments().forEach(attachment -> attachments.add(new AttachmentEmbeddable(attachment)));
    }

    Episode toDomain() {
        Map<CareField, ClinicalValue> values = new EnumMap<>(CareField.class);
        care.forEach((field, value) -> values.put(CareField.valueOf(field), value.toDomain()));
        return Episode.restore(ClinicalId.of(id), patientId, ClinicalId.ofNullable(slotId), centerId,
                EpisodeStatus.valueOf(status), CareRecord.of(values),
                attachments.stream().map(AttachmentEmbeddable::toDomain).toList(), attendedAt);
    }

    EpisodeView toView() {
        Map<String, String> careTexts = new LinkedHashMap<>();
        care.forEach((field, value) -> careTexts.put(field, value.text()));
        return new EpisodeView(id, patientId, slotId, centerId, status, careTexts,
                attachments.stream().map(AttachmentEmbeddable::toView).toList(), attendedAt);
    }
}
