package com.redsaludandina.agenda.infraestructura.adaptador.entrada.web;

import com.redsaludandina.agenda.aplicacion.puerto.entrada.PublicarCupo;
import com.redsaludandina.agenda.aplicacion.puerto.entrada.ReservarCupo;
import com.redsaludandina.compartido.dominio.IdClinico;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API de Agenda [HTTPS]. */
@RestController
@RequestMapping("/agenda/cupos")
public class AgendaControlador {

    private final PublicarCupo publicarCupo;
    private final ReservarCupo reservarCupo;

    public AgendaControlador(PublicarCupo publicarCupo, ReservarCupo reservarCupo) {
        this.publicarCupo = publicarCupo;
        this.reservarCupo = reservarCupo;
    }

    record SolicitudCupo(String cupoId, String especialistaId, Instant inicio, Instant fin) {
    }

    record SolicitudReserva(String reservaId, String pacienteId, String centroId) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void publicar(@RequestBody SolicitudCupo solicitud) {
        publicarCupo.publicar(new PublicarCupo.Comando(IdClinico.de(solicitud.cupoId()), solicitud.especialistaId(),
                solicitud.inicio(), solicitud.fin()));
    }

    @PostMapping("/{cupoId}/reservas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void reservar(@PathVariable String cupoId, @RequestBody SolicitudReserva solicitud) {
        reservarCupo.reservar(new ReservarCupo.Comando(IdClinico.de(cupoId), IdClinico.de(solicitud.reservaId()),
                solicitud.pacienteId(), solicitud.centroId()));
    }
}
