package com.redsaludandina.compartido.infraestructura.persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.regex.Pattern;

/**
 * Conversiones JDBC comunes. Los instantes viajan como {@link OffsetDateTime} en UTC, el tipo que
 * PostgreSQL y H2 aceptan para TIMESTAMP WITH TIME ZONE.
 */
public final class Jdbc {

    private static final Pattern IDENTIFICADOR = Pattern.compile("^[a-z_]+$");

    private Jdbc() {
    }

    public static OffsetDateTime aFecha(Instant instante) {
        return instante == null ? null : instante.atOffset(ZoneOffset.UTC);
    }

    public static Instant instante(ResultSet fila, String columna) throws SQLException {
        OffsetDateTime valor = fila.getObject(columna, OffsetDateTime.class);
        return valor == null ? null : valor.toInstant();
    }

    /** Valida un nombre de esquema o tabla antes de concatenarlo en SQL. */
    public static String identificador(String nombre) {
        if (nombre == null || !IDENTIFICADOR.matcher(nombre).matches()) {
            throw new IllegalArgumentException("Identificador SQL inválido: " + nombre);
        }
        return nombre;
    }
}
