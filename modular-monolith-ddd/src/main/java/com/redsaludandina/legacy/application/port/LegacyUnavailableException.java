package com.redsaludandina.legacy.application.port;

/** El legado no respondió (caído o más lento que el límite de 6 s del caso). */
public class LegacyUnavailableException extends RuntimeException {

    public LegacyUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
