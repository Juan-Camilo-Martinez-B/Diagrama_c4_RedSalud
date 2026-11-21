package com.redsaludandina.episodio.infraestructura.adaptador.salida.almacenamiento;

import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.episodio.aplicacion.puerto.salida.AlmacenAdjuntosPort;
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
 * Adjuntos en S3 cifrados con una clave KMS propia del contexto Episodio (Caso §5: claves por
 * contexto). El monolito solo firma URLs de corta duración:
 *
 * <ul>
 *   <li>Subida: exige cifrado SSE-KMS con la clave del contexto y el checksum SHA-256 declarado;
 *       S3 rechaza el PUT si el binario no coincide.</li>
 *   <li>Descarga: solo se firma después de autorizar y auditar la lectura.</li>
 * </ul>
 *
 * <p>La política del bucket (sin replicación fuera de la región, solo SSE-KMS) se define en la
 * infraestructura como código.
 */
public final class AlmacenAdjuntosS3 implements AlmacenAdjuntosPort {

    private static final Pattern SHA_256 = Pattern.compile("^[0-9a-f]{64}$");

    private final S3Presigner presigner;
    private final String bucket;
    private final String claveKms;
    private final Duration vigencia;

    public AlmacenAdjuntosS3(S3Presigner presigner, String bucket, String claveKms, Duration vigencia) {
        this.presigner = presigner;
        this.bucket = bucket;
        this.claveKms = claveKms;
        this.vigencia = vigencia;
    }

    @Override
    public String claveDe(IdClinico episodioId, IdClinico adjuntoId) {
        return "episodios/" + episodioId.valor() + "/adjuntos/" + adjuntoId.valor();
    }

    @Override
    public SubidaPrefirmada prepararSubida(IdClinico episodioId, IdClinico adjuntoId, String sha256) {
        String huella = sha256.strip().toLowerCase(Locale.ROOT);
        if (!SHA_256.matcher(huella).matches()) {
            throw new IllegalArgumentException("La huella del adjunto debe ser SHA-256 en hexadecimal");
        }
        String clave = claveDe(episodioId, adjuntoId);
        String checksum = Base64.getEncoder().encodeToString(HexFormat.of().parseHex(huella));
        PresignedPutObjectRequest firmada = presigner.presignPutObject(solicitud -> solicitud
                .signatureDuration(vigencia)
                .putObjectRequest(objeto -> objeto
                        .bucket(bucket)
                        .key(clave)
                        .serverSideEncryption(ServerSideEncryption.AWS_KMS)
                        .ssekmsKeyId(claveKms)
                        .checksumSHA256(checksum)));
        return new SubidaPrefirmada(uri(firmada.url()), firmada.signedHeaders(), clave, firmada.expiration());
    }

    @Override
    public DescargaPrefirmada prepararDescarga(String claveAlmacenamiento) {
        PresignedGetObjectRequest firmada = presigner.presignGetObject(solicitud -> solicitud
                .signatureDuration(vigencia)
                .getObjectRequest(objeto -> objeto.bucket(bucket).key(claveAlmacenamiento)));
        return new DescargaPrefirmada(uri(firmada.url()), firmada.expiration());
    }

    private static URI uri(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException imposible) {
            throw new IllegalStateException("URL prefirmada inválida", imposible);
        }
    }
}
