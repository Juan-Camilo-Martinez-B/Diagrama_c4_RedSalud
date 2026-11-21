package com.redsaludandina.agenda.aplicacion.servicio;

import com.redsaludandina.agenda.aplicacion.puerto.entrada.PublicarCupo;
import com.redsaludandina.agenda.aplicacion.puerto.entrada.ReservarCupo;
import com.redsaludandina.agenda.aplicacion.puerto.salida.CupoRepositorio;
import com.redsaludandina.agenda.dominio.modelo.Cupo;
import com.redsaludandina.agenda.dominio.modelo.Franja;
import com.redsaludandina.compartido.aplicacion.RecursoNoEncontradoException;
import com.redsaludandina.compartido.aplicacion.UnidadDeTrabajo;
import com.redsaludandina.compartido.aplicacion.mensajeria.RegistradorOutbox;
import com.redsaludandina.compartido.dominio.Requisitos;
import java.time.Clock;

/** Casos de uso de Agenda. Cada reserva guarda el cupo y su CupoReservado en una sola transacción. */
public final class ServicioAgenda implements PublicarCupo, ReservarCupo {

    private final CupoRepositorio cupos;
    private final UnidadDeTrabajo unidadDeTrabajo;
    private final RegistradorOutbox outbox;
    private final Clock reloj;

    public ServicioAgenda(CupoRepositorio cupos, UnidadDeTrabajo unidadDeTrabajo, RegistradorOutbox outbox, Clock reloj) {
        this.cupos = Requisitos.noNulo(cupos, "cupos");
        this.unidadDeTrabajo = Requisitos.noNulo(unidadDeTrabajo, "unidadDeTrabajo");
        this.outbox = Requisitos.noNulo(outbox, "outbox");
        this.reloj = Requisitos.noNulo(reloj, "reloj");
    }

    @Override
    public void publicar(PublicarCupo.Comando comando) {
        Requisitos.noNulo(comando, "comando");
        unidadDeTrabajo.ejecutarSinResultado(() -> {
            if (cupos.buscar(comando.cupoId()).isEmpty()) {
                cupos.guardar(Cupo.publicar(comando.cupoId(), comando.especialistaId(),
                        new Franja(comando.inicio(), comando.fin())));
            }
        });
    }

    @Override
    public void reservar(ReservarCupo.Comando comando) {
        Requisitos.noNulo(comando, "comando");
        unidadDeTrabajo.ejecutarSinResultado(() -> {
            Cupo cupo = cupos.buscar(comando.cupoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("el cupo " + comando.cupoId()));
            cupo.reservar(comando.reservaId(), comando.pacienteId(), comando.centroId(), reloj.instant());
            cupos.guardar(cupo);
            outbox.registrar(cupo);
        });
    }
}
