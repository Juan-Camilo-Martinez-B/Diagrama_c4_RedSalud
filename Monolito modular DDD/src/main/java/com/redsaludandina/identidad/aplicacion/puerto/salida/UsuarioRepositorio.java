package com.redsaludandina.identidad.aplicacion.puerto.salida;

import com.redsaludandina.identidad.dominio.modelo.Usuario;
import java.util.Optional;

/** Perfiles y atributos de acceso (DynamoDB); la identidad del token la valida Cognito. */
public interface UsuarioRepositorio {

    Optional<Usuario> buscar(String usuarioId);
}
