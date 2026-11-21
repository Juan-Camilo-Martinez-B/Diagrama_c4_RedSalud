package com.redsaludandina.identidad.dominio.modelo;

import com.redsaludandina.compartido.dominio.Requisitos;

/** Resultado de evaluar un acceso, con la razón para poder auditarlo y explicarlo. */
public record DecisionAcceso(boolean permitido, String razon) {

    public DecisionAcceso {
        razon = Requisitos.texto(razon, "razon");
    }

    public static DecisionAcceso permitir(String razon) {
        return new DecisionAcceso(true, razon);
    }

    public static DecisionAcceso denegar(String razon) {
        return new DecisionAcceso(false, razon);
    }
}
