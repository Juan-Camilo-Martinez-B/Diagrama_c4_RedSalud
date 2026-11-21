package com.redsaludandina.interconsulta.dominio.modelo;

/** Qué le falta a la interconsulta para avanzar. Hay una alerta abierta por estado no final. */
public enum TipoAlerta {
    PENDIENTE_AGENDAR,
    PENDIENTE_ATENCION,
    PENDIENTE_CIERRE;

    static TipoAlerta para(EstadoInterconsulta estado) {
        switch (estado) {
            case SOLICITADA:
                return PENDIENTE_AGENDAR;
            case AGENDADA:
                return PENDIENTE_ATENCION;
            case ATENDIDA:
                return PENDIENTE_CIERRE;
            default:
                return null;
        }
    }
}
