package com.redsaludandina.sincronizacion.aplicacion.puerto.salida;

import com.redsaludandina.sincronizacion.aplicacion.puerto.entrada.SincronizarLote.ResultadoLote;
import java.util.Optional;

/**
 * Tabla Idempotency-Key (esquema sincronizacion), PERSISTIDA: sobrevive reinicios y la comparten
 * todas las instancias detrás del balanceador. Por cada clave guarda la huella del lote y la
 * respuesta R que se devolvió.
 */
public interface RegistroIdempotenciaPort {

    Optional<RespuestaGuardada> buscar(String clave);

    /** Si otra instancia guardó la clave primero, gana la primera y esta llamada no hace nada. */
    void guardar(String clave, String huella, ResultadoLote resultado);

    record RespuestaGuardada(String huella, ResultadoLote resultado) {
    }
}
