package com.redsaludandina.interconsulta.infraestructura.adaptador.entrada.web;

import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.interconsulta.aplicacion.puerto.entrada.CerrarInterconsulta;
import com.redsaludandina.interconsulta.aplicacion.puerto.entrada.ConsultarAlertasPendientes;
import com.redsaludandina.interconsulta.aplicacion.puerto.entrada.SolicitarInterconsulta;
import com.redsaludandina.interconsulta.dominio.modelo.Alerta;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API de Interconsulta [HTTPS], con la vista «Alertas pendientes». */
@RestController
@RequestMapping("/interconsultas")
public class InterconsultaControlador {

    private final SolicitarInterconsulta solicitarInterconsulta;
    private final CerrarInterconsulta cerrarInterconsulta;
    private final ConsultarAlertasPendientes consultarAlertasPendientes;

    public InterconsultaControlador(SolicitarInterconsulta solicitarInterconsulta, CerrarInterconsulta cerrarInterconsulta,
                                    ConsultarAlertasPendientes consultarAlertasPendientes) {
        this.solicitarInterconsulta = solicitarInterconsulta;
        this.cerrarInterconsulta = cerrarInterconsulta;
        this.consultarAlertasPendientes = consultarAlertasPendientes;
    }

    record SolicitudInterconsulta(String interconsultaId, String episodioOrigenId, String pacienteId, String centroId,
                                  String especialidad, String motivo) {
    }

    record VistaPendiente(String interconsultaId, String especialidad, String estado, List<String> alertas) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void solicitar(@RequestBody SolicitudInterconsulta solicitud) {
        solicitarInterconsulta.solicitar(new SolicitarInterconsulta.Comando(IdClinico.de(solicitud.interconsultaId()),
                IdClinico.de(solicitud.episodioOrigenId()), solicitud.pacienteId(), solicitud.centroId(),
                solicitud.especialidad(), solicitud.motivo()));
    }

    @PostMapping("/{interconsultaId}/cierre")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cerrar(@PathVariable String interconsultaId) {
        cerrarInterconsulta.cerrar(IdClinico.de(interconsultaId));
    }

    @GetMapping("/alertas")
    List<VistaPendiente> alertas(@RequestParam String centroId) {
        return consultarAlertasPendientes.pendientes(centroId).stream()
                .map(interconsulta -> new VistaPendiente(interconsulta.id().valor(), interconsulta.especialidad(),
                        interconsulta.estado().name(),
                        interconsulta.alertasPendientes().stream().map(Alerta::tipo).map(Enum::name).toList()))
                .toList();
    }
}
