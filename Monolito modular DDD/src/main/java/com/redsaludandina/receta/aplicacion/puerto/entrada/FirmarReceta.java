package com.redsaludandina.receta.aplicacion.puerto.entrada;

import com.redsaludandina.compartido.dominio.IdClinico;

/** Firma la receta (solo su prescriptor) y publica RecetaFirmada. */
public interface FirmarReceta {

    void firmar(Comando comando);

    record Comando(IdClinico recetaId, String firmanteId, String valorFirma) {
    }
}
