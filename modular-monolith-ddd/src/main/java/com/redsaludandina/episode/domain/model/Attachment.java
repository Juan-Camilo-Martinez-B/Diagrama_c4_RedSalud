package com.redsaludandina.episode.domain.model;

import com.redsaludandina.shared.domain.ClinicalId;
import com.redsaludandina.shared.domain.Require;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Adjunto del episodio (examen o imagen) guardado en S3 cifrado con KMS. La huella SHA-256 permite
 * reconocer un reenvío idéntico del mismo adjunto durante la sincronización.
 */
public record Attachment(ClinicalId id, AttachmentType type, String storageKey, String sha256) {

    private static final Pattern SHA_256 = Pattern.compile("^[0-9a-f]{64}$");

    public Attachment {
        Require.notNull(id, "attachmentId");
        Require.notNull(type, "type");
        storageKey = Require.text(storageKey, "storageKey");
        sha256 = Require.text(sha256, "sha256").toLowerCase(Locale.ROOT);
        if (!SHA_256.matcher(sha256).matches()) {
            throw new IllegalArgumentException("La huella del adjunto debe ser SHA-256 en hexadecimal");
        }
    }

    public boolean sameContentAs(Attachment other) {
        return type == other.type && sha256.equals(other.sha256);
    }
}
