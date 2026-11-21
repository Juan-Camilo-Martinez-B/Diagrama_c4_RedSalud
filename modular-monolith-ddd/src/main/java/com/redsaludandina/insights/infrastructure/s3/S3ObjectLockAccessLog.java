package com.redsaludandina.insights.infrastructure.s3;

import com.redsaludandina.insights.application.port.AccessLogReader;
import com.redsaludandina.insights.application.port.AccessLogWriter;
import com.redsaludandina.insights.application.query.AccessLogLine;
import com.redsaludandina.insights.domain.audit.AccessRecord;
import com.redsaludandina.insights.domain.audit.AccessType;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.ObjectLockMode;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import tools.jackson.databind.json.JsonMapper;

/**
 * Evidencia de accesos en S3 con Object Lock en modo COMPLIANCE (ADR-04): nadie, ni la cuenta raíz,
 * puede borrar ni sobrescribir un registro antes de que venza su retención.
 *
 * <ul>
 *   <li>Un objeto por registro, bajo el prefijo del recurso: «quién vio X» es listar un prefijo.</li>
 *   <li>{@code If-None-Match: *}: si el objeto ya existe (entrega repetida), S3 responde 412 y no se
 *       escribe nada.</li>
 * </ul>
 */
public final class S3ObjectLockAccessLog implements AccessLogWriter, AccessLogReader {

    static final String PREFIX = "access-log/";
    private static final int PRECONDITION_FAILED = 412;

    record StoredRecord(UUID id, String type, String userId, String resource, String reason, Instant occurredAt,
                        String detail) {
    }

    private final S3Client s3;
    private final String bucket;
    private final Duration retention;
    private final JsonMapper json;

    public S3ObjectLockAccessLog(S3Client s3, String bucket, Duration retention, JsonMapper json) {
        if (retention == null || retention.isZero() || retention.isNegative()) {
            throw new IllegalArgumentException("La retención de la auditoría debe ser positiva");
        }
        this.s3 = s3;
        this.bucket = bucket;
        this.retention = retention;
        this.json = json;
    }

    static String prefixOf(String resource) {
        return PREFIX + URLEncoder.encode(resource, StandardCharsets.UTF_8) + "/";
    }

    static String keyOf(AccessRecord accessRecord) {
        return prefixOf(accessRecord.resource()) + accessRecord.occurredAt() + "_" + accessRecord.id() + ".json";
    }

    @Override
    public boolean appendIfAbsent(AccessRecord accessRecord) {
        StoredRecord stored = new StoredRecord(accessRecord.id(), accessRecord.type().name(), accessRecord.userId(),
                accessRecord.resource(), accessRecord.reason(), accessRecord.occurredAt(), accessRecord.detail());
        try {
            s3.putObject(object -> object
                            .bucket(bucket)
                            .key(keyOf(accessRecord))
                            .contentType("application/json")
                            .ifNoneMatch("*")
                            .objectLockMode(ObjectLockMode.COMPLIANCE)
                            .objectLockRetainUntilDate(accessRecord.occurredAt().plus(retention))
                            .checksumAlgorithm(ChecksumAlgorithm.SHA256),
                    RequestBody.fromString(json.writeValueAsString(stored), StandardCharsets.UTF_8));
            return true;
        } catch (S3Exception error) {
            return rethrowUnlessAlreadyStored(error);
        }
    }

    private static boolean rethrowUnlessAlreadyStored(S3Exception error) {
        if (error.statusCode() != PRECONDITION_FAILED) {
            throw error;
        }
        return false;
    }

    @Override
    public List<AccessLogLine> clinicalReadsOf(String resource) {
        return s3.listObjectsV2Paginator(listing -> listing.bucket(bucket).prefix(prefixOf(resource)))
                .contents().stream()
                .map(S3Object::key)
                .map(key -> s3.getObjectAsBytes(read -> read.bucket(bucket).key(key)).asUtf8String())
                .map(content -> json.readValue(content, StoredRecord.class))
                .filter(stored -> AccessType.CLINICAL_READ.name().equals(stored.type()))
                .sorted(Comparator.comparing(StoredRecord::occurredAt).thenComparing(stored -> stored.id().toString()))
                .map(stored -> new AccessLogLine(stored.userId(), stored.reason(), stored.occurredAt()))
                .toList();
    }
}
