package com.redsaludandina.legado.infraestructura.adaptador.entrada.web;

import com.redsaludandina.compartido.infraestructura.web.UsuarioAutenticado;
import com.redsaludandina.legado.aplicacion.puerto.entrada.ConsultarHistorialClinico;
import com.redsaludandina.legado.aplicacion.puerto.entrada.ConsultarHistorialClinico.Consulta;
import com.redsaludandina.legado.aplicacion.puerto.entrada.ConsultarHistorialClinico.VistaHistorial;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /historial/{pacienteId}: el especialista lee el historial con su frescura visible
 * ({@code vigente}, {@code caducaEn}, {@code origen}). El motivo es obligatorio y queda auditado.
 */
@RestController
public class HistorialControlador {

    private final ConsultarHistorialClinico consultarHistorial;

    public HistorialControlador(ConsultarHistorialClinico consultarHistorial) {
        this.consultarHistorial = consultarHistorial;
    }

    @GetMapping("/historial/{pacienteId}")
    VistaHistorial consultar(@PathVariable String pacienteId, @RequestParam String centroId, @RequestParam String motivo,
                             @UsuarioAutenticado String usuarioId) {
        return consultarHistorial.consultar(new Consulta(usuarioId, pacienteId, centroId, motivo));
    }
}
