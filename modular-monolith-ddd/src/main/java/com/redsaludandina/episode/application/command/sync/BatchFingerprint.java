package com.redsaludandina.episode.application.command.sync;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Huella SHA-256 del lote. Usa el {@code toString} canónico de cada cambio, ordenado por agregado y
 * secuencia, para que el mismo lote produzca la misma huella en cualquier instancia.
 */
final class BatchFingerprint {

    private static final Comparator<OfflineItem> CANONICAL_ORDER = Comparator
            .comparing(OfflineItem::aggregateId, Comparator.nullsFirst(String::compareTo))
            .thenComparingLong(OfflineItem::sequence)
            .thenComparing(OfflineItem::changeId, Comparator.nullsFirst(String::compareTo));

    private BatchFingerprint() {
    }

    static String of(String centerId, List<OfflineItem> items) {
        return sha256(canonical(centerId, items));
    }

    private static String canonical(String centerId, List<OfflineItem> items) {
        String body = items.stream().sorted(CANONICAL_ORDER).map(OfflineItem::toString).collect(Collectors.joining("\n"));
        return centerId + "\n" + body;
    }

    private static String sha256(String canonical) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 no disponible", impossible);
        }
    }
}
