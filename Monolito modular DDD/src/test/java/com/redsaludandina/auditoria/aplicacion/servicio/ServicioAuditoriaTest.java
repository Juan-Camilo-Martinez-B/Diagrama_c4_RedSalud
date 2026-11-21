package com.redsaludandina.auditoria.aplicacion.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import com.redsaludandina.auditoria.aplicacion.puerto.salida.RegistroAccesosPort;
import com.redsaludandina.auditoria.dominio.modelo.EventoAcceso;
import com.redsaludandina.compartido.aplicacion.Fijos;
import com.redsaludandina.compartido.aplicacion.mensajeria.EventoEntrante;
import com.redsaludandina.compartido.evento.LecturaClinica;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ServicioAuditoriaTest {

    /** Almacén append-only de prueba: solo agrega, por id. */
    private final Map<UUID, EventoAcceso> registros = new LinkedHashMap<>();
    private final ServicioAuditoria servicio = new ServicioAuditoria(new RegistroAccesosPort() {
        @Override
        public boolean agregarSiNoExiste(EventoAcceso evento) {
            return registros.putIfAbsent(evento.id(), evento) == null;
        }

        @Override
        public List<EventoAcceso> porRecurso(String recurso) {
            return registros.values().stream().filter(evento -> evento.recurso().equals(recurso)).toList();
        }
    });

    @Test
    void unaLecturaEntregadaDosVecesSeRegistraUnaSolaVezConElIdDelEvento() {
        UUID eventoId = new UUID(0, 7);
        EventoEntrante lectura = new EventoEntrante(eventoId,
                new LecturaClinica("esp-1", "historial/pac-1", "Interconsulta", Fijos.AHORA));

        servicio.procesar(lectura);
        servicio.procesar(lectura);

        assertThat(registros).containsOnlyKeys(eventoId);
        assertThat(servicio.quienVio("historial/pac-1")).singleElement()
                .satisfies(registro -> assertThat(registro.motivo()).isEqualTo("Interconsulta"));
    }
}
