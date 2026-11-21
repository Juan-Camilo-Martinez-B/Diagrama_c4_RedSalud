package com.redsaludandina.auditoria.aplicacion.puerto.salida;

import com.redsaludandina.auditoria.dominio.modelo.EventoAcceso;
import java.util.List;

/**
 * Almacén append-only de accesos (S3 Object Lock en modo compliance, ADR-04). No ofrece
 * modificar ni borrar.
 */
public interface RegistroAccesosPort {

    /** Guarda el registro si su id no existe todavía; devuelve {@code false} si ya estaba. */
    boolean agregarSiNoExiste(EventoAcceso evento);

    List<EventoAcceso> porRecurso(String recurso);
}
