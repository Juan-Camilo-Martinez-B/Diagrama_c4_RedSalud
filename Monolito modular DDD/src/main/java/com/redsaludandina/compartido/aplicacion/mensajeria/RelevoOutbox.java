package com.redsaludandina.compartido.aplicacion.mensajeria;

import com.redsaludandina.compartido.dominio.Requisitos;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Relay del outbox: publica en el bus los mensajes pendientes de un contexto y los marca como
 * publicados. Lo invoca periódicamente la infraestructura (un planificador).
 *
 * <p>Conserva el orden por agregado: si falla la publicación de un mensaje, los siguientes del
 * MISMO agregado no se publican en esta vuelta (se reintentan en la próxima), pero los de otros
 * agregados sí. Si se cae entre publicar y marcar, el mensaje se publicará de nuevo: por eso los
 * consumidores toleran duplicados.
 */
public final class RelevoOutbox {

    private final OutboxPort outbox;
    private final BusEventosPort bus;
    private final Clock reloj;

    public RelevoOutbox(OutboxPort outbox, BusEventosPort bus, Clock reloj) {
        this.outbox = Requisitos.noNulo(outbox, "outbox");
        this.bus = Requisitos.noNulo(bus, "bus");
        this.reloj = Requisitos.noNulo(reloj, "reloj");
    }

    public ResultadoRelevo relevar(int limite) {
        if (limite <= 0) {
            throw new IllegalArgumentException("El límite del relevo debe ser positivo");
        }
        List<MensajeOutbox> pendientes = outbox.pendientes(limite);
        Set<String> agregadosBloqueados = new HashSet<>();
        int publicados = 0;
        int fallidos = 0;
        int diferidos = 0;
        for (MensajeOutbox mensaje : pendientes) {
            if (agregadosBloqueados.contains(mensaje.agregadoId())) {
                diferidos++;
                continue;
            }
            try {
                bus.publicar(mensaje);
                outbox.marcarPublicado(mensaje.eventoId(), reloj.instant());
                publicados++;
            } catch (RuntimeException error) {
                agregadosBloqueados.add(mensaje.agregadoId());
                fallidos++;
            }
        }
        return new ResultadoRelevo(publicados, fallidos, diferidos);
    }

    /** Cuántos se publicaron, cuántos fallaron y cuántos se dejaron para conservar el orden. */
    public record ResultadoRelevo(int publicados, int fallidos, int diferidos) {
    }
}
