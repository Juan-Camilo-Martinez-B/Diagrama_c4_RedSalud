package com.redsaludandina.legado.infraestructura.acl.cliente.bd;

import com.redsaludandina.legado.infraestructura.acl.modelo.HistorialLegado;
import com.redsaludandina.legado.infraestructura.acl.modelo.HistorialLegado.AtencionLegado;
import com.redsaludandina.legado.infraestructura.acl.modelo.HistorialLegado.DocumentoLegado;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Cliente de la base de SOLO LECTURA del legado (Caso §3). Solo hace SELECT y habla el modelo del
 * legado. Cada consulta tiene un límite de 6 s, el tope del caso para la consulta directa.
 */
public final class ClienteBdLectura {

    public static final int LIMITE_SEGUNDOS = 6;

    private final DataSource fuente;
    private final JdbcClient jdbc;

    public ClienteBdLectura(DataSource fuente) {
        this.fuente = fuente;
        JdbcTemplate plantilla = new JdbcTemplate(fuente);
        plantilla.setQueryTimeout(LIMITE_SEGUNDOS);
        this.jdbc = JdbcClient.create(plantilla);
    }

    public Optional<HistorialLegado> obtener(String codPaciente) {
        Optional<String> paciente = jdbc.sql("SELECT COD_PACIENTE FROM HC_PACIENTE WHERE COD_PACIENTE = :cod")
                .param("cod", codPaciente)
                .query(String.class)
                .optional();
        if (paciente.isEmpty()) {
            return Optional.empty();
        }
        List<AtencionLegado> atenciones = jdbc.sql("SELECT NRO_ATENCION, FEC_ATENCION, GLOSA FROM HC_ATENCION"
                        + " WHERE COD_PACIENTE = :cod")
                .param("cod", codPaciente)
                .query((fila, n) -> new AtencionLegado(fila.getString("NRO_ATENCION"), fila.getString("FEC_ATENCION"),
                        fila.getString("GLOSA")))
                .list();
        List<DocumentoLegado> documentos = jdbc.sql("SELECT COD_DOCUMENTO, TIP_DOCUMENTO, FEC_DOCUMENTO FROM HC_DOCUMENTO"
                        + " WHERE COD_PACIENTE = :cod")
                .param("cod", codPaciente)
                .query((fila, n) -> new DocumentoLegado(fila.getString("COD_DOCUMENTO"),
                        fila.getString("TIP_DOCUMENTO"), fila.getString("FEC_DOCUMENTO")))
                .list();
        return Optional.of(new HistorialLegado(paciente.get(), atenciones, documentos));
    }

    /** Cierra el pool de conexiones al legado al apagar la aplicación. */
    public void cerrar() throws Exception {
        if (fuente instanceof AutoCloseable cerrable) {
            cerrable.close();
        }
    }
}
