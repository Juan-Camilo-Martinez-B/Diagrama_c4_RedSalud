package com.redsaludandina.auditoria.aplicacion.servicio;

import com.redsaludandina.auditoria.aplicacion.puerto.entrada.ConsultarQuienVio;
import com.redsaludandina.auditoria.aplicacion.puerto.entrada.RegistrarAcceso;
import com.redsaludandina.auditoria.aplicacion.puerto.salida.RegistroAccesosPort;
import com.redsaludandina.auditoria.dominio.modelo.EventoAcceso;
import com.redsaludandina.auditoria.dominio.modelo.ReporteAccesos;
import com.redsaludandina.compartido.aplicacion.mensajeria.EventoEntrante;
import com.redsaludandina.compartido.dominio.Requisitos;
import com.redsaludandina.compartido.evento.LecturaClinica;
import com.redsaludandina.compartido.evento.ValorClinicoDescartado;
import java.util.List;

/**
 * Auditoría. El id del registro ES el id del evento del outbox: si el bus entrega el mismo evento
 * dos veces, el segundo intento encuentra el registro y no lo duplica. No necesita inbox.
 */
public final class ServicioAuditoria implements RegistrarAcceso, ConsultarQuienVio {

    private final RegistroAccesosPort registro;

    public ServicioAuditoria(RegistroAccesosPort registro) {
        this.registro = Requisitos.noNulo(registro, "registro");
    }

    @Override
    public void procesar(EventoEntrante entrante) {
        Requisitos.noNulo(entrante, "entrante");
        if (entrante.evento() instanceof LecturaClinica lectura) {
            registro.agregarSiNoExiste(EventoAcceso.desde(lectura, entrante.eventoId()));
        } else if (entrante.evento() instanceof ValorClinicoDescartado descarte) {
            registro.agregarSiNoExiste(EventoAcceso.desde(descarte, entrante.eventoId()));
        }
    }

    @Override
    public List<EventoAcceso> quienVio(String recurso) {
        return ReporteAccesos.quienVio(recurso, registro.porRecurso(Requisitos.texto(recurso, "recurso")));
    }
}
