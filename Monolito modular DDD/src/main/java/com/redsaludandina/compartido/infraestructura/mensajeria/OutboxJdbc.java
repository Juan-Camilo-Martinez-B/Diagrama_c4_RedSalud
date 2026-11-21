package com.redsaludandina.compartido.infraestructura.mensajeria;

import com.redsaludandina.compartido.aplicacion.mensajeria.MensajeOutbox;
import com.redsaludandina.compartido.aplicacion.mensajeria.OutboxPort;
import com.redsaludandina.compartido.infraestructura.persistencia.Jdbc;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Outbox de un contexto en su propio esquema ({@code <esquema>.outbox}). {@link #agregar} corre
 * dentro de la transacción del caso de uso, así que el evento se confirma junto con el agregado.
 */
public final class OutboxJdbc implements OutboxPort {

    private final JdbcClient jdbc;
    private final SerializadorEventos serializador;
    private final String tabla;

    public OutboxJdbc(JdbcClient jdbc, String esquema, SerializadorEventos serializador) {
        this.jdbc = jdbc;
        this.serializador = serializador;
        this.tabla = Jdbc.identificador(esquema) + ".outbox";
    }

    @Override
    public void agregar(List<MensajeOutbox> mensajes) {
        for (MensajeOutbox mensaje : mensajes) {
            jdbc.sql("INSERT INTO " + tabla
                            + " (evento_id, contexto, agregado_id, tipo, payload, registrado_en)"
                            + " VALUES (:eventoId, :contexto, :agregadoId, :tipo, :payload, :registradoEn)")
                    .param("eventoId", mensaje.eventoId())
                    .param("contexto", mensaje.contexto())
                    .param("agregadoId", mensaje.agregadoId())
                    .param("tipo", serializador.tipo(mensaje.evento()))
                    .param("payload", serializador.aJson(mensaje.evento()))
                    .param("registradoEn", Jdbc.aFecha(mensaje.registradoEn()))
                    .update();
        }
    }

    @Override
    public List<MensajeOutbox> pendientes(int limite) {
        return jdbc.sql("SELECT evento_id, contexto, agregado_id, tipo, payload, registrado_en FROM " + tabla
                        + " WHERE publicado_en IS NULL ORDER BY secuencia LIMIT :limite")
                .param("limite", limite)
                .query((fila, numero) -> new MensajeOutbox(
                        fila.getObject("evento_id", UUID.class),
                        fila.getString("contexto"),
                        fila.getString("agregado_id"),
                        serializador.desdeJson(fila.getString("tipo"), fila.getString("payload")),
                        Jdbc.instante(fila, "registrado_en")))
                .list();
    }

    @Override
    public void marcarPublicado(UUID eventoId, Instant publicadoEn) {
        jdbc.sql("UPDATE " + tabla + " SET publicado_en = :publicadoEn WHERE evento_id = :eventoId")
                .param("publicadoEn", Jdbc.aFecha(publicadoEn))
                .param("eventoId", eventoId)
                .update();
    }
}
