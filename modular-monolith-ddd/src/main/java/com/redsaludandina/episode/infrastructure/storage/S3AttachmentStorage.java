package com.redsaludandina.episode.infrastructure.storage;

import com.redsaludandina.episode.application.port.AttachmentStorage;
import com.redsaludandina.episode.application.query.DownloadLink;
import com.redsaludandina.episode.application.query.UploadLink;
import com.redsaludandina.shared.domain.ClinicalId;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

/**
 * Adjuntos en S3 con URL prefirmadas. La subida exige cifrado SSE-KMS con la clave del contexto
 * Episodio (Caso §5: llaves KMS por contexto) y el checksum SHA-256 declarado, así S3 rechaza un
 * archivo distinto al anunciado.
 */
public final class S3AttachmentStorage implements AttachmentStorage {

    private static final Pattern SHA_256 = Pattern.compile("^[0-9a-f]{64}$");

    private final S3Presigner presigner;
    private final String bucket;
    private final String kmsKey;
    private final Duration validity;

    public S3AttachmentStorage(S3Presigner presigner, String bucket, String kmsKey, Duration validity) {
        this.presigner = presigner;
        this.bucket = bucket;
        this.kmsKey = kmsKey;
        this.validity = validity;
    }

    @Override
    public String keyFor(ClinicalId episodeId, ClinicalId attachmentId) {
        return "episodes/" + episodeId.value() + "/attachments/" + attachmentId.value();
    }

    @Override
    public UploadLink presignUpload(ClinicalId episodeId, ClinicalId attachmentId, String sha256) {
        String hash = sha256.strip().toLowerCase(Locale.ROOT);
        if (!SHA_256.matcher(hash).matches()) {
            throw new IllegalArgumentException("La huella del adjunto debe ser SHA-256 en hexadecimal");
        }
        String key = keyFor(episodeId, attachmentId);
        String checksum = Base64.getEncoder().encodeToString(HexFormat.of().parseHex(hash));
        PresignedPutObjectRequest signed = presigner.presignPutObject(request -> request
                .signatureDuration(validity)
                .putObjectRequest(object -> object
                        .bucket(bucket)
                        .key(key)
                        .serverSideEncryption(ServerSideEncryption.AWS_KMS)
                        .ssekmsKeyId(kmsKey)
                        .checksumSHA256(checksum)));
        return new UploadLink(uri(signed.url()), signed.signedHeaders(), key, signed.expiration());
    }

    @Override
    public DownloadLink presignDownload(String storageKey) {
        PresignedGetObjectRequest signed = presigner.presignGetObject(request -> request
                .signatureDuration(validity)
                .getObjectRequest(object -> object.bucket(bucket).key(storageKey)));
        return new DownloadLink(uri(signed.url()), signed.expiration());
    }

    private static URI uri(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException impossible) {
            throw new IllegalStateException("URL prefirmada inválida", impossible);
        }
    }
}
