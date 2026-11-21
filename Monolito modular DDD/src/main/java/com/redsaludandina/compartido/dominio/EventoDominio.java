package com.redsaludandina.compartido.dominio;

import java.time.Instant;

/** Hecho del dominio que ya ocurrió y que otros contextos pueden consumir. */
public interface EventoDominio {

    Instant ocurridoEn();
}
