package com.redsaludandina.interconsulta.dominio.modelo;

/** Ciclo de vida de la interconsulta (Caso §2). Solo avanza, de a un paso. */
public enum EstadoInterconsulta {
    SOLICITADA,
    AGENDADA,
    ATENDIDA,
    CERRADA;

    public boolean puedeAvanzarA(EstadoInterconsulta destino) {
        return destino.ordinal() == ordinal() + 1;
    }
}
