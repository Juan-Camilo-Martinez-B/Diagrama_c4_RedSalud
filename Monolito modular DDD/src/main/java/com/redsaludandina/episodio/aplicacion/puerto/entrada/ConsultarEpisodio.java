package com.redsaludandina.episodio.aplicacion.puerto.entrada;

import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.episodio.dominio.modelo.Episodio;

/**
 * Lee un episodio. Autoriza por rol y atributo y deja una LecturaClinica con el motivo; sin
 * motivo no hay lectura (Caso §5).
 */
public interface ConsultarEpisodio {

    Episodio consultar(Consulta consulta);

    record Consulta(IdClinico episodioId, String usuarioId, String motivo) {
    }
}
