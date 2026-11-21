package com.redsaludandina.receta.aplicacion.puerto.salida;

import com.redsaludandina.receta.dominio.modelo.Receta;

/**
 * Envío de la receta firmada a la farmacia del centro (cola SQS). El envío es «al menos una vez»:
 * la farmacia debe descartar duplicados por el id de la receta.
 */
public interface FarmaciaPort {

    void enviar(Receta receta);
}
