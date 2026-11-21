package com.redsaludandina.identidad.aplicacion.servicio;

import com.redsaludandina.compartido.aplicacion.UnidadDeTrabajo;
import com.redsaludandina.compartido.aplicacion.mensajeria.RegistradorOutbox;
import com.redsaludandina.compartido.dominio.Requisitos;
import com.redsaludandina.identidad.aplicacion.puerto.entrada.AutorizarAcceso;
import com.redsaludandina.identidad.aplicacion.puerto.salida.UsuarioRepositorio;
import com.redsaludandina.identidad.dominio.evento.AccesoDenegado;
import com.redsaludandina.identidad.dominio.modelo.DecisionAcceso;
import com.redsaludandina.identidad.dominio.modelo.RecursoClinico;
import com.redsaludandina.identidad.dominio.servicio.PoliticaAcceso;
import java.time.Clock;
import java.util.List;

/**
 * Evalúa la política de acceso y deja en el outbox de Identidad cada denegación (AccesoDenegado).
 * Un usuario sin perfil se deniega.
 */
public final class ServicioAutorizacion implements AutorizarAcceso {

    private final UsuarioRepositorio usuarios;
    private final PoliticaAcceso politica;
    private final UnidadDeTrabajo unidadDeTrabajo;
    private final RegistradorOutbox outbox;
    private final Clock reloj;

    public ServicioAutorizacion(UsuarioRepositorio usuarios, PoliticaAcceso politica, UnidadDeTrabajo unidadDeTrabajo,
                                RegistradorOutbox outbox, Clock reloj) {
        this.usuarios = Requisitos.noNulo(usuarios, "usuarios");
        this.politica = Requisitos.noNulo(politica, "politica");
        this.unidadDeTrabajo = Requisitos.noNulo(unidadDeTrabajo, "unidadDeTrabajo");
        this.outbox = Requisitos.noNulo(outbox, "outbox");
        this.reloj = Requisitos.noNulo(reloj, "reloj");
    }

    @Override
    public ResultadoAutorizacion autorizar(SolicitudAcceso solicitud) {
        Requisitos.noNulo(solicitud, "solicitud");
        return unidadDeTrabajo.ejecutar(() -> {
            RecursoClinico recurso = new RecursoClinico(solicitud.recurso(), solicitud.pacienteId(),
                    solicitud.centroId(), solicitud.especialidad());
            DecisionAcceso decision = usuarios.buscar(solicitud.usuarioId())
                    .map(usuario -> politica.evaluar(usuario, recurso))
                    .orElseGet(() -> DecisionAcceso.denegar("Usuario sin perfil de acceso"));
            if (!decision.permitido()) {
                outbox.registrar(solicitud.usuarioId(), List.of(new AccesoDenegado(
                        solicitud.usuarioId(), recurso.referencia(), decision.razon(), reloj.instant())));
            }
            return new ResultadoAutorizacion(decision.permitido(), decision.razon());
        });
    }
}
