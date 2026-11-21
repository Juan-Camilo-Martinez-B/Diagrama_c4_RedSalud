package com.redsaludandina.auditoria.infraestructura.adaptador.salida.s3;

import static org.assertj.core.api.Assertions.assertThat;

import com.redsaludandina.auditoria.dominio.modelo.EventoAcceso;
import com.redsaludandina.auditoria.dominio.modelo.ReporteAccesos;
import com.redsaludandina.compartido.aplicacion.Fijos;
import com.redsaludandina.compartido.evento.LecturaClinica;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectLockMode;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import tools.jackson.databind.json.JsonMapper;

/** Auditoría en S3 Object Lock con un S3 falso que respeta If-None-Match (sin red). */
class RegistroAccesosS3ObjectLockTest {

    /** S3 falso: guarda objetos y responde 412 si la clave ya existe y se pidió If-None-Match: *. */
    private static final class S3DePrueba implements S3Client {
        final Map<String, byte[]> objetos = new LinkedHashMap<>();
        final List<PutObjectRequest> escrituras = new ArrayList<>();

        @Override
        public PutObjectResponse putObject(PutObjectRequest solicitud, RequestBody cuerpo) {
            escrituras.add(solicitud);
            if ("*".equals(solicitud.ifNoneMatch()) && objetos.containsKey(solicitud.key())) {
                throw (S3Exception) S3Exception.builder().statusCode(412).message("PreconditionFailed").build();
            }
            try {
                objetos.put(solicitud.key(), cuerpo.contentStreamProvider().newStream().readAllBytes());
            } catch (IOException error) {
                throw new UncheckedIOException(error);
            }
            return PutObjectResponse.builder().build();
        }

        @Override
        public ListObjectsV2Response listObjectsV2(ListObjectsV2Request solicitud) {
            return ListObjectsV2Response.builder()
                    .contents(objetos.keySet().stream().filter(clave -> clave.startsWith(solicitud.prefix()))
                            .map(clave -> S3Object.builder().key(clave).build()).toList())
                    .isTruncated(false)
                    .build();
        }

        @Override
        public ResponseBytes<GetObjectResponse> getObjectAsBytes(GetObjectRequest solicitud) {
            return ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), objetos.get(solicitud.key()));
        }

        @Override
        public String serviceName() {
            return "s3";
        }

        @Override
        public void close() {
        }
    }

    private final S3DePrueba s3 = new S3DePrueba();
    private final RegistroAccesosS3ObjectLock registro = new RegistroAccesosS3ObjectLock(s3, "redsalud-auditoria",
            Duration.ofDays(3650), JsonMapper.builder().build());

    private static EventoAcceso lectura(int n, String usuario) {
        return EventoAcceso.desde(new LecturaClinica(usuario, "historial/pac-1", "Control", Fijos.AHORA.plusSeconds(n)),
                new UUID(5, n));
    }

    @Test
    void escribeEnModoComplianceConRetencionYSinPisarNada() {
        assertThat(registro.agregarSiNoExiste(lectura(1, "esp-1"))).isTrue();

        PutObjectRequest escritura = s3.escrituras.get(0);
        assertThat(escritura.bucket()).isEqualTo("redsalud-auditoria");
        assertThat(escritura.objectLockMode()).isEqualTo(ObjectLockMode.COMPLIANCE);
        assertThat(escritura.objectLockRetainUntilDate()).isEqualTo(Fijos.AHORA.plusSeconds(1).plus(Duration.ofDays(3650)));
        assertThat(escritura.ifNoneMatch()).isEqualTo("*");
        assertThat(escritura.key()).startsWith("accesos/historial%2Fpac-1/");
    }

    @Test
    void unEventoEntregadoDosVecesSeGuardaUnaSolaVez() {
        assertThat(registro.agregarSiNoExiste(lectura(1, "esp-1"))).isTrue();
        assertThat(registro.agregarSiNoExiste(lectura(1, "esp-1"))).isFalse();

        assertThat(s3.objetos).hasSize(1);
    }

    @Test
    void elReporteQuienVioSeArmaDesdeS3() {
        registro.agregarSiNoExiste(lectura(2, "esp-2"));
        registro.agregarSiNoExiste(lectura(1, "esp-1"));

        assertThat(ReporteAccesos.quienVio("historial/pac-1", registro.porRecurso("historial/pac-1")))
                .extracting(EventoAcceso::usuarioId).containsExactly("esp-1", "esp-2");
    }
}
