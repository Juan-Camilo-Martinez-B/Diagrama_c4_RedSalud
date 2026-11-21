package com.redsaludandina.compartido.evento;

import com.redsaludandina.compartido.dominio.EventoDominio;
import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.compartido.dominio.Requisitos;
import com.redsaludandina.compartido.dominio.SelloCentro;
import java.time.Instant;

/**
 * Last-writer-wins dejó un valor clínico fuera del estado vigente. Lleva AMBOS valores para que
 * Auditoría los registre: «no se descarta nada» (Caso §4).
 */
public record ValorClinicoDescartado(
        IdClinico episodioId,
        String campo,
        String valorConservado,
        SelloCentro selloConservado,
        String valorDescartado,
        SelloCentro selloDescartado,
        Instant ocurridoEn) implements EventoDominio {

    public ValorClinicoDescartado {
        Requisitos.noNulo(episodioId, "episodioId");
        campo = Requisitos.texto(campo, "campo");
        Requisitos.noNulo(valorConservado, "valorConservado");
        Requisitos.noNulo(selloConservado, "selloConservado");
        Requisitos.noNulo(valorDescartado, "valorDescartado");
        Requisitos.noNulo(selloDescartado, "selloDescartado");
        Requisitos.noNulo(ocurridoEn, "ocurridoEn");
    }
}
