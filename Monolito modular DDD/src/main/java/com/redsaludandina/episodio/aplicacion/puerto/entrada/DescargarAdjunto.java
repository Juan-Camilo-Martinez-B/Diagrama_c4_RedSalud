package com.redsaludandina.episodio.aplicacion.puerto.entrada;

import com.redsaludandina.compartido.dominio.IdClinico;
import java.net.URI;
import java.time.Instant;

/**
 * Lee un examen o imagen: autoriza por rol y atributo, deja una LecturaClinica con el motivo y
 * devuelve una URL prefirmada de corta duración para descargarlo.
 */
public interface DescargarAdjunto {

    Descarga descargar(Consulta consulta);

    record Consulta(IdClinico episodioId, IdClinico adjuntoId, String usuarioId, String motivo) {
    }

    record Descarga(URI url, Instant expiraEn) {
    }
}
