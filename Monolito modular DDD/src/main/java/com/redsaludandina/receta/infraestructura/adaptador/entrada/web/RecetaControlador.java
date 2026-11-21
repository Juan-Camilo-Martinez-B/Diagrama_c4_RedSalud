package com.redsaludandina.receta.infraestructura.adaptador.entrada.web;

import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.compartido.infraestructura.web.UsuarioAutenticado;
import com.redsaludandina.receta.aplicacion.puerto.entrada.AgregarMedicamento;
import com.redsaludandina.receta.aplicacion.puerto.entrada.FirmarReceta;
import com.redsaludandina.receta.aplicacion.puerto.entrada.RedactarReceta;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API de Receta [HTTPS]. Firma el usuario autenticado, nunca alguien que se declare en el cuerpo. */
@RestController
@RequestMapping("/recetas")
public class RecetaControlador {

    private final RedactarReceta redactarReceta;
    private final AgregarMedicamento agregarMedicamento;
    private final FirmarReceta firmarReceta;

    public RecetaControlador(RedactarReceta redactarReceta, AgregarMedicamento agregarMedicamento,
                             FirmarReceta firmarReceta) {
        this.redactarReceta = redactarReceta;
        this.agregarMedicamento = agregarMedicamento;
        this.firmarReceta = firmarReceta;
    }

    record SolicitudReceta(String recetaId, String episodioId) {
    }

    record SolicitudMedicamento(String nombre, String dosis, String indicacion) {
    }

    record SolicitudFirma(String valorFirma) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void redactar(@RequestBody SolicitudReceta solicitud, @UsuarioAutenticado String prescriptorId) {
        redactarReceta.redactar(new RedactarReceta.Comando(IdClinico.de(solicitud.recetaId()),
                IdClinico.de(solicitud.episodioId()), prescriptorId));
    }

    @PostMapping("/{recetaId}/medicamentos")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void agregar(@PathVariable String recetaId, @RequestBody SolicitudMedicamento solicitud) {
        agregarMedicamento.agregar(new AgregarMedicamento.Comando(IdClinico.de(recetaId), solicitud.nombre(),
                solicitud.dosis(), solicitud.indicacion()));
    }

    @PostMapping("/{recetaId}/firma")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void firmar(@PathVariable String recetaId, @RequestBody SolicitudFirma solicitud,
                @UsuarioAutenticado String firmanteId) {
        firmarReceta.firmar(new FirmarReceta.Comando(IdClinico.de(recetaId), firmanteId, solicitud.valorFirma()));
    }
}
