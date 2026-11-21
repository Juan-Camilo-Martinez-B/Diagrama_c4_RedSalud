package com.redsaludandina.episode.infrastructure.persistence;

import com.redsaludandina.episode.application.query.EpisodeView.AttachmentView;
import com.redsaludandina.episode.domain.model.Attachment;
import com.redsaludandina.episode.domain.model.AttachmentType;
import com.redsaludandina.shared.domain.ClinicalId;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/** Fila {@code episode.attachment}. */
@Embeddable
public class AttachmentEmbeddable {

    @Column(nullable = false)
    private String id;

    @Column(nullable = false)
    private String type;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(nullable = false)
    private String sha256;

    protected AttachmentEmbeddable() {
    }

    AttachmentEmbeddable(Attachment attachment) {
        this.id = attachment.id().value();
        this.type = attachment.type().name();
        this.storageKey = attachment.storageKey();
        this.sha256 = attachment.sha256();
    }

    Attachment toDomain() {
        return new Attachment(ClinicalId.of(id), AttachmentType.valueOf(type), storageKey, sha256);
    }

    AttachmentView toView() {
        return new AttachmentView(id, type, storageKey);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AttachmentEmbeddable attachment && Objects.equals(id, attachment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
