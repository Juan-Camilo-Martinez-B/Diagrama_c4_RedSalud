package com.redsaludandina.receta.aplicacion.puerto.entrada;

import com.redsaludandina.compartido.aplicacion.mensajeria.EventoEntrante;

/**
 * Política «Enviar a farmacia»: consume RecetaFirmada desde el bus y envía la receta a la
 * farmacia del centro. Ignora cualquier otro evento.
 */
public interface EnviarRecetaAFarmacia {

    void procesar(EventoEntrante entrante);
}
