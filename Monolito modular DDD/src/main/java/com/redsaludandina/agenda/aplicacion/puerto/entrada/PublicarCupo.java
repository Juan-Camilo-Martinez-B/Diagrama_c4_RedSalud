package com.redsaludandina.agenda.aplicacion.puerto.entrada;

import com.redsaludandina.compartido.dominio.IdClinico;
import java.time.Instant;

/** Publica un cupo de un especialista. Publicar dos veces el mismo id no lo duplica. */
public interface PublicarCupo {

    void publicar(Comando comando);

    record Comando(IdClinico cupoId, String especialistaId, Instant inicio, Instant fin) {
    }
}
