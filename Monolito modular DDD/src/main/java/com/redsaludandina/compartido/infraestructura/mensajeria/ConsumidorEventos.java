package com.redsaludandina.compartido.infraestructura.mensajeria;

import com.redsaludandina.compartido.aplicacion.mensajeria.EventoEntrante;
import java.util.function.Consumer;

/** Un consumidor del bus: el caso de uso de un contexto que reacciona a eventos del dominio. */
public record ConsumidorEventos(String nombre, Consumer<EventoEntrante> accion) {
}
