package com.redsaludandina.analitica.aplicacion.puerto.entrada;

import com.redsaludandina.compartido.aplicacion.mensajeria.EventoEntrante;

/**
 * Política «Proyectar indicador» (conformista con el lenguaje publicado). Un evento repetido no
 * se cuenta dos veces.
 */
public interface ProyectarIndicadores {

    void procesar(EventoEntrante entrante);
}
