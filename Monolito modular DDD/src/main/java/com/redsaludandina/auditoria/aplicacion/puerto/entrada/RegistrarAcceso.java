package com.redsaludandina.auditoria.aplicacion.puerto.entrada;

import com.redsaludandina.compartido.aplicacion.mensajeria.EventoEntrante;

/**
 * Política «Auditar acceso» (conformista con el lenguaje publicado): guarda cada LecturaClinica y
 * cada ValorClinicoDescartado. Un evento repetido no se registra dos veces.
 */
public interface RegistrarAcceso {

    void procesar(EventoEntrante entrante);
}
