package com.redsaludandina.episode.application.query;

import java.net.URI;
import java.time.Instant;

/** URL prefirmada y de corta vida para descargar un adjunto de S3. */
public record DownloadLink(URI url, Instant expiresAt) {
}
