package com.redsaludandina.agenda.infraestructura.adaptador.salida.persistencia;

import com.redsaludandina.agenda.aplicacion.puerto.salida.CupoRepositorio;
import com.redsaludandina.agenda.dominio.modelo.Cupo;
import com.redsaludandina.agenda.dominio.modelo.Franja;
import com.redsaludandina.agenda.dominio.modelo.Reserva;
import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.compartido.infraestructura.persistencia.Jdbc;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Cupos en el esquema agenda. {@link #buscar} BLOQUEA la fila del cupo ({@code FOR UPDATE}) hasta
 * que termine la transacción: dos reservas simultáneas del mismo cupo se serializan y la segunda
 * ve la primera, así que la regla «sin doble reserva» se cumple también con concurrencia. La
 * clave primaria de agenda.reserva lo garantiza además en la base.
 */
public final class CupoRepositorioJdbc implements CupoRepositorio {

    private final JdbcClient jdbc;

    public CupoRepositorioJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Cupo> buscar(IdClinico cupoId) {
        Optional<Reserva> reserva = jdbc.sql("SELECT reserva_id, paciente_id, centro_id, reservada_en"
                        + " FROM agenda.reserva WHERE cupo_id = :id")
                .param("id", cupoId.valor())
                .query((fila, n) -> new Reserva(IdClinico.de(fila.getString("reserva_id")),
                        fila.getString("paciente_id"), fila.getString("centro_id"), Jdbc.instante(fila, "reservada_en")))
                .optional();
        return jdbc.sql("SELECT id, especialista_id, inicio, fin FROM agenda.cupo WHERE id = :id FOR UPDATE")
                .param("id", cupoId.valor())
                .query((fila, n) -> Cupo.reconstituir(cupoId, fila.getString("especialista_id"),
                        new Franja(Jdbc.instante(fila, "inicio"), Jdbc.instante(fila, "fin")), reserva.orElse(null)))
                .optional();
    }

    @Override
    public void guardar(Cupo cupo) {
        int actualizados = jdbc.sql("UPDATE agenda.cupo SET especialista_id = :especialista, inicio = :inicio,"
                        + " fin = :fin WHERE id = :id")
                .param("especialista", cupo.especialistaId())
                .param("inicio", Jdbc.aFecha(cupo.franja().inicio()))
                .param("fin", Jdbc.aFecha(cupo.franja().fin()))
                .param("id", cupo.id().valor())
                .update();
        if (actualizados == 0) {
            jdbc.sql("INSERT INTO agenda.cupo (id, especialista_id, inicio, fin) VALUES (:id, :especialista, :inicio, :fin)")
                    .param("id", cupo.id().valor())
                    .param("especialista", cupo.especialistaId())
                    .param("inicio", Jdbc.aFecha(cupo.franja().inicio()))
                    .param("fin", Jdbc.aFecha(cupo.franja().fin()))
                    .update();
        }
        cupo.reserva().ifPresent(reserva -> {
            Integer existe = jdbc.sql("SELECT COUNT(*) FROM agenda.reserva WHERE cupo_id = :id")
                    .param("id", cupo.id().valor()).query(Integer.class).single();
            if (existe == 0) {
                jdbc.sql("INSERT INTO agenda.reserva (cupo_id, reserva_id, paciente_id, centro_id, reservada_en)"
                                + " VALUES (:cupo, :reserva, :paciente, :centro, :en)")
                        .param("cupo", cupo.id().valor())
                        .param("reserva", reserva.id().valor())
                        .param("paciente", reserva.pacienteId())
                        .param("centro", reserva.centroId())
                        .param("en", Jdbc.aFecha(reserva.reservadaEn()))
                        .update();
            }
        });
    }
}
