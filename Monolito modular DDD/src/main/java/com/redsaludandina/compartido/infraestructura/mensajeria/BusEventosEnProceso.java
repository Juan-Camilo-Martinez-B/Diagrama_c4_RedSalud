package com.redsaludandina.compartido.infraestructura.mensajeria;

import com.redsaludandina.compartido.aplicacion.mensajeria.BusEventosPort;
import com.redsaludandina.compartido.aplicacion.mensajeria.EventoEntrante;
import com.redsaludandina.compartido.aplicacion.mensajeria.MensajeOutbox;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bus en proceso para el monolito sin AWS: entrega cada evento a todos los consumidores. Si
 * alguno falla, lanza una excepción para que el relevo deje el mensaje pendiente y lo reintente;
 * los consumidores que ya lo procesaron lo recibirán de nuevo y lo descartan por idempotencia.
 * En AWS lo reemplaza el adaptador de EventBridge.
 */
public final class BusEventosEnProceso implements BusEventosPort {

    private static final Logger LOG = LoggerFactory.getLogger(BusEventosEnProceso.class);

    private final List<ConsumidorEventos> consumidores;

    public BusEventosEnProceso(List<ConsumidorEventos> consumidores) {
        this.consumidores = List.copyOf(consumidores);
    }

    @Override
    public void publicar(MensajeOutbox mensaje) {
        EventoEntrante entrante = new EventoEntrante(mensaje.eventoId(), mensaje.evento());
        List<String> fallidos = new ArrayList<>();
        for (ConsumidorEventos consumidor : consumidores) {
            try {
                consumidor.accion().accept(entrante);
            } catch (RuntimeException error) {
                LOG.warn("El consumidor {} no pudo procesar {} {}: {}", consumidor.nombre(), mensaje.tipo(),
                        mensaje.eventoId(), error.getMessage());
                fallidos.add(consumidor.nombre());
            }
        }
        if (!fallidos.isEmpty()) {
            throw new IllegalStateException("Consumidores con falla para " + mensaje.eventoId() + ": " + fallidos);
        }
    }
}
