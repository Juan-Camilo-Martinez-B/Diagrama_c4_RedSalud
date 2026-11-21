package com.redsaludandina.legado.aplicacion.servicio;

import com.redsaludandina.compartido.aplicacion.RecursoNoEncontradoException;
import com.redsaludandina.compartido.aplicacion.UnidadDeTrabajo;
import com.redsaludandina.compartido.aplicacion.mensajeria.RegistradorOutbox;
import com.redsaludandina.compartido.dominio.Requisitos;
import com.redsaludandina.compartido.evento.LecturaClinica;
import com.redsaludandina.identidad.aplicacion.puerto.entrada.AutorizarAcceso;
import com.redsaludandina.identidad.aplicacion.puerto.entrada.AutorizarAcceso.SolicitudAcceso;
import com.redsaludandina.legado.aplicacion.puerto.entrada.ConsultarHistorialClinico;
import com.redsaludandina.legado.aplicacion.puerto.salida.HistorialClinicoPort;
import com.redsaludandina.legado.dominio.modelo.HistorialClinico;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Consulta del historial (GET /historial/{id}): autoriza por rol y atributo, pide el historial a
 * la ACL y deja una LecturaClinica en el outbox de Integración legado (Caso §3 y §5).
 *
 * <p>La autorización va FUERA de la unidad de trabajo de la lectura: si se niega, Identidad
 * confirma su AccesoDenegado aunque aquí no se lea nada.
 */
public final class ServicioHistorialClinico implements ConsultarHistorialClinico {

    private final AutorizarAcceso autorizacion;
    private final HistorialClinicoPort historiales;
    private final UnidadDeTrabajo unidadDeTrabajo;
    private final RegistradorOutbox outbox;
    private final Clock reloj;

    public ServicioHistorialClinico(AutorizarAcceso autorizacion, HistorialClinicoPort historiales,
                                    UnidadDeTrabajo unidadDeTrabajo, RegistradorOutbox outbox, Clock reloj) {
        this.autorizacion = Requisitos.noNulo(autorizacion, "autorizacion");
        this.historiales = Requisitos.noNulo(historiales, "historiales");
        this.unidadDeTrabajo = Requisitos.noNulo(unidadDeTrabajo, "unidadDeTrabajo");
        this.outbox = Requisitos.noNulo(outbox, "outbox");
        this.reloj = Requisitos.noNulo(reloj, "reloj");
    }

    @Override
    public VistaHistorial consultar(Consulta consulta) {
        Requisitos.noNulo(consulta, "consulta");
        String motivo = Requisitos.texto(consulta.motivo(), "motivo de la lectura");
        String recurso = "historial/" + Requisitos.texto(consulta.pacienteId(), "pacienteId");
        autorizacion.exigir(new SolicitudAcceso(consulta.usuarioId(), recurso, consulta.pacienteId(),
                consulta.centroId(), null));

        return unidadDeTrabajo.ejecutar(() -> {
            HistorialClinico historial = historiales.obtener(consulta.pacienteId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(recurso));
            Instant ahora = reloj.instant();
            outbox.registrar(recurso, List.of(new LecturaClinica(consulta.usuarioId(), recurso, motivo, ahora)));
            return aVista(historial, ahora);
        });
    }

    private static VistaHistorial aVista(HistorialClinico historial, Instant ahora) {
        return new VistaHistorial(
                historial.pacienteId(),
                historial.episodios().stream()
                        .map(episodio -> new EpisodioVista(episodio.referencia(), episodio.fecha(), episodio.descripcion()))
                        .toList(),
                historial.documentos().stream()
                        .map(documento -> new DocumentoVista(documento.referencia(), documento.tipo(), documento.fecha()))
                        .toList(),
                historial.frescura().obtenidoEn(),
                historial.caducaEn(),
                historial.frescura().origen().name(),
                historial.estaVigente(ahora));
    }
}
