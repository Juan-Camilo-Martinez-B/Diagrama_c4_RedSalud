package com.redsaludandina.sincronizacion.infraestructura.adaptador.salida.persistencia;

import com.redsaludandina.compartido.infraestructura.persistencia.Jdbc;
import com.redsaludandina.sincronizacion.aplicacion.puerto.entrada.SincronizarLote.ResultadoLote;
import com.redsaludandina.sincronizacion.aplicacion.puerto.salida.RegistroIdempotenciaPort;
import java.time.Clock;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tabla Idempotency-Key en sincronizacion.idempotencia: clave del lote, huella del contenido y la
 * respuesta R en JSON. Es una tabla, no memoria: sobrevive reinicios y la ven todas las instancias.
 */
public final class RegistroIdempotenciaJdbc implements RegistroIdempotenciaPort {

    private final JdbcClient jdbc;
    private final JsonMapper json;
    private final Clock reloj;

    public RegistroIdempotenciaJdbc(JdbcClient jdbc, JsonMapper json, Clock reloj) {
        this.jdbc = jdbc;
        this.json = json;
        this.reloj = reloj;
    }

    @Override
    public Optional<RespuestaGuardada> buscar(String clave) {
        return jdbc.sql("SELECT huella, respuesta FROM sincronizacion.idempotencia WHERE clave = :clave")
                .param("clave", clave)
                .query((fila, n) -> new RespuestaGuardada(fila.getString("huella").strip(),
                        json.readValue(fila.getString("respuesta"), ResultadoLote.class)))
                .optional();
    }

    /** Se ejecuta fuera de transacción: si otra instancia insertó la clave primero, gana la primera. */
    @Override
    public void guardar(String clave, String huella, ResultadoLote resultado) {
        try {
            jdbc.sql("INSERT INTO sincronizacion.idempotencia (clave, huella, respuesta, registrada_en)"
                            + " VALUES (:clave, :huella, :respuesta, :en)")
                    .param("clave", clave)
                    .param("huella", huella)
                    .param("respuesta", json.writeValueAsString(resultado))
                    .param("en", Jdbc.aFecha(reloj.instant()))
                    .update();
        } catch (DuplicateKeyException otraInstanciaGanoLaCarrera) {
            // La primera respuesta guardada es la que se devolverá en los reintentos.
        }
    }
}
