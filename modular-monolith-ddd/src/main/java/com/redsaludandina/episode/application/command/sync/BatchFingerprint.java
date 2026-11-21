package com.redsaludandina.episode.application.command.sync;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Huella SHA-256 del contenido de un lote. Los records tienen un {@code toString} determinista, así
 * que el mismo lote produce siempre la misma huella y cualquier cambio produce otra.
 */
final class BatchFingerprint {

    private BatchFingerprint() {
    }

    static String of(String centerId, List<OfflineItem> items) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((centerId + "|" + items).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 no disponible", impossible);
        }
    }
}
