package com.redsaludandina.sincronizacion.aplicacion.puerto.entrada;

import java.time.Instant;
import java.util.List;

/**
 * OHS de sincronización offline (POST /sync/episodios). Un centro rural envía un LOTE de su cola
 * con una {@code Idempotency-Key} propia del lote:
 *
 * <ul>
 *   <li>Primer intento: se aplica cada cambio y, si todos quedaron resueltos, se persiste la clave
 *       con la respuesta R ({@link Respuesta#repetida()} = false → 201).</li>
 *   <li>Reintento con la misma clave (corte de red, otra instancia, reinicio): se devuelve la MISMA
 *       respuesta R sin aplicar nada ({@code repetida} = true → 200).</li>
 *   <li>Cupos y recetas firmadas no se resuelven en el borde: se rechazan y el centro reintenta
 *       contra el servidor.</li>
 * </ul>
 */
public interface SincronizarLote {

    Respuesta sincronizar(String idempotencyKey, Lote lote);

    record Lote(String centroId, List<Item> items) {
    }

    /**
     * {@code cambioId} identifica el cambio en la respuesta; {@code secuencia} ordena los cambios de
     * un mismo agregado tal como ocurrieron en el centro.
     */
    record Item(String cambioId, String agregadoId, long secuencia, CambioOffline cambio) {
    }

    /** Cambios que llegan desde el borde. */
    sealed interface CambioOffline {
    }

    /** {@code cupoId} es nulo si la atención no viene de la agenda. */
    record AperturaEpisodio(String episodioId, String pacienteId, String cupoId) implements CambioOffline {
    }

    record RegistroAsistencia(String episodioId, String campo, String texto, Instant selloInstante)
            implements CambioOffline {
    }

    record AdjuntoArchivo(String episodioId, String adjuntoId, String tipo, String claveAlmacenamiento,
                           String sha256) implements CambioOffline {
    }

    record CierreTeleconsulta(String episodioId) implements CambioOffline {
    }

    /** No se resuelve en el borde: siempre RECHAZADO con REQUIERE_SERVIDOR. */
    record ReservaCupo(String cupoId, String reservaId, String pacienteId) implements CambioOffline {
    }

    /** No se resuelve en el borde: siempre RECHAZADO con REQUIERE_SERVIDOR. */
    record FirmaReceta(String recetaId) implements CambioOffline {
    }

    enum Estado {
        /** Se aplicó en este intento. */
        APLICADO,
        /** La restricción única mostró que otro intento ya lo había aplicado. */
        YA_APLICADO,
        /** Una regla lo rechazó (p. ej. REQUIERE_SERVIDOR): reintentar contra el servidor. */
        RECHAZADO,
        /** No se pudo aplicar todavía (falla transitoria u orden): reenviar el lote. */
        PENDIENTE
    }

    record ResultadoItem(String cambioId, Estado estado, String codigo, String mensaje) {
    }

    record ResultadoLote(List<ResultadoItem> items) {

        /** Ningún ítem quedó pendiente: la respuesta ya no cambia y se puede guardar con la clave. */
        public boolean definitivo() {
            return items.stream().noneMatch(item -> item.estado() == Estado.PENDIENTE);
        }
    }

    /** La respuesta R y si es la repetición de una ya guardada para la misma clave. */
    record Respuesta(ResultadoLote resultado, boolean repetida) {
    }
}
