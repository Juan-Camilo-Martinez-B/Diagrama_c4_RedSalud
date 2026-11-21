package com.redsaludandina.episodio.infraestructura.adaptador.salida.almacenamiento;

import static com.redsaludandina.compartido.dominio.IdsDePrueba.id;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.redsaludandina.episodio.aplicacion.puerto.salida.AlmacenAdjuntosPort.DescargaPrefirmada;
import com.redsaludandina.episodio.aplicacion.puerto.salida.AlmacenAdjuntosPort.SubidaPrefirmada;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/** Firma real de URLs de S3 (es un cálculo local: no hace falta red ni cuenta de AWS). */
class AlmacenAdjuntosS3Test {

    private final S3Presigner presigner = S3Presigner.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("prueba", "prueba")))
            .build();
    private final AlmacenAdjuntosS3 almacen = new AlmacenAdjuntosS3(presigner, "redsalud-adjuntos", "alias/adjuntos",
            Duration.ofMinutes(10));

    @AfterEach
    void cerrar() {
        presigner.close();
    }

    @Test
    void laSubidaExigeCifradoKmsDelContextoYElChecksumDeclarado() {
        SubidaPrefirmada subida = almacen.prepararSubida(id(1), id(2), "a".repeat(64));

        assertThat(subida.claveAlmacenamiento()).isEqualTo("episodios/" + id(1) + "/adjuntos/" + id(2));
        assertThat(subida.url().toString()).contains("redsalud-adjuntos").contains("X-Amz-Signature");
        assertThat(subida.encabezados()).containsKey("x-amz-server-side-encryption");
        assertThat(subida.encabezados().get("x-amz-server-side-encryption")).containsExactly("aws:kms");
        assertThat(subida.encabezados().get("x-amz-server-side-encryption-aws-kms-key-id")).containsExactly("alias/adjuntos");
        assertThat(subida.encabezados()).containsKey("x-amz-checksum-sha256");
    }

    @Test
    void unaHuellaQueNoEsSha256SeRechaza() {
        assertThatThrownBy(() -> almacen.prepararSubida(id(1), id(2), "no-es-sha"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void laDescargaEsUnaUrlFirmadaDeCortaDuracion() {
        DescargaPrefirmada descarga = almacen.prepararDescarga(almacen.claveDe(id(1), id(2)));

        assertThat(descarga.url().toString()).contains("X-Amz-Expires=600").contains("X-Amz-Signature");
    }
}
