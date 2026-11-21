package com.redsaludandina.interconsulta.dominio.modelo;

import com.redsaludandina.compartido.dominio.AgregadoRaiz;
import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.compartido.dominio.ReglaDeNegocioException;
import com.redsaludandina.compartido.dominio.Requisitos;
import com.redsaludandina.compartido.evento.InterconsultaCerrada;
import com.redsaludandina.compartido.evento.InterconsultaSolicitada;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Raíz del agregado Interconsulta (Núcleo): lleva el caso del médico rural al especialista por
 * solicitada → agendada → atendida → cerrada, con una alerta abierta mientras falte un paso
 * (Caso §2). Avanza consumiendo CupoReservado y PacienteAtendido; como los eventos pueden llegar
 * repetidos, recibir de nuevo el mismo hecho no tiene efecto.
 */
public final class Interconsulta extends AgregadoRaiz<IdClinico> {

    public static final String TRANSICION_INVALIDA = "INTERCONSULTA_TRANSICION_INVALIDA";

    private final IdClinico id;
    private final IdClinico episodioOrigenId;
    private final String pacienteId;
    private final String centroId;
    private final String especialidad;
    private final String motivo;
    private EstadoInterconsulta estado;
    private IdClinico cupoId;
    private IdClinico episodioAtencionId;
    private final List<Alerta> alertas;

    private Interconsulta(IdClinico id, IdClinico episodioOrigenId, String pacienteId, String centroId,
                          String especialidad, String motivo, EstadoInterconsulta estado, IdClinico cupoId,
                          IdClinico episodioAtencionId, Collection<Alerta> alertas) {
        this.id = Requisitos.noNulo(id, "interconsultaId");
        this.episodioOrigenId = Requisitos.noNulo(episodioOrigenId, "episodioOrigenId");
        this.pacienteId = Requisitos.texto(pacienteId, "pacienteId");
        this.centroId = Requisitos.texto(centroId, "centroId");
        this.especialidad = Requisitos.texto(especialidad, "especialidad");
        this.motivo = Requisitos.texto(motivo, "motivo");
        this.estado = Requisitos.noNulo(estado, "estado");
        this.cupoId = cupoId;
        this.episodioAtencionId = episodioAtencionId;
        this.alertas = new ArrayList<>(Requisitos.noNulo(alertas, "alertas"));
    }

    /** El médico rural pide la interconsulta desde un episodio; queda pendiente de agendar. */
    public static Interconsulta solicitar(IdClinico id, IdClinico episodioOrigenId, String pacienteId,
                                          String centroId, String especialidad, String motivo, Instant ahora) {
        Requisitos.noNulo(ahora, "ahora");
        Interconsulta interconsulta = new Interconsulta(id, episodioOrigenId, pacienteId, centroId, especialidad,
                motivo, EstadoInterconsulta.SOLICITADA, null, null, List.of());
        interconsulta.alertas.add(Alerta.abrir(TipoAlerta.PENDIENTE_AGENDAR, ahora));
        interconsulta.registrar(new InterconsultaSolicitada(id, interconsulta.centroId,
                interconsulta.especialidad, ahora));
        return interconsulta;
    }

    /** Reconstruye una interconsulta ya guardada; no emite eventos. */
    public static Interconsulta reconstituir(IdClinico id, IdClinico episodioOrigenId, String pacienteId,
                                             String centroId, String especialidad, String motivo,
                                             EstadoInterconsulta estado, IdClinico cupoId,
                                             IdClinico episodioAtencionId, Collection<Alerta> alertas) {
        return new Interconsulta(id, episodioOrigenId, pacienteId, centroId, especialidad, motivo, estado, cupoId,
                episodioAtencionId, alertas);
    }

    /** Se reservó el cupo con el especialista (reacción a CupoReservado). */
    public void agendar(IdClinico cupoReservado, Instant ahora) {
        Requisitos.noNulo(cupoReservado, "cupoId");
        if (estado == EstadoInterconsulta.AGENDADA && cupoReservado.equals(cupoId)) {
            return;
        }
        avanzarA(EstadoInterconsulta.AGENDADA, ahora);
        cupoId = cupoReservado;
    }

    /** El especialista atendió al paciente (reacción a PacienteAtendido). */
    public void registrarAtencion(IdClinico episodioAtendido, Instant ahora) {
        Requisitos.noNulo(episodioAtendido, "episodioId");
        if (estado == EstadoInterconsulta.ATENDIDA && episodioAtendido.equals(episodioAtencionId)) {
            return;
        }
        avanzarA(EstadoInterconsulta.ATENDIDA, ahora);
        episodioAtencionId = episodioAtendido;
    }

    /** Cierra la interconsulta y publica InterconsultaCerrada. */
    public void cerrar(Instant ahora) {
        if (estado == EstadoInterconsulta.CERRADA) {
            return;
        }
        avanzarA(EstadoInterconsulta.CERRADA, ahora);
        registrar(new InterconsultaCerrada(id, centroId, ahora));
    }

    private void avanzarA(EstadoInterconsulta destino, Instant ahora) {
        Requisitos.noNulo(ahora, "ahora");
        if (!estado.puedeAvanzarA(destino)) {
            throw new ReglaDeNegocioException(TRANSICION_INVALIDA,
                    "La interconsulta " + id + " no puede pasar de " + estado + " a " + destino);
        }
        alertas.stream().filter(Alerta::estaPendiente).forEach(alerta -> alerta.resolver(ahora));
        estado = destino;
        TipoAlerta siguiente = TipoAlerta.para(destino);
        if (siguiente != null) {
            alertas.add(Alerta.abrir(siguiente, ahora));
        }
    }

    public List<Alerta> alertasPendientes() {
        return alertas.stream().filter(Alerta::estaPendiente).toList();
    }

    @Override
    public IdClinico id() {
        return id;
    }

    public IdClinico episodioOrigenId() {
        return episodioOrigenId;
    }

    public String pacienteId() {
        return pacienteId;
    }

    public String centroId() {
        return centroId;
    }

    public String especialidad() {
        return especialidad;
    }

    public String motivo() {
        return motivo;
    }

    public EstadoInterconsulta estado() {
        return estado;
    }

    public Optional<IdClinico> cupoId() {
        return Optional.ofNullable(cupoId);
    }

    public Optional<IdClinico> episodioAtencionId() {
        return Optional.ofNullable(episodioAtencionId);
    }

    public List<Alerta> alertas() {
        return List.copyOf(alertas);
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof Interconsulta interconsulta && id.equals(interconsulta.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
