package com.redsaludandina.auditoria.infraestructura.adaptador.salida.s3;

import com.redsaludandina.auditoria.aplicacion.puerto.salida.RegistroAccesosPort;
import com.redsaludandina.auditoria.dominio.modelo.EventoAcceso;
import com.redsaludandina.auditoria.dominio.modelo.TipoAcceso;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
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
 * Registro de accesos append-only en S3 con Object Lock en modo COMPLIANCE (Caso §5, ADR-04).
 *
 * <ul>
 *   <li>Un objeto por acceso: {@code accesos/<recurso>/<instante>_<id>.json}. La clave sale del
 *       evento, así que el mismo evento siempre cae en la misma clave.</li>
 *   <li>{@code If-None-Match: *}: S3 rechaza (412) escribir una clave que ya existe. Así un evento
 *       entregado dos veces no se registra dos veces y un registro nunca se sobrescribe.</li>
 *   <li>Retención COMPLIANCE hasta {@code ocurridoEn + retención}: nadie puede borrarlo ni
 *       modificarlo antes, ni siquiera la cuenta raíz.</li>
 * </ul>
 *
 * <p>El bucket debe crearse con Object Lock habilitado y sin replicación fuera de la región (eso
 * se define en la infraestructura como código, no aquí).
 */
public final class RegistroAccesosS3ObjectLock implements RegistroAccesosPort {

    static final String PREFIJO = "accesos/";

    /** Representación JSON del registro guardado en S3. */
    record RegistroJson(UUID id, String tipo, String usuarioId, String recurso, String motivo, Instant ocurridoEn,
                        String detalle) {
    }

    private final S3Client s3;
    private final String bucket;
    private final Duration retencion;
    private final JsonMapper json;

    public RegistroAccesosS3ObjectLock(S3Client s3, String bucket, Duration retencion, JsonMapper json) {
        if (retencion == null || retencion.isZero() || retencion.isNegative()) {
            throw new IllegalArgumentException("La retención de la auditoría debe ser positiva");
        }
        this.s3 = s3;
        this.bucket = bucket;
        this.retencion = retencion;
        this.json = json;
    }

    static String prefijo(String recurso) {
        return PREFIJO + URLEncoder.encode(recurso, StandardCharsets.UTF_8) + "/";
    }

    static String clave(EventoAcceso evento) {
        return prefijo(evento.recurso()) + evento.ocurridoEn() + "_" + evento.id() + ".json";
    }

    @Override
    public boolean agregarSiNoExiste(EventoAcceso evento) {
        RegistroJson registro = new RegistroJson(evento.id(), evento.tipo().name(), evento.usuarioId(), evento.recurso(),
                evento.motivo(), evento.ocurridoEn(), evento.detalle());
        try {
            s3.putObject(objeto -> objeto
                            .bucket(bucket)
                            .key(clave(evento))
                            .contentType("application/json")
                            .ifNoneMatch("*")
                            .objectLockMode(ObjectLockMode.COMPLIANCE)
                            .objectLockRetainUntilDate(evento.ocurridoEn().plus(retencion))
                            .checksumAlgorithm(ChecksumAlgorithm.SHA256),
                    RequestBody.fromString(json.writeValueAsString(registro), StandardCharsets.UTF_8));
            return true;
        } catch (S3Exception error) {
            if (error.statusCode() == 412) {
                return false;
            }
            throw error;
        }
    }

    @Override
    public List<EventoAcceso> porRecurso(String recurso) {
        return s3.listObjectsV2Paginator(listado -> listado.bucket(bucket).prefix(prefijo(recurso)))
                .contents().stream()
                .map(S3Object::key)
                .map(clave -> s3.getObjectAsBytes(lectura -> lectura.bucket(bucket).key(clave)).asUtf8String())
                .map(contenido -> json.readValue(contenido, RegistroJson.class))
                .map(registro -> new EventoAcceso(registro.id(), TipoAcceso.valueOf(registro.tipo()), registro.usuarioId(),
                        registro.recurso(), registro.motivo(), registro.ocurridoEn(), registro.detalle()))
                .toList();
    }
}
