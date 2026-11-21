package com.redsaludandina.episode.infrastructure.persistence;

import com.redsaludandina.episode.domain.model.ClinicalValue;
import com.redsaludandina.shared.domain.CenterStamp;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

/** Fila {@code episode.care_value}: el valor vigente de un campo con su sello de centro. */
@Embeddable
public class CareValueEmbeddable {

    @Column(nullable = false)
    private String text;

    @Column(name = "center_id", nullable = false)
    private String centerId;

    @Column(name = "stamped_at", nullable = false)
    private Instant stampedAt;

    protected CareValueEmbeddable() {
    }

    CareValueEmbeddable(ClinicalValue value) {
        this.text = value.text();
        this.centerId = value.stamp().centerId();
        this.stampedAt = value.stamp().instant();
    }

    ClinicalValue toDomain() {
        return new ClinicalValue(text, new CenterStamp(centerId, stampedAt));
    }

    String text() {
        return text;
    }
}
