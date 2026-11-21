package com.redsaludandina.interconsulta.aplicacion.servicio;

import com.redsaludandina.compartido.aplicacion.RecursoNoEncontradoException;
import com.redsaludandina.compartido.aplicacion.UnidadDeTrabajo;
import com.redsaludandina.compartido.aplicacion.mensajeria.EventoEntrante;
import com.redsaludandina.compartido.aplicacion.mensajeria.EventoFueraDeOrdenException;
import com.redsaludandina.compartido.aplicacion.mensajeria.InboxPort;
import com.redsaludandina.compartido.aplicacion.mensajeria.RegistradorOutbox;
import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.compartido.dominio.Requisitos;
import com.redsaludandina.compartido.evento.CupoReservado;
import com.redsaludandina.compartido.evento.PacienteAtendido;
import com.redsaludandina.interconsulta.aplicacion.puerto.entrada.AvanzarInterconsulta;
import com.redsaludandina.interconsulta.aplicacion.puerto.entrada.CerrarInterconsulta;
import com.redsaludandina.interconsulta.aplicacion.puerto.entrada.ConsultarAlertasPendientes;
import com.redsaludandina.interconsulta.aplicacion.puerto.entrada.SolicitarInterconsulta;
import com.redsaludandina.interconsulta.aplicacion.puerto.salida.InterconsultaRepositorio;
import com.redsaludandina.interconsulta.dominio.modelo.Interconsulta;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

/**
 * Casos de uso de Interconsulta y su política «Avanzar estado».
 *
 * <p>Correlación de eventos: un CupoReservado agenda la interconsulta SOLICITADA más antigua del
 * paciente (si ya hay una agendada con ese cupo, es un duplicado y no pasa nada); un
 * PacienteAtendido avanza la interconsulta agendada con su cupo. Si PacienteAtendido llega antes
 * que el CupoReservado y el paciente tiene interconsultas solicitadas, se pide reintentar.
 *
 * <p>Consumidor idempotente: descarta un evento ya procesado por su id (inbox), en la misma
 * transacción que aplica el efecto; si pide reintento, el registro del inbox también se deshace.
 */
public final class ServicioInterconsulta
        implements SolicitarInterconsulta, CerrarInterconsulta, ConsultarAlertasPendientes, AvanzarInterconsulta {

    static final String CONSUMIDOR = "interconsulta.avanzar-estado";

    private final InterconsultaRepositorio interconsultas;
    private final InboxPort inbox;
    private final UnidadDeTrabajo unidadDeTrabajo;
    private final RegistradorOutbox outbox;
    private final Clock reloj;

    public ServicioInterconsulta(InterconsultaRepositorio interconsultas, InboxPort inbox,
                                 UnidadDeTrabajo unidadDeTrabajo, RegistradorOutbox outbox, Clock reloj) {
        this.interconsultas = Requisitos.noNulo(interconsultas, "interconsultas");
        this.inbox = Requisitos.noNulo(inbox, "inbox");
        this.unidadDeTrabajo = Requisitos.noNulo(unidadDeTrabajo, "unidadDeTrabajo");
        this.outbox = Requisitos.noNulo(outbox, "outbox");
        this.reloj = Requisitos.noNulo(reloj, "reloj");
    }

    @Override
    public void solicitar(SolicitarInterconsulta.Comando comando) {
        Requisitos.noNulo(comando, "comando");
        unidadDeTrabajo.ejecutarSinResultado(() -> {
            if (interconsultas.buscar(comando.interconsultaId()).isPresent()) {
                return;
            }
            Interconsulta interconsulta = Interconsulta.solicitar(comando.interconsultaId(), comando.episodioOrigenId(),
                    comando.pacienteId(), comando.centroId(), comando.especialidad(), comando.motivo(),
                    reloj.instant());
            guardar(interconsulta);
        });
    }

    @Override
    public void cerrar(IdClinico interconsultaId) {
        unidadDeTrabajo.ejecutarSinResultado(() -> {
            Interconsulta interconsulta = interconsultas.buscar(Requisitos.noNulo(interconsultaId, "interconsultaId"))
                    .orElseThrow(() -> new RecursoNoEncontradoException("la interconsulta " + interconsultaId));
            interconsulta.cerrar(reloj.instant());
            guardar(interconsulta);
        });
    }

    @Override
    public List<Interconsulta> pendientes(String centroId) {
        return unidadDeTrabajo.ejecutar(() -> interconsultas.conAlertasPendientes(Requisitos.texto(centroId, "centroId")));
    }

    @Override
    public void procesar(EventoEntrante entrante) {
        Requisitos.noNulo(entrante, "entrante");
        if (entrante.evento() instanceof CupoReservado reservado) {
            unidadDeTrabajo.ejecutarSinResultado(() -> {
                if (inbox.registrarSiEsNuevo(CONSUMIDOR, entrante.eventoId())) {
                    agendar(reservado);
                }
            });
        } else if (entrante.evento() instanceof PacienteAtendido atendido) {
            unidadDeTrabajo.ejecutarSinResultado(() -> {
                if (inbox.registrarSiEsNuevo(CONSUMIDOR, entrante.eventoId())) {
                    registrarAtencion(atendido);
                }
            });
        }
    }

    private void agendar(CupoReservado reservado) {
        Optional<Interconsulta> destino = interconsultas.buscarPorCupo(reservado.cupoId())
                .or(() -> interconsultas.solicitadasDelPaciente(reservado.pacienteId()).stream().findFirst());
        destino.ifPresent(interconsulta -> {
            interconsulta.agendar(reservado.cupoId(), reloj.instant());
            guardar(interconsulta);
        });
    }

    private void registrarAtencion(PacienteAtendido atendido) {
        Optional<Interconsulta> destino = atendido.cupo().flatMap(interconsultas::buscarPorCupo);
        if (destino.isEmpty()) {
            if (atendido.cupo().isPresent() && !interconsultas.solicitadasDelPaciente(atendido.pacienteId()).isEmpty()) {
                throw new EventoFueraDeOrdenException("PacienteAtendido del episodio " + atendido.episodioId()
                        + " llegó antes que el CupoReservado de su interconsulta");
            }
            return;
        }
        Interconsulta interconsulta = destino.get();
        interconsulta.registrarAtencion(atendido.episodioId(), reloj.instant());
        guardar(interconsulta);
    }

    private void guardar(Interconsulta interconsulta) {
        interconsultas.guardar(interconsulta);
        outbox.registrar(interconsulta);
    }
}
