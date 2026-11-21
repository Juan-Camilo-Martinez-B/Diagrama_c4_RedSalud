package com.redsaludandina.shared.application.messaging;

/**
 * El evento llegó antes que otro del que depende. El adaptador consumidor debe reintentarlo más tarde
 * en vez de descartarlo.
 */
public class EventOutOfOrderException extends RuntimeException {

    public EventOutOfOrderException(String message) {
        super(message);
    }
}
