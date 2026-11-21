package com.redsaludandina.compartido.infraestructura.mensajeria;

import com.redsaludandina.compartido.aplicacion.mensajeria.InboxPort;
import com.redsaludandina.compartido.infraestructura.persistencia.Jdbc;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Inbox de un contexto ({@code <esquema>.inbox}). Consulta antes de insertar para no provocar un
 * error de clave duplicada, que en PostgreSQL invalidaría la transacción en curso. Si dos entregas
 * simultáneas compiten, una falla por la clave primaria, su transacción se deshace y el bus la
 * reintenta: en el reintento ya figura como vista.
 */
public final class InboxJdbc implements InboxPort {

    private final JdbcClient jdbc;
    private final String tabla;

    public InboxJdbc(JdbcClient jdbc, String esquema) {
        this.jdbc = jdbc;
        this.tabla = Jdbc.identificador(esquema) + ".inbox";
    }

    @Override
    public boolean registrarSiEsNuevo(String consumidor, UUID eventoId) {
        Integer vistos = jdbc.sql("SELECT COUNT(*) FROM " + tabla + " WHERE consumidor = :consumidor AND evento_id = :eventoId")
                .param("consumidor", consumidor)
                .param("eventoId", eventoId)
                .query(Integer.class)
                .single();
        if (vistos > 0) {
            return false;
        }
        jdbc.sql("INSERT INTO " + tabla + " (consumidor, evento_id) VALUES (:consumidor, :eventoId)")
                .param("consumidor", consumidor)
                .param("eventoId", eventoId)
                .update();
        return true;
    }
}
