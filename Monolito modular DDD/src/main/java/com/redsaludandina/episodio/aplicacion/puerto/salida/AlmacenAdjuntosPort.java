package com.redsaludandina.episodio.aplicacion.puerto.salida;

import com.redsaludandina.compartido.dominio.IdClinico;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Almacén de los binarios de adjuntos (S3 cifrado con KMS, sin réplicas fuera de la región). El
 * binario no pasa por el monolito: se sube y se baja con URLs prefirmadas de corta duración.
 */
public interface AlmacenAdjuntosPort {

    /** La ÚNICA clave válida para un adjunto: la define el servidor, no el cliente. */
    String claveDe(IdClinico episodioId, IdClinico adjuntoId);

    /** URL para subir el binario; el almacén verifica que su SHA-256 coincida con {@code sha256}. */
    SubidaPrefirmada prepararSubida(IdClinico episodioId, IdClinico adjuntoId, String sha256);

    DescargaPrefirmada prepararDescarga(String claveAlmacenamiento);

    /** {@code encabezados}: los que el cliente DEBE enviar tal cual en el PUT (cifrado, checksum). */
    record SubidaPrefirmada(URI url, Map<String, List<String>> encabezados, String claveAlmacenamiento,
                            Instant expiraEn) {
    }

    record DescargaPrefirmada(URI url, Instant expiraEn) {
    }
}
