package com.redsaludandina.compartido.aplicacion.mensajeria;

/**
 * El evento llegó antes que otro del que depende (el bus no garantiza orden). El adaptador
 * consumidor debe reintentarlo más tarde en vez de descartarlo.
 */
public class EventoFueraDeOrdenException extends RuntimeException {

    public EventoFueraDeOrdenException(String mensaje) {
        super(mensaje);
    }
}
