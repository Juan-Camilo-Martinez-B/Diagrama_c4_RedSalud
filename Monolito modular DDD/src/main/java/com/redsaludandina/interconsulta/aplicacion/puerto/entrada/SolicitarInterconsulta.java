package com.redsaludandina.interconsulta.aplicacion.puerto.entrada;

import com.redsaludandina.compartido.dominio.IdClinico;

/** El médico rural pide una interconsulta. Repetir el mismo id no la duplica. */
public interface SolicitarInterconsulta {

    void solicitar(Comando comando);

    record Comando(IdClinico interconsultaId, IdClinico episodioOrigenId, String pacienteId, String centroId,
                   String especialidad, String motivo) {
    }
}
