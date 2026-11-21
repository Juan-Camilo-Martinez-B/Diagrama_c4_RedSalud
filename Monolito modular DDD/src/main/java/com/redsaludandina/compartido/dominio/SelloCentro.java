package com.redsaludandina.compartido.dominio;

import java.time.Instant;

/**
 * Sello de tiempo con el que un centro registró un dato, incluso sin conexión (Caso §4).
 * Ordena los cambios para last-writer-wins; si dos centros coinciden en el instante, desempata
 * el id del centro para que el resultado sea el mismo en cualquier servidor.
 */
public record SelloCentro(String centroId, Instant instante) implements Comparable<SelloCentro> {

    public SelloCentro {
        centroId = Requisitos.texto(centroId, "centroId");
        Requisitos.noNulo(instante, "instante");
    }

    @Override
    public int compareTo(SelloCentro otro) {
        int porInstante = instante.compareTo(otro.instante);
        return porInstante != 0 ? porInstante : centroId.compareTo(otro.centroId);
    }

    public boolean esPosteriorA(SelloCentro otro) {
        return compareTo(otro) > 0;
    }
}
