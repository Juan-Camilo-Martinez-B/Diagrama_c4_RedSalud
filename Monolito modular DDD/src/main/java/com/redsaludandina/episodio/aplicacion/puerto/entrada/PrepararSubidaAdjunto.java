package com.redsaludandina.episodio.aplicacion.puerto.entrada;

import com.redsaludandina.compartido.dominio.IdClinico;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Paso 1 para adjuntar un examen o imagen: autoriza al usuario sobre el episodio y devuelve una
 * URL prefirmada para subir el binario. Paso 2: el cliente sube y registra el adjunto con
 * {@link AdjuntarArchivo}, usando la clave que devuelve este paso.
 */
public interface PrepararSubidaAdjunto {

    Subida preparar(Comando comando);

    record Comando(IdClinico episodioId, IdClinico adjuntoId, String sha256, String usuarioId) {
    }

    record Subida(URI url, Map<String, List<String>> encabezados, String claveAlmacenamiento, Instant expiraEn) {
    }
}
