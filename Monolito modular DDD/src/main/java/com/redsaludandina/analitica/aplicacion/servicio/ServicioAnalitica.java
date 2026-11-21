package com.redsaludandina.analitica.aplicacion.servicio;

import com.redsaludandina.analitica.aplicacion.puerto.entrada.ConsultarIndicador;
import com.redsaludandina.analitica.aplicacion.puerto.entrada.ProyectarIndicadores;
import com.redsaludandina.analitica.aplicacion.puerto.salida.IndicadorRepositorio;
import com.redsaludandina.analitica.dominio.modelo.IndicadorUso;
import com.redsaludandina.compartido.aplicacion.UnidadDeTrabajo;
import com.redsaludandina.compartido.aplicacion.mensajeria.EventoEntrante;
import com.redsaludandina.compartido.aplicacion.mensajeria.InboxPort;
import com.redsaludandina.compartido.dominio.EventoDominio;
import com.redsaludandina.compartido.dominio.Requisitos;
import com.redsaludandina.compartido.evento.CupoReservado;
import com.redsaludandina.compartido.evento.InterconsultaCerrada;
import com.redsaludandina.compartido.evento.InterconsultaSolicitada;
import com.redsaludandina.compartido.evento.PacienteAtendido;
import java.time.Clock;
import java.time.YearMonth;
import java.util.Optional;

/**
 * Analítica de uso. Sumar no es idempotente, así que usa un INBOX: dentro de la misma unidad de
 * trabajo registra el id del evento y solo proyecta si es la primera vez que lo ve. El mes del
 * indicador sale de la fecha del evento en la zona horaria del reloj configurado.
 */
public final class ServicioAnalitica implements ProyectarIndicadores, ConsultarIndicador {

    static final String CONSUMIDOR = "analitica.indicadores";

    private final IndicadorRepositorio indicadores;
    private final InboxPort inbox;
    private final UnidadDeTrabajo unidadDeTrabajo;
    private final Clock reloj;

    public ServicioAnalitica(IndicadorRepositorio indicadores, InboxPort inbox, UnidadDeTrabajo unidadDeTrabajo,
                             Clock reloj) {
        this.indicadores = Requisitos.noNulo(indicadores, "indicadores");
        this.inbox = Requisitos.noNulo(inbox, "inbox");
        this.unidadDeTrabajo = Requisitos.noNulo(unidadDeTrabajo, "unidadDeTrabajo");
        this.reloj = Requisitos.noNulo(reloj, "reloj");
    }

    @Override
    public void procesar(EventoEntrante entrante) {
        Requisitos.noNulo(entrante, "entrante");
        Optional<String> centro = centroDe(entrante.evento());
        if (centro.isEmpty()) {
            return;
        }
        unidadDeTrabajo.ejecutarSinResultado(() -> {
            if (!inbox.registrarSiEsNuevo(CONSUMIDOR, entrante.eventoId())) {
                return;
            }
            YearMonth periodo = YearMonth.from(entrante.evento().ocurridoEn().atZone(reloj.getZone()));
            IndicadorUso indicador = indicadores.buscar(centro.get(), periodo)
                    .orElseGet(() -> IndicadorUso.iniciar(centro.get(), periodo));
            indicador.proyectar(entrante.evento());
            indicadores.guardar(indicador);
        });
    }

    @Override
    public Optional<IndicadorUso> consultar(String centroId, YearMonth periodo) {
        return unidadDeTrabajo.ejecutar(() -> indicadores.buscar(Requisitos.texto(centroId, "centroId"),
                Requisitos.noNulo(periodo, "periodo")));
    }

    private static Optional<String> centroDe(EventoDominio evento) {
        if (evento instanceof CupoReservado reservado) {
            return Optional.of(reservado.centroId());
        }
        if (evento instanceof PacienteAtendido atendido) {
            return Optional.of(atendido.centroId());
        }
        if (evento instanceof InterconsultaSolicitada solicitada) {
            return Optional.of(solicitada.centroId());
        }
        if (evento instanceof InterconsultaCerrada cerrada) {
            return Optional.of(cerrada.centroId());
        }
        return Optional.empty();
    }
}
