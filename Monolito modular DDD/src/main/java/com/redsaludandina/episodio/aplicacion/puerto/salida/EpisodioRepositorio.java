package com.redsaludandina.episodio.aplicacion.puerto.salida;

import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.episodio.dominio.modelo.Episodio;
import java.util.Optional;

/** Episodios en Aurora PostgreSQL (esquema episodio); los binarios de adjuntos van a S3. */
public interface EpisodioRepositorio {

    Optional<Episodio> buscar(IdClinico episodioId);

    void guardar(Episodio episodio);
}
