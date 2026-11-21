package com.redsaludandina.legado.dominio.modelo;

import com.redsaludandina.compartido.dominio.Requisitos;
import java.time.LocalDate;

/** Documento del legado que el especialista puede solicitar (solo su referencia, no el contenido). */
public record DocumentoDisponible(String referencia, String tipo, LocalDate fecha) {

    public DocumentoDisponible {
        referencia = Requisitos.texto(referencia, "referencia");
        tipo = Requisitos.texto(tipo, "tipo");
        Requisitos.noNulo(fecha, "fecha");
    }
}
