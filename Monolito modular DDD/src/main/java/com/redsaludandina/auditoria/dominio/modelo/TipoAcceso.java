package com.redsaludandina.auditoria.dominio.modelo;

public enum TipoAcceso {
    /** Alguien leyó historia clínica. */
    LECTURA_CLINICA,
    /** Last-writer-wins dejó un valor clínico fuera; se guardan ambos valores. */
    VALOR_CLINICO_DESCARTADO
}
