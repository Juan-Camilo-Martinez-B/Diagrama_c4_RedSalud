package com.redsaludandina.receta.aplicacion.puerto.salida;

import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.receta.dominio.modelo.Receta;
import java.util.Optional;

/** Recetas en Aurora PostgreSQL (esquema receta). */
public interface RecetaRepositorio {

    Optional<Receta> buscar(IdClinico recetaId);

    void guardar(Receta receta);
}
