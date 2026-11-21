package com.redsaludandina.identidad.infraestructura.adaptador.salida.persistencia;

import com.redsaludandina.identidad.aplicacion.puerto.salida.UsuarioRepositorio;
import com.redsaludandina.identidad.dominio.modelo.AtributosAcceso;
import com.redsaludandina.identidad.dominio.modelo.Rol;
import com.redsaludandina.identidad.dominio.modelo.Usuario;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Perfiles y atributos de acceso en el esquema identidad (en AWS: tabla de perfiles en DynamoDB). */
public final class UsuarioRepositorioJdbc implements UsuarioRepositorio {

    private final JdbcClient jdbc;

    public UsuarioRepositorioJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Usuario> buscar(String usuarioId) {
        Set<String> pacientes = new HashSet<>(jdbc.sql("SELECT paciente_id FROM identidad.paciente_relacionado"
                        + " WHERE usuario_id = :id")
                .param("id", usuarioId)
                .query(String.class)
                .list());
        return jdbc.sql("SELECT rol, centro_id, especialidad FROM identidad.usuario WHERE id = :id")
                .param("id", usuarioId)
                .query((fila, n) -> new Usuario(usuarioId, Rol.valueOf(fila.getString("rol")),
                        new AtributosAcceso(fila.getString("centro_id"), fila.getString("especialidad"), pacientes)))
                .optional();
    }
}
