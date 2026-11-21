package com.redsaludandina.episode.application.query;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** URL prefirmada para subir un adjunto directo a S3, con los encabezados que S3 exige. */
public record UploadLink(URI url, Map<String, List<String>> headers, String storageKey, Instant expiresAt) {
}
